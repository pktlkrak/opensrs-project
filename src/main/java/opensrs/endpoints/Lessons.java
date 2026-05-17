/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package opensrs.endpoints;

import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import java.io.IOException;
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
import opensrs.dataobjects.Result;
import opensrs.dataobjects.Review;
import opensrs.sets.GlobalStore;
import opensrs.sets.SRSObject;
import opensrs.sets.Set;
import org.autumn.State;
import org.autumn.State.Cookie;
import org.autumn.annotations.Endpoint;
import org.autumn.annotations.FromBody;
import org.autumn.annotations.FromCookie;
import org.autumn.annotations.FromPath;
import org.autumn.annotations.Route;

/**
 * Endpoints related to accesing lessons.
 * <p>Lessons are the first time a user sees a certain reviewable - it is where they learn their meaning. After that the object can be reviewed</p>
 * @author piotr
 */
@Route("/api/{set}/lesson/")
public class Lessons {

    private Database database;

    public Lessons(Database database) {
        this.database = database;
    }

    /**
     * Endpoint for starting the lesson.
     * @param state Autumn State.
     * @param set   The set the lesson is for.
     * @throws IOException 
     */
    @Endpoint("/start")
    public void start(
            State state,
            @FromPath("set") String set
    ) throws IOException {
        state.setHeader("Cache-Control", "no-store");
        state.setCookie(new Cookie("_rev_session", Math.abs(new Random().nextInt()) + "").setHttpOnly(true).setPath("/api/" + set + "/lesson"));
        state.sendString("{}");
    }

    /**
     * Endpoint for accessing the current queue of lessons.
    * During the lesson process each lesson object the user can review now is queued and served to the user in random order.
     * @param state     Autumn State.
     * @param set       Set the lessons are form.
     * @param session   The current session.
     * @throws IOException 
     */
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
        java.util.Set<Integer> lessonQueuedIds = Utils.getQueuedLessons(database, uid, set.hashCode());
        Set thisSet = GlobalStore.getSet(set);
        List<SRSObject> queued = lessonQueuedIds.stream().map(thisSet::get).collect(Collectors.toList());
        Map<String, Long> typeCounts = new HashMap<>();
        queued.stream().forEach(n -> typeCounts.put(n.getFamily(), queued.stream()
                                        .filter(q -> q.getFamily().equals(n.getFamily()))
                                        .count()));
        ObjectNode node = Utils.MAPPER.createObjectNode();
        node.set("count", Utils.MAPPER.valueToTree(typeCounts));
        node.set("queue", Utils.MAPPER.valueToTree(queued));
        ((ArrayNode) node.get("queue")).forEach(n -> Utils.appendAdditionalToJSON((ObjectNode) n, thisSet));
        //No need to add userdata - no data for items that had just been unlocked.
        state.sendString(Utils.MAPPER.writeValueAsString(node), "application/JSON; charset=UTF-8");
    }

    /**
     * Endpoint for accessing what the user learned in the last session.
     * @param state     Autumn State.
     * @param set       Set the lessons are form. 
     * @param session   Current session.
     * @throws IOException 
     */
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
                        new Filter(Review.Parameter.TYPE, Filters.EqOnly.EQUAL, Review.TYPE_LESSON))),
                new Sorter(Review.Parameter.UNLOCK_DATE, Sorters.NumberSorter.DESCENDING));
        List<Review> onLastSession;
        if (last == null) {
            onLastSession = Arrays.asList();
        } else {
            onLastSession = database.fetchReviews(new Filter(Review.Parameter.REVIEW_SESSION_ID, Filters.EqOnly.EQUAL, last.getReviewSessionId()), null, -1);
        }
        List<SRSObject> data = onLastSession.stream().filter(n -> n.getSrsWeight() > 0).map(Review::getItemSUID).map(thisSet::get).collect(Collectors.toList());

        state.sendString(Utils.MAPPER.writeValueAsString(data), "application/JSON; charset=UTF-8");
    }

    /**
     * Endpoint for completing a review by a user.
     * @param state         Autumn State
     * @param set           Set the lessons are form. 
     * @param session       Current session.
     * @param lessonSession The lesson session this is a part of. 
     * @param keys          The keys of the completed lesson objects.
     * @throws IOException 
     */
    @Endpoint(value = "/completed", allowedMethods = { "POST" })
    public void completed(
            State state,
            @FromPath("set") String set,
            @FromCookie("session") String session,
            @FromCookie("_rev_session") String lessonSession,
            @FromBody("keys") List<Object> keys
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
        int lessonSessionI = Integer.valueOf(lessonSession);

        for (Object idStr : keys) {
            int id = Integer.valueOf(idStr.toString());
            List<Review> reviewL = database.updateReviews(
                    new FilterChain<>(FilterChain.Junction.AND, Arrays.asList(
                            new Filter<>(Review.Parameter.UID, Filters.EqOnly.EQUAL, uid),
                            new Filter<>(Review.Parameter.SET_CODE, Filters.EqOnly.EQUAL, set.hashCode()),
                            new Filter<>(Review.Parameter.ITEM_SUID, Filters.EqOnly.EQUAL, id),
                            new Filter<>(Review.Parameter.QUEUED_DATE, Filters.Number.ELESSER, System.currentTimeMillis()),
                            new Filter<>(Review.Parameter.UNLOCK_DATE, Filters.Number.EQUAL, -2))),
                    new Update<>(Review.Parameter.UNLOCK_DATE, Utils.currentTimeMillis()),
                    new Update<>(Review.Parameter.REVIEW_SESSION_ID, lessonSessionI),
                    new Update<>(Review.Parameter.SRS_WEIGHT, 1));
            if (reviewL.isEmpty()) {
                state.sendString("{\"errorCode\": 404, \"message\": \"No such lesson\"}", 404);
                return;
            }
            Review parent = reviewL.get(0);
            SRSObject parentObject = thisSet.get(id);
            int setCode = set.hashCode();
            database.createResults(
                    Stream.iterate(0, n -> n + 1)
                            .limit(parentObject.getReviewable().size())
                            .map(i -> {
                                Result res = new Result();
                                res.setAmountOfTries(0);
                                res.setItemParam(i);
                                res.setItemSUID(id);
                                res.setUid(uid);
                                res.setSetCode(setCode);
                                res.setReviewID(parent.getId());
                                return res;
                            }).toArray(Result[]::new));
            Utils.queueNextReview(database, uid, set, 1, id);
        }
        state.sendString(Utils.MAPPER.writeValueAsString(keys));
    }
}
