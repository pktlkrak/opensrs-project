/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package opensrs.endpoints;

import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import opensrs.SessionHolder;
import opensrs.Utils;
import opensrs.database.Database;
import opensrs.database.Filter;
import opensrs.database.FilterChain;
import opensrs.database.Filters;
import opensrs.database.Sorter;
import opensrs.database.Sorters;
import opensrs.database.Update;
import opensrs.database.implementation.SQLiteDatabase;
import opensrs.dataobjects.Result;
import opensrs.dataobjects.Review;
import opensrs.dataobjects.SRSObjectState;
import opensrs.sets.GlobalStore;
import opensrs.sets.SRSObject;
import opensrs.sets.Set;
import org.autumn.State;
import org.autumn.State.Cookie;
import org.autumn.annotations.Endpoint;
import org.autumn.annotations.FromCookie;
import org.autumn.annotations.FromPath;
import org.autumn.annotations.Route;

/**
 *
 * @author piotr
 */
@Route("/api/{set}/review/")
public class Reviews {

    private Database database;

    public Reviews(Database database) {
        this.database = database;
    }

    @Endpoint("/start")
    public void start(
            State state,
            @FromPath("set") String set
    ) throws IOException {
        //((SQLiteDatabase) database).snapshot();
        state.setHeader("Cache-Control", "no-store");
        state.setCookie(new Cookie("_rev_session", Math.abs(new Random().nextInt()) + "").setHttpOnly(true).setPath("/api/" + set + "/review"));
        state.sendString("{}");
    }

    @Endpoint("/queue")
    public void queue(
            State state,
            @FromPath("set") String set,
            @FromCookie("session") String session
    ) throws IOException {
        state.setHeader("Cache-Control", "no-store");
        int uid = SessionHolder.getUID(session);
        if (uid == -1) {
            Utils.sendNotLoggedInJSON(state);
            return;
        }
        if (Utils.isSetInvalid(set, state)) {
            return;
        }
        java.util.Set<Integer> reviewQueueIds = Utils.getQueuedReviews(database, uid, set.hashCode());
        Set thisSet = GlobalStore.getSet(set);
        List<SRSObject> queued = reviewQueueIds.stream().map(thisSet::get).collect(Collectors.toList());
        Map<String, Long> typeCounts = new HashMap<>();
        queued.stream().forEach(n -> typeCounts.put(n.getFamily(), queued.stream()
                .filter(q -> q.getFamily().equals(n.getFamily()))
                .count()));
        ObjectNode node = Utils.MAPPER.createObjectNode();
        node.set("count", Utils.MAPPER.valueToTree(typeCounts));
        node.set("queue", Utils.MAPPER.valueToTree(queued));
        ((ArrayNode) node.get("queue")).forEach(n -> Utils.appendAdditionalToJSON((ObjectNode) n, thisSet));
        state.sendString(Utils.MAPPER.writeValueAsString(node), "application/JSON; charset=UTF-8");
    }

    @Endpoint("/lastSession")
    public void lastSession(
            State state,
            @FromPath("set") String set,
            @FromCookie("session") String session
    ) throws IOException {
        state.setHeader("Cache-Control", "no-store");
        int uid = SessionHolder.getUID(session);
        if (uid == -1) {
            Utils.sendNotLoggedInJSON(state);
            return;
        }
        if (Utils.isSetInvalid(set, state)) {
            return;
        }
        Set thisSet = GlobalStore.getSet(set);
        Review last = database.fetchReview(
                new FilterChain<>(FilterChain.Junction.AND, Arrays.asList(
                        new Filter<>(Review.Parameter.UID, Filters.EqOnly.EQUAL, uid),
                        new Filter<>(Review.Parameter.SET_CODE, Filters.EqOnly.EQUAL, set.hashCode()),
                        new Filter(Review.Parameter.UNLOCK_DATE, Filters.Number.EGREATER, 0),
                        new Filter(Review.Parameter.TYPE, Filters.EqOnly.EQUAL, Review.TYPE_REVIEW))),
                new Sorter(Review.Parameter.UNLOCK_DATE, Sorters.NumberSorter.DESCENDING));
        List<Review> onLastSession;
        if (last == null) {
            onLastSession = Arrays.asList();
        } else {
            onLastSession = database.fetchReviews(new Filter(Review.Parameter.REVIEW_SESSION_ID, Filters.EqOnly.EQUAL, last.getReviewSessionId()), null, -1);
        }
        Map<Integer, Integer> srsStages = database.fetchState(
                new FilterChain<>(FilterChain.Junction.AND,
                        Arrays.asList(
                                new Filter<>(SRSObjectState.Parameter.UID, Filters.EqOnly.EQUAL, uid),
                                new Filter<>(SRSObjectState.Parameter.SET_CODE, Filters.EqOnly.EQUAL, set.hashCode()),
                                new FilterChain<>(FilterChain.Junction.OR,
                                        onLastSession.stream()
                                                .map(n -> new Filter<>(SRSObjectState.Parameter.ITEM_SUID, Filters.EqOnly.EQUAL, n.getItemSUID()))
                                                .collect(Collectors.toList()))
                        )), -1).stream()
                .collect(Collectors.toMap(SRSObjectState::getItemSUID, SRSObjectState::getWeight));

        List<SRSObject> correct = onLastSession.stream().filter(n -> n.getSrsWeight() > 0).map(Review::getItemSUID).map(thisSet::get).collect(Collectors.toList());
        List<SRSObject> incorrect = onLastSession.stream().filter(n -> n.getSrsWeight() <= 0).map(Review::getItemSUID).map(thisSet::get).collect(Collectors.toList());

        ArrayNode correctNode = Utils.MAPPER.valueToTree(correct);
        ArrayNode incorrectNode = Utils.MAPPER.valueToTree(incorrect);

        correctNode.forEach(n -> ((ObjectNode) n).put("srsPhase", thisSet.config.getPhaseForStage(srsStages.get(n.get("id").intValue()))));
        incorrectNode.forEach(n -> ((ObjectNode) n).put("srsPhase", thisSet.config.getPhaseForStage(srsStages.get(n.get("id").intValue()))));

        ObjectNode root = Utils.MAPPER.createObjectNode();
        root.set("correct", correctNode);
        root.set("incorrect", incorrectNode);
        state.sendString(Utils.MAPPER.writeValueAsString(root), "application/JSON; charset=UTF-8");
    }

