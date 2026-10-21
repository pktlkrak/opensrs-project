/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package opensrs.endpoints;

import java.io.IOException;
import java.util.List;
import opensrs.SessionHolder;
import opensrs.Utils;
import opensrs.database.Database;
import opensrs.database.Filter;
import opensrs.database.FilterChain;
import opensrs.database.Filters;
import opensrs.dataobjects.Review;
import opensrs.dataobjects.User;
import opensrs.sets.GlobalStore;
import org.autumn.State;
import org.autumn.annotations.Endpoint;
import org.autumn.annotations.FromBody;
import org.autumn.annotations.FromCookie;
import org.autumn.annotations.FromPath;
import org.autumn.annotations.Route;

/**
 * Endpoint utilized for testing and debugging
 * @author piotr
 */
@Route("/management")
public class Management {
    
    public static final boolean USERS_CAN_SELF_ENROLL = true;

    private final Database database;

    public Management(Database database) {
        this.database = database;
    }

    /**
     * Management endpoint to initialize a set for a user with the starting values for each reviewable object.
     * @param state     Autumn State.
     * @param set       Which set to initialize.
     * @param user      Which user to initialize for.
     * @param session   The current session of the user.
     * @throws IOException 
     */
    @Endpoint("/initSetForUser/{set}/{user}")
    public void initSetForUser(
            State state,
            @FromPath("set") String set,
            @FromPath("user") String user,
            @FromCookie("session") String session
    ) throws IOException {
        state.setHeader("Cache-Control", "no-store");
        int uid = Integer.parseInt(user);
        int localUid = SessionHolder.getUID(session);
        if(localUid == 0 || (USERS_CAN_SELF_ENROLL && uid == localUid)) {
            Review anyReview = database.fetchReview(
                    new FilterChain<>(FilterChain.Junction.AND, List.of(
                            new Filter<>(Review.Parameter.UID, Filters.EqOnly.EQUAL, uid),
                            new Filter<>(Review.Parameter.SET_CODE, Filters.EqOnly.EQUAL, set.hashCode()))), null);
            if(anyReview == null) {
                // No queued = no collisions
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
            } else {
                state.sendString("Already enrolled", 400);
            }
        } else {
            state.sendString("Access denied", 403);
        }
    }

    /**
     * Force the unlock of a reviewable, regardless of if the user met it's prerequisites.
     * @param state     Autumn State.
     * @param user      The user to unlock for.
     * @param set       The set to unlock for.
     * @param id        The id of the reviewable.
     * @param session   The current session of the user.
     * @throws IOException 
     */
    @Endpoint("/forceUnlock/{user}/{set}/{id}")
    public void forceUnlock(
            State state,
            @FromPath("user") String user,
            @FromPath("set") String set,
            @FromPath("id") String id,
            @FromCookie("session") String session
    ) throws IOException {
        if(SessionHolder.getUID(session) != 0) {
            state.sendString("Not root user", 403);
            return;
        }
        database.createReviews(Utils.createLesson(Integer.valueOf(user), set.hashCode(), Integer.valueOf(id)));
        state.sendString("OK");
    }

    /**
     * Reloads the current state of {@link GlobalStore}.
     * @param state         Autumn State.
     * @param session   The current session of the user.
     * @throws IOException
     */
    @Endpoint("/reload")
    public void reload(State state, @FromCookie("session") String session) throws IOException {
        if(SessionHolder.getUID(session) != 0) {
            state.sendString("Not root user", 403);
            return;
        }
        GlobalStore.reload();
        state.sendString("OK");
    }

    /**
     * Get the info about the user in the current session.
     * <p>This can tell if the user is logged in or not, and what is the UID of the logged in user.</p>
     * @param state     Autumn State.
     * @param session   The session for which we check the user status.
     * @throws IOException 
     */
    @Endpoint("/user")
    public void user(
            State state,
            @FromCookie("session") String session
    ) throws IOException {
        state.setHeader("Cache-Control", "no-store");
        int uid = SessionHolder.getUID(session);
        state.sendString(uid == -1 ? "Not logged in" : "Logged in as UID " + uid + " with name '" + SessionHolder.getNameOfUser(session) + "'");
    }

    /**
     * Change the password of the currently logged in user.
     * @param state     Autumn State.
     * @param session   The session for which we check the user status.
     * @param username  The name of the user whose password is to be changed.
     * @param password  The new password
     * @throws IOException 
     */
    @Endpoint(value = "/changepw", allowedMethods = {"POST"})
    public void changePassword(
            State state,
            @FromCookie("session") String session,
            @FromBody("username") String username,
            @FromBody("password") String password
    ) throws IOException {
        state.setHeader("Cache-Control", "no-store");
        int uid = SessionHolder.getUID(session);
        User userToChange = database.getUserByName(username);
        if(userToChange == null) {
            state.sendString("Not found", 404);
        } else if(uid == 0 || uid == userToChange.getUid()) {
            int orgUid = userToChange.getUid();
            userToChange = User.fromPassword(username, password);
            userToChange.setUid(orgUid);
            database.updateUser(userToChange);
            state.sendString("OK");
        } else {
            state.sendString("Access denied.", 403);
        }
    }
}
