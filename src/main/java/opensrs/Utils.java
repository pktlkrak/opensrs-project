/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package opensrs;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import opensrs.database.Database;
import opensrs.database.Filter;
import opensrs.database.FilterChain;
import opensrs.database.Filters;
import opensrs.dataobjects.Result;
import opensrs.dataobjects.Review;
import opensrs.dataobjects.SRSObjectState;
import opensrs.sets.GlobalStore;
import opensrs.sets.SRSObject;
import opensrs.sets.Set;
import org.autumn.State;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author piotr
 */
public class Utils {
    public static final ObjectMapper MAPPER = new ObjectMapper();
    public static DocumentBuilder documentBuilder;

    static {
        try {
            documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException ex) {
            Log.handleException("Utils", ex);
        }
    }

    public static Stream<Element> asStream(NodeList n) {
        return new AbstractList<Node>() {
            @Override
            public Node get(int index) {
                return (Node) n.item(index);
            }

            @Override
            public int size() {
                return n.getLength();
            }
        }.stream().filter(q -> q instanceof Element).map(Element.class::cast);
    }

    public static String getTextOfElement(Element parent, String name) {
        return parent.getElementsByTagName(name).item(0).getTextContent();
    }
    private final static char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static String randStr(int n) {
        return randStr(n, false);
    }

    public static String randStr(int n, boolean s) {
        final char[] data = "1234567890QWERTYUIOPASDFGHJKLZXCVBNMqwertyuiopasdfghjklzxcvbnm".toCharArray();
        String out = "";
        Random rand;
        try {
            rand = s ? SecureRandom.getInstanceStrong() : new Random();
        } catch (NoSuchAlgorithmException ex) {
            return null;
        }
        for (int i = 0; i < n; i++) {
            out += data[rand.nextInt(data.length)];
        }
        return out;
    }