    @Endpoint(value = "/progress", tryParsingBody = false, allowedMethods = { "POST" })
    public void progress(
            State state,
            @FromPath("set") String set,
            @FromCookie("session") String session,
            @FromCookie("_rev_session") String lessonSession
    ) throws IOException {
        state.setHeader("Cache-Control", "no-store");

        int uid = SessionHolder.getUID(session);
        if (uid == -1) {
            Utils.sendNotLoggedInJSON(state);
            return;
        }
        if (Utils.isSetInvalid(set, state)) {
            return;
        }

        Map<String, Object> items = org.autumn.Utils.queryToMap(new String(org.autumn.Utils.readFully(state.getInput(), Integer.valueOf(state.getInboundHeaders().get("Content-Length").get(0)))));
        //1234 => ["1", "2"]

        //Manual check - we can't use autumn here - unknown key names
        for (Object i : items.values()) {
            if (!(i instanceof List)) {
                state.sendString("{\"errorCode\": 400, \"message\": \"Invalid request\"}", 400);
                return;
            }
        }

        Map<Integer, Integer> states = database.fetchState(
                new FilterChain<>(FilterChain.Junction.AND, Arrays.asList(
                        new Filter<>(SRSObjectState.Parameter.UID, Filters.EqOnly.EQUAL, uid),
                        new Filter<>(SRSObjectState.Parameter.SET_CODE, Filters.EqOnly.EQUAL, set.hashCode()),
                        new FilterChain<>(FilterChain.Junction.OR,
                                items.keySet().stream()
                                        .map(n -> new Filter<>(SRSObjectState.Parameter.ITEM_SUID, Filters.EqOnly.EQUAL, Integer.valueOf(n)))
                                        .collect(Collectors.toList()))
                )), -1
        ).stream().collect(Collectors.toMap(SRSObjectState::getItemSUID, SRSObjectState::getWeight));

        Set thisSet = GlobalStore.getSet(set);

        int reviewSessionI;
        try {
            reviewSessionI = Integer.valueOf(lessonSession);
        } catch (RuntimeException ex) {
            state.sendString("{\"errorCode\": 400, \"message\": \"Invalid request - missing review token\"}", 400);
            return;
        }

        for (String idStr : items.keySet()) {
            int id = Integer.valueOf(idStr);
            SRSObject objectFromSet = thisSet.get(id);

            List<Integer> mistakes = ((List<String>) items.get(idStr)).stream().limit(objectFromSet.getReviewable().size()).map(Integer::valueOf).collect(Collectors.toList());
            int overallMistakes = mistakes.stream().reduce(0, Integer::sum);
            int currentSRSState = states.get(id);

            int srsDiff;
            if (overallMistakes == 0) {
                srsDiff = 1;
                int setCode = set.hashCode();
                //OK lesson passed.
                //Is this going to advance to the 'pass' phase?
                if (thisSet.config.getPhaseForStage(currentSRSState) == 1 && thisSet.config.getPhaseForStage(currentSRSState + srsDiff) == 2) {
                    //Unlock items dependent on this item from this level
                    List<SRSObjectState> everVisited = database.fetchState(
                            new FilterChain<>(FilterChain.Junction.AND, Arrays.asList(
                                    new Filter<>(SRSObjectState.Parameter.SET_CODE, Filters.EqOnly.EQUAL, setCode),
                                    new Filter<>(SRSObjectState.Parameter.UID, Filters.EqOnly.EQUAL, uid))), -1);

                    List<Integer> currentlyPassed = Stream.concat(
                            everVisited
                                    .stream()
                                    .filter(n -> thisSet.config.getPhaseForStage(n.getWeight()) >= 2) //Get only those on or above the passing stage
                                    .map(SRSObjectState::getItemSUID),
                            Stream.of(id)
                    ).collect(Collectors.toList());

                    Review[] newLessons = thisSet.stream()
                            //                     is on the same level                      depends on this item                is not this item
                            .filter(n -> n.getLevel() == objectFromSet.getLevel() /* && n.getDependencies().contains(id)*/ && n.getId() != objectFromSet.getId())
                            .filter(n -> !everVisited.stream().map(SRSObjectState::getItemSUID).anyMatch(e -> e == n.getId())) // Make sure this user has never seen this item before, eg. it's not queued or review-pending.
                            .filter(n -> { //If it's not missing any dependencies, queue a lesson for it
                                List<Integer> q = new ArrayList<>(n.getDependencies());
                                q.removeAll(currentlyPassed);
                                return q.isEmpty();
                            })
                            .map(n -> opensrs.Utils.createLesson(uid, setCode, n.getId())).toArray(Review[]::new);

                    // Current level is the max. level of any item seen before.
                    int currentLevel = everVisited.stream().map(n -> thisSet.get(n.getItemSUID()).getLevel()).max(Integer::compare).orElse(0);

                    // Level advancing conditions
                    // Is on a level smaller than the next level according to this item 
                    boolean a = currentLevel < objectFromSet.getLevel() + 1;
                    // All passed on this level?
                    boolean b = currentlyPassed.stream().filter(n -> thisSet.get(n).getLevel() == objectFromSet.getLevel()).count() == thisSet.stream().filter(n -> n.getLevel() == objectFromSet.getLevel()).count();
                    /*// And is the user not on the next level...
                    int idOfFirstLevelFirst = thisSet.stream().filter(n -> n.getLevel() == objectFromSet.getLevel() + 1 && n.getDependencies().isEmpty()).map(n -> n.getId()).findFirst().orElse(-1);
                    Review nextLevelReview = database.fetchReview(new Filter<>(Review.Parameter.ITEM_SUID, Filters.EqOnly.EQUAL, idOfFirstLevelFirst), null);
                    boolean c = nextLevelReview != null;*/

                    if (newLessons.length > 0) {
                        database.createReviews(newLessons);
                    } else if (a && b) {
                        //Next level
                        database.createReviews(
                                thisSet.stream()
                                        .filter(n -> {
                                            List<Integer> deps = new ArrayList<>(n.getDependencies());
                                            deps.removeAll(currentlyPassed);
                                            return n.getLevel() == objectFromSet.getLevel() + 1 && deps.isEmpty();
                                        })
                                        .map(n -> opensrs.Utils.createLesson(uid, setCode, n.getId()))
                                        .toArray(Review[]::new)
                        ); //Queue new lessons
                    }
                }
            } else {
                double adjustment = overallMistakes;
                adjustment = Math.ceil(adjustment / 2);
                srsDiff = -((int) adjustment * thisSet.config.getPenalty(currentSRSState));
            }
            srsDiff = currentSRSState + srsDiff < 1 ? -(currentSRSState - 1) : srsDiff; //Cannot fall below 1
            List<Review> reviewL = database.updateReviews(
                    new FilterChain<>(FilterChain.Junction.AND, Arrays.asList(
                            new Filter<>(Review.Parameter.UID, Filters.EqOnly.EQUAL, uid),
                            new Filter<>(Review.Parameter.SET_CODE, Filters.EqOnly.EQUAL, set.hashCode()),
                            new Filter<>(Review.Parameter.ITEM_SUID, Filters.EqOnly.EQUAL, id),
                            new Filter<>(Review.Parameter.QUEUED_DATE, Filters.Number.ELESSER, System.currentTimeMillis()),
                            new Filter<>(Review.Parameter.UNLOCK_DATE, Filters.Number.EQUAL, -1))),
                    new Update<>(Review.Parameter.UNLOCK_DATE, Utils.currentTimeMillis()),
                    new Update<>(Review.Parameter.REVIEW_SESSION_ID, reviewSessionI),
                    new Update<>(Review.Parameter.SRS_WEIGHT, srsDiff));
            if (reviewL.isEmpty()) {
                state.sendString("{\"errorCode\": 404, \"message\": \"No such review\"}", 404);
                return;
            }
            Review parent = reviewL.get(0);
            int setCode = set.hashCode();
            database.createResults(
                    Stream.iterate(0, n -> n + 1)
                            .limit(objectFromSet.getReviewable().size())
                            .map(i -> {
                                Result res = new Result();
                                res.setAmountOfTries(mistakes.get(i));
                                res.setItemParam(i);
                                res.setItemSUID(id);
                                res.setUid(uid);
                                res.setSetCode(setCode);
                                res.setReviewID(parent.getId());
                                return res;
                            }).toArray(Result[]::new));
            Utils.queueNextReview(database, uid, set, currentSRSState + srsDiff, id);
        }
        state.sendString(Utils.MAPPER.writeValueAsString(items));
    }
}
