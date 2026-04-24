/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package opensrs.endpoints;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
import opensrs.dataobjects.Review;
import opensrs.dataobjects.SRSObjectState;
import opensrs.sets.SRSObject;
import opensrs.sets.GlobalStore;
import opensrs.sets.Set;
import org.autumn.State;
import org.autumn.annotations.Endpoint;
import org.autumn.annotations.FromCookie;
import org.autumn.annotations.FromPath;
import org.autumn.annotations.Route;

/**
 *
 * @author piotr
 */
@Route("/debug")
public class Debug {

    private Database database;

    public Debug(Database database) {
        this.database = database;
    }

    @Endpoint("/initSetForUser/{set}/{user}")
    public void initSetForUser(
            State state,
            @FromPath("set") String set,
            @FromPath("user") String user,
            @FromCookie("session") String session
    ) throws IOException {
        state.setHeader("Cache-Control", "no-store");
        int uid = Integer.valueOf(user);

        database.createReviews(GlobalStore.getSet(set)
                .stream()
                .filter(n -> n.getLevel() == 1 && n.getDependencies().isEmpty())
                .map(n -> {
                    Review r = Review.create();
                    r.setUid(uid);
                    r.setSetCode(set.hashCode());
                    r.setType(Review.TYPE_LESSON);
                    r.setQueuedDate(System.currentTimeMillis());
                    r.setUnlockDate(-2);
                    r.setItemSUID(n.getId());
                    return r;
                })
                .toArray(Review[]::new));

        state.sendString("OK Done");
    }

    @Endpoint("/forceUnlock/{user}/{set}/{id}")
    public void forceUnlock(
            State state,
            @FromPath("user") String user,
            @FromPath("set") String set,
            @FromPath("id") String id
    ) throws IOException {
        database.createReviews(Utils.createLesson(Integer.valueOf(user), set.hashCode(), Integer.valueOf(id)));
        state.sendString("OK");
    }

    @Endpoint("/reload")
    public void reload(State state) throws IOException {
        GlobalStore.reload();
        state.sendString("OK");
    }

    @Endpoint("/user")
    public void user(
            State state,
            @FromCookie("session") String session
    ) throws IOException {
        state.setHeader("Cache-Control", "no-store");
        int uid = SessionHolder.getUID(session);
        state.sendString(uid == -1 ? "Not logged in" : "Logged in as UID " + uid + " with name '" + SessionHolder.getNameOfUser(session) + "'");
    }

    @Endpoint("/rerunlessoncode/{set}/{uid}/{levelPlaceholder}")
    public void reRunLessonCode(
            State state,
            @FromPath("set") String set,
            @FromPath("uid") String uidStr,
            @FromPath("levelPlaceholder") String lvlStr) throws IOException {
        int uid = Integer.valueOf(uidStr);
        int setCode = set.hashCode();
        int level = Integer.valueOf(lvlStr);

        Set thisSet = GlobalStore.getSet(set);

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
                Stream.of()
        ).collect(Collectors.toList());

        Review[] newLessons = thisSet.stream()
                //                     is on the same level                      depends on this item                is not this item
                .filter(n -> n.getLevel() == level /* && n.getDependencies().contains(id) && n.getId() != objectFromSet.getId())*/)
                .filter(n -> !everVisited.stream().map(SRSObjectState::getItemSUID).anyMatch(e -> e == n.getId())) // Make sure this user has never seen this item before, eg. it's not queued or review-pending.
                .filter(n -> { //If it's not missing any dependencies, queue a lesson for it
                    List<Integer> q = new ArrayList<>(n.getDependencies());
                    q.removeAll(currentlyPassed);
                    return q.isEmpty();
                })
                .map(n -> opensrs.Utils.createLesson(uid, setCode, n.getId())).toArray(Review[]::new);
        // Current level is the max. level of any item seen before.
        int currentLevel = everVisited.stream().map(n -> thisSet.get(n.getItemSUID()).getLevel()).max(Integer::compare).orElse(0);

        //Is on a level smaller than the next level according to this item 
        boolean a = currentLevel < level + 1;
        // All passed on this level?
        boolean b = currentlyPassed.stream().filter(n -> thisSet.get(n).getLevel() == level).count() == thisSet.stream().filter(n -> n.getLevel() == level).count();
        /*// And is the user not on the next level...
        int idOfFirstLevelFirst = thisSet.stream().filter(n -> n.getLevel() == level + 1 && n.getDependencies().isEmpty()).map(n -> n.getId()).findFirst().orElse(-1);
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
                                return n.getLevel() == level + 1 && deps.isEmpty();
                            })
                            .map(n -> opensrs.Utils.createLesson(uid, setCode, n.getId()))
                            .toArray(Review[]::new)
            ); //Queue new lessons
        }
        state.sendString("OK, created " + newLessons.length + " new lessons.");
    }
}