    public static String getSHA256Hash(byte[] data) {
        String result = null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            result = bytesToHex(hash); // make it printable
        } catch (NoSuchAlgorithmException ex) {
            Log.handleException("Utils", ex);
        }
        return result;
    }

    public static java.util.Set<Integer> getQueuedLessons(Database database, int user, int setCode) {
        return database.fetchReviews(
                new FilterChain<>(FilterChain.Junction.AND, Arrays.asList(
                        new Filter<>(Review.Parameter.UNLOCK_DATE, Filters.Number.EQUAL, -2),
                        new Filter<>(Review.Parameter.QUEUED_DATE, Filters.Number.ELESSER, System.currentTimeMillis()),
                        new Filter<>(Review.Parameter.SET_CODE, Filters.EqOnly.EQUAL, setCode),
                        new Filter<>(Review.Parameter.UID, Filters.EqOnly.EQUAL, user)
                )), null, -1)
                .stream()
                .map(Review::getItemSUID)
                .collect(Collectors.toSet());
    }

    public static java.util.Set<Integer> getQueuedReviews(Database database, int user, int setCode) {
        return database.fetchReviews(
                new FilterChain<>(FilterChain.Junction.AND, Arrays.asList(
                        new Filter<>(Review.Parameter.UNLOCK_DATE, Filters.Number.EQUAL, -1),
                        new Filter<>(Review.Parameter.QUEUED_DATE, Filters.Number.ELESSER, System.currentTimeMillis()),
                        new Filter<>(Review.Parameter.SET_CODE, Filters.EqOnly.EQUAL, setCode),
                        new Filter<>(Review.Parameter.UID, Filters.EqOnly.EQUAL, user)
                )), null, -1)
                .stream()
                .map(Review::getItemSUID)
                .collect(Collectors.toSet());
    }

    public static void sendNotLoggedInJSON(State state) throws IOException {
        state.sendString("{\"errorCode\": 400, \"message\": \"Not logged in\"}", 400);
    }

    public static boolean isSetInvalid(String set, State state) throws IOException {
        if (!GlobalStore.hasSet(set)) {
            state.sendString("{\"errorCode\": 404, \"message\": \"Set not found\"}", 404);
            return true;
        }
        return false;
    }

    public static void queueNextReview(Database database, int user, String setName, int currentSrsStage, int id) {
        int delay = GlobalStore.getSet(setName).config.getDelayInHours(currentSrsStage);
        if(delay == -1) return; //Burned item
        long msDelay = delay;
        msDelay *= 60L /*minutes*/ * 60L /*seconds*/ * 1000L /*ms*/;
        Log.log(Log.DEBUG, "DEBUG", "Delaying " + id + " by " + delay + " hours");
        long msTime = Utils.currentTimeMillis() + msDelay;
        Review r = Review.create();
        r.setItemSUID(id);
        r.setQueuedDate(msTime);
        r.setSetCode(setName.hashCode());
        r.setUid(user);
        r.setUnlockDate(-1);
        r.setSrsWeight(0);
        r.setType(Review.TYPE_REVIEW);
        database.createReviews(r);
    }

    public static Review createLesson(int uid, int setCode, int id) {
        Review newLesson = Review.create();
        newLesson.setType(Review.TYPE_LESSON);
        newLesson.setQueuedDate(System.currentTimeMillis());
        newLesson.setItemSUID(id);
        newLesson.setSrsWeight(0);
        newLesson.setUnlockDate(-2);
        newLesson.setSetCode(setCode);
        newLesson.setUid(uid);
        return newLesson;
    }

    public static long currentTimeMillis() {
        Calendar c = Calendar.getInstance();
        return System.currentTimeMillis() - ((c.get(Calendar.SECOND) + (c.get(Calendar.MINUTE) * 60)) * 1000 + c.get(Calendar.MILLISECOND));
    }

    public static void appendAdditionalToJSON(ObjectNode n, Set set) {
        SRSObject thisObject = set.get(n.get("id").intValue());
        ((ObjectNode) n).set("itemsDependent", Utils.MAPPER.valueToTree(set.stream().filter(q -> q.getDependencies().contains(thisObject.getId())).toArray(SRSObject[]::new)));
        ((ObjectNode) n).set("dependencies", Utils.MAPPER.valueToTree(thisObject.getDependencies().stream().map(q -> set.get(q)).toArray(SRSObject[]::new)));
    }

    public static Consumer<? super JsonNode> appendUserdataToJSON(Database database, String setName, int uid) {
        Set set = GlobalStore.getSet(setName);
        Map<Integer, Integer> srsStages = database.fetchState(new FilterChain<>(FilterChain.Junction.AND, Arrays.asList(
                new Filter<>(SRSObjectState.Parameter.UID, Filters.EqOnly.EQUAL, uid),
                new Filter<>(SRSObjectState.Parameter.SET_CODE, Filters.EqOnly.EQUAL, setName.hashCode())
        )), -1).stream()
                .collect(Collectors.toMap(SRSObjectState::getItemSUID, SRSObjectState::getWeight));

        List<Result> results = database.fetchResults(new FilterChain<>(FilterChain.Junction.AND, Arrays.asList(
                new Filter<>(Result.Parameter.UID, Filters.EqOnly.EQUAL, uid),
                new Filter<>(Result.Parameter.SET_CODE, Filters.EqOnly.EQUAL, setName.hashCode())
        )), null, -1);

        return n -> {
            ObjectNode thisNode = (ObjectNode) n;
            SRSObject thisObject = set.get(thisNode.get("id").intValue());
            ArrayNode reviews = thisNode.putArray("reviewsTries");
            results.stream()
                    .filter(q -> q.getItemSUID() == thisObject.getId())
                    .map(Result::getReviewID)
                    .distinct()
                    .forEach(q -> {
                        reviews.add(Utils.MAPPER.valueToTree(
                                results.parallelStream()
                                        .filter(w -> w.getReviewID() == q)
                                        .sorted((a, b) -> a.getItemParam() - b.getItemParam())
                                        .map(Result::getAmountOfTries)
                                        .collect(Collectors.toList())
                        ));
                    });
            thisNode.put("rawStage", srsStages.getOrDefault(thisObject.getId(), 0));
            thisNode.put("phase", set.config.getPhaseForStage(srsStages.getOrDefault(thisObject.getId(), 0)));
            ((ArrayNode) thisNode.get("itemsDependent")).forEach(q -> {
                ((ObjectNode) q).put("phase", srsStages.get(q.get("id").intValue()));
            });
        };

    }

    public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        java.util.Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }

}
