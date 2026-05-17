/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package opensrs.endpoints;

import java.io.IOException;
import opensrs.SessionHolder;
import opensrs.Utils;
import opensrs.database.Database;
import opensrs.dataobjects.Review;
import opensrs.sets.GlobalStore;
import org.autumn.State;
import org.autumn.annotations.Endpoint;
import org.autumn.annotations.FromCookie;
import org.autumn.annotations.FromPath;
import org.autumn.annotations.Route;

/**
 * Endpoint utilized for testing and debugging
 * @author piotr
 */
@Route("/debug")
public class Debug {

    private final Database database;

    public Debug(Database database) {
        this.database = database;
    }

    /**
     * Debug endpoint to initialise a set for a user with the starting values for each reviewable object.
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

    /**
     * Force the unlock of a reviewable, regardless of if the user met it's prerequisites.
     * @param state Autumn State.
     * @param user  The user to unlock for.
     * @param set   The set to unlock for.
     * @param id    The id of the reviewable.
     * @throws IOException 
     */
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

    /**
     * Reloads the current state of {@link GlobalStore}.
     * @param state         Autumn State.
     * @throws IOException
     */
    @Endpoint("/reload")
    public void reload(State state) throws IOException {
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
}
