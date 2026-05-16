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

    /**
     * Stream an XML node list
     * @param n The node list to stream
     * @return  A {@link Stream} object with all the objects from the NodeList buffered.
     */
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

    /**
     * Returns the text of a given XML node
     * @param parent    The XML parent
     * @param name      The name of the field, whose contents have to be returned
     *                  as text.
     * @return 
     */
    public static String getTextOfElement(Element parent, String name) {
        return parent.getElementsByTagName(name).item(0).getTextContent();
    }
    private final static char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    /**
     * Convert a byte array to a {@link String}
     * @param bytes The input byte array
     * @return      The string
     */
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    /**
     * Generates a random string of length n using the not cryptographically secure
     * random number generator
     * @param n     The length of the string to generate
     * @return      The random string
     */
    public static String randStr(int n) {
        return randStr(n, false);
    }

    /**
     * Generates a random string of length n using either the secure or insecure
     * random number generator
     * @param n     The length of the string
     * @param s     Whether or not to use a cryptographically-secure RNG source
     * @return      The random string
     */
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

    /**
     * Returns the SHA256 hash as a {@link String}
     * @param data  The data to hash
     * @return      Stringified SHA256 digest
     */
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

    /**
     * A helper method for fetching all the items for which lessons are queued
     * for a given user in a given set
     * @param database      The database instance
     * @param user          The user for whom the data is to be fetched
     * @param setCode       The set code from which lessons are to be fetched
     * @return              The set of item IDs
     */
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

    /**
     * A helper method for fetching all the items for which reviews are queued
     * for a given user in a given set
     * @param database      The database instance
     * @param user          The user for whom the data is to be fetched
     * @param setCode       The set code from which lessons are to be fetched
     * @return              The set of item IDs
     */
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

    /**
     * A helper method which sends back the not-logged-in JSON object.
     * @param state     Autumn server state
     * @throws IOException 
     */
    public static void sendNotLoggedInJSON(State state) throws IOException {
        state.sendJSON("{\"errorCode\": 400, \"message\": \"Not logged in\"}", 400);
    }

    /**
     * A helper method which checks if a given set exists, and in case it does not,
     * send back an appropriate error through the Autumn server state.
     * @param set       Set name
     * @param state     Autumn server state
     * @return          True if set is invalid
     * @throws IOException 
     */
    public static boolean isSetInvalid(String set, State state) throws IOException {
        if (!GlobalStore.hasSet(set)) {
            state.sendJSON("{\"errorCode\": 404, \"message\": \"Set not found\"}", 404);
            return true;
        }
        return false;
    }

    /**
     * A helper method for queuing a review
     * @param database          The database instance
     * @param user              The user ID for whom the review is to be queued
     * @param setName           The name of the set to which the item belongs
     * @param currentSrsStage   The SRS stage on which the item is right now 
     * @param id                The item ID
     */
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

    /**
     * A convenience function for creating a lesson object for a given UID, setCode and item ID.
     * @param uid       The ID of the user for whom the lesson is to be created
     * @param setCode   The setCode (hashCode of set name) of the set
     *                  the item belongs to.
     * @param id        The ID of the item to define the lesson for
     * @return          The final Lesson object
     */
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

    /**
     * Get {@link System#currentTimeMillis} but round it down to the nearest hour.
     * @return The rounded down {@link System#currentTimeMillis}
     */
    public static long currentTimeMillis() {
        Calendar c = Calendar.getInstance();
        return System.currentTimeMillis() - ((c.get(Calendar.SECOND) + (c.get(Calendar.MINUTE) * 60)) * 1000 + c.get(Calendar.MILLISECOND));
    }

    /**
     * Append dependencies and itemsDependent fields to the given JSON {@link ObjectNode}
     * @param n     The object node from which the ID of the item is retrieved,
     *              and which gets updated with the new fields
     * @param set   The set to which the item belongs to.
     */
    public static void appendAdditionalToJSON(ObjectNode n, Set set) {
        SRSObject thisObject = set.get(n.get("id").intValue());
        ((ObjectNode) n).set("itemsDependent", Utils.MAPPER.valueToTree(set.stream().filter(q -> q.getDependencies().contains(thisObject.getId())).toArray(SRSObject[]::new)));
        ((ObjectNode) n).set("dependencies", Utils.MAPPER.valueToTree(thisObject.getDependencies().stream().map(q -> set.get(q)).toArray(SRSObject[]::new)));
    }

    /**
     * Creates a function which fills in the rawStage and phase attributes for
     * a given object, while acting on the data looked up by the arguments.
     * 
     * @param database  The database instance
     * @param setName   The name of the set to which all the items passed to the
     *                  returned function belong
     * @param uid       The ID of the user for whom the parameters have to be looked up
     * @return          The function which takes in a JSON {@link ObjectNode}
     *                  and fills in the rawStage and phase attributes.
     */
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

    /**
     * A simple function which can be passed to a {@link Stream#filter} function
     * in order to remove duplicates
     * @param <T>               The type of the objects to act on
     * @param keyExtractor      The function which maps the object to the given 
     *                          unique 'key'.
     * @return                  A filtering function ideal for {@link Stream#filter}
     */
    public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        java.util.Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }

}
