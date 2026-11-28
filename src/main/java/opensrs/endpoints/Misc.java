/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package opensrs.endpoints;

import java.io.ByteArrayInputStream;
import tools.jackson.databind.node.ObjectNode;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import opensrs.InputStreamLength;
import opensrs.Log;
import opensrs.OpenSRS;
import opensrs.SessionHolder;
import org.autumn.Utils;
import opensrs.database.Database;
import opensrs.database.Filter;
import opensrs.database.FilterChain;
import opensrs.database.Filters;
import opensrs.dataobjects.Review;
import opensrs.dataobjects.Image;
import opensrs.filetransformers.Bindings;
import opensrs.filetransformers.FileTransformers;
import opensrs.sets.GlobalStore;
import opensrs.sets.Set;
import org.autumn.State;
import org.autumn.annotations.Endpoint;
import org.autumn.annotations.FromCookie;
import org.autumn.annotations.FromPath;
import org.autumn.annotations.FromQuery;
import tools.jackson.databind.node.ArrayNode;

/**
 * Miscellaneous endpoints.
 *
 * @author piotr
 */
public class Misc {
    
    private final Database database;
    
    public Misc(Database database) {
        this.database = database;
    }

    /**
     * Endpoint for accessing static paths.
     *
     * @param state Autumn State
     * @param path Path the generic endpoint is trying to access.
     * @throws IOException
     */
    @Endpoint("/{path}")
    public void access(
            State state,
            @FromPath("path") String path
    ) throws IOException {
        state.setHeader("Cache-Control", "no-store");
        if (path == null) {
            state.setHeader("Location", "/index.html");
            state.sendResponseHeaders(302);
            return;
        }
        if (path.contains("..")) {
            state.sendString("Invalid path", 404);
            return;
        }
        path = path.split(Pattern.quote("?"))[0];
        String[] pathTokens = path.split("/");
        Set thisSet;
        Bindings.BindingsOutput rebound;
        if (GlobalStore.hasSet(pathTokens[0])) {
            thisSet = GlobalStore.getSet(pathTokens[0]);
            if (pathTokens.length == 1 || pathTokens.length == 0) {
                state.setHeader("Location", thisSet.config.getBindings().getRoot());
                state.sendResponseHeaders(302);
                return;
            }
            String subPath = Arrays.asList(pathTokens).stream().skip(1).collect(Collectors.joining("/"));
            rebound = thisSet.config.getBindings().process(subPath);
        } else {
            if(path.equals("") || path.equals("/")) {
                state.setHeader("Location", GlobalStore.getBindings().getRoot());
                state.sendResponseHeaders(302);
                return;
            }
            rebound = GlobalStore.getBindings().process(path);
            thisSet = null;
        }
        if(rebound == null) {
            state.sendString("404 Not found", 404);
            return;
        }

        if (rebound.getRedirectDestination() != null) {
            state.setHeader("Location", rebound.getRedirectDestination());
            state.sendResponseHeaders(302);
            return;
        }

        String mimeType = rebound.getMimeType();
        if(mimeType == null) mimeType = GlobalStore.getBindings().getMimeTypeFor(path, rebound);
        if (mimeType != null) {
            state.setHeader("Content-Type", mimeType);
        }
        
        state.setOutputLength((int) rebound.getFileStream().getLength());
        state.sendResponseHeaders(200);
        Utils.copyStream(rebound.getFileStream().getInputStream(), state.getOutput(), 4096);
    }

    /**
     * The endpoint for information about the currently logged in user.
     *
     * @param state Autumn State.
     * @param session Current session.
     * @throws IOException
     */
    @Endpoint("/api/user")
    public void user(
            State state,
            @FromCookie(value = "session", optional = true) String session
    ) throws IOException {
        state.setHeader("Cache-Control", "no-store");
        int uid = SessionHolder.getUID(session);
        ObjectNode rootNode = opensrs.Utils.MAPPER.createObjectNode();
        rootNode.put("username", uid == -1 ? null : SessionHolder.getNameOfUser(session));
        rootNode.put("uid", uid);
        state.sendString(opensrs.Utils.MAPPER.writeValueAsString(rootNode), "application/json; charset=utf-8");
    }

    /**
     * The endpoint for retrieving the user's picture in a set, or the first one assigned when no set is given
     *
     * @param state Autumn State.
     * @param set The set (optional).
     * @param session Current session.
     * @throws IOException
     */
    @Endpoint(value = "/api/user/pic", allowedMethods = {"GET"})
    public void userPicture(
            State state,
            @FromQuery(value = "set", optional = true) String set,
            @FromCookie(value = "session", optional = true) String session
    ) throws IOException {
        state.setHeader("Cache-Control", "no-store");
        int uid = SessionHolder.getUID(session);
        Image userImage = null;
        if (uid != -1) {
            userImage = set == null || set.isEmpty()
                    ? database.getUserImage(uid)
                    : database.getUserImage(uid, set.hashCode());
        }
        byte[] contents = userImage == null ? null : database.getImageContents(userImage.getId());
        if (contents == null) {
            state.sendString("Not found", "text/plain", 404);
        } else {
            Images.sendImage(state, userImage, contents);
        }
    }

    /**
     * The endpoint for assigning an uploaded image to the user in a set
     *
     * @param state Autumn State.
     * @param set The set.
     * @param imageId The image's ID.
     * @param session Current session.
     * @throws IOException
     */
    @Endpoint(value = "/api/user/assignimage", allowedMethods = {"POST"})
    public void assignUserImage(
            State state,
            @FromQuery("set") String set,
            @FromQuery("imageid") String imageId,
            @FromCookie(value = "session", optional = true) String session
    ) throws IOException {
        int uid = SessionHolder.getUID(session);
        if (uid == -1) {
            state.sendString("Unauthorized", "text/plain", 403);
            return;
        }
        if (!GlobalStore.hasSet(set)) {
            state.sendString("No such set", "text/plain", 404);
            return;
        }
        int id;
        try {
            id = Integer.parseInt(imageId);
        } catch (NumberFormatException ex) {
            state.sendString("Invalid image ID", "text/plain", 400);
            return;
        }
        if (database.assignUserImage(uid, set.hashCode(), id)) {
            state.sendString("OK", "text/plain", 200);
        } else {
            state.sendString("No such image", "text/plain", 404);
        }
    }

    /**
     * Endpoint for the about section.
     * <p>
     * It displays generic info: The version of the system and how many sets are
     * Loaded.</p>
     *
     * @param state Autumn State.
     * @throws IOException
     */
    @Endpoint("/about")
    public void about(
            State state
    ) throws IOException {
        state.sendString(new StringBuilder()
                .append(OpenSRS.VERSION)
                .append("\n")
                .append("There are ")
                .append(GlobalStore.getAvailableSets().length)
                .append(" set(s) loaded.")
                .toString()
        );
    }

    /**
     * Endpoint that returns the list of all available sets.
     *
     * @param state Autumn State
     * @throws IOException
     */
    @Endpoint("/api/sets")
    public void listSets(
            State state,
            @FromCookie(value = "session", optional = true) String session
    ) throws IOException {
        ArrayNode an = opensrs.Utils.MAPPER.createArrayNode();
        int uid = SessionHolder.getUID(session);
        for (String setName : GlobalStore.getAvailableSets()) {
            ObjectNode on = opensrs.Utils.MAPPER.createObjectNode();
            on.put("name", GlobalStore.getSet(setName).config.getName());
            on.put("id", setName);
            if (uid != -1) {
                Review rev = database.fetchReview(
                        new FilterChain<>(FilterChain.Junction.AND, List.of(
                                new Filter<>(Review.Parameter.SET_CODE, Filters.EqOnly.EQUAL, setName.hashCode()),
                                new Filter<>(Review.Parameter.UID, Filters.EqOnly.EQUAL, uid)
                        )), null);
                on.put("unlocked", rev != null);
            }
            an.add(on);
        }
        state.sendJSON(opensrs.Utils.MAPPER.writeValueAsString(an));
    }

    /**
     * Endpoint for the user to register for a certain Set.
     * <p>
     * Not every user might want to use every available Set so the users first
     * need to register for the sets they actually want to study.</p>
     *
     * @param state Autumn State.
     * @param set The set to register for.
     * @param seskey The session key.
     * @throws IOException
     */
    @Endpoint(value = "/api/{set}/register", allowedMethods = {"POST"})
    public void registerUserForSet(
            State state,
            @FromPath("set") String set,
            @FromCookie("session") String seskey
    ) throws IOException {
        int uid = SessionHolder.getUID(seskey);
        if (uid == -1) {
            state.sendString("Not logged in!", "text/plain", 403);
            return;
        }
        Review rev = database.fetchReview(new FilterChain<>(FilterChain.Junction.AND, Arrays.asList(
                new Filter<>(Review.Parameter.UID, Filters.EqOnly.EQUAL, uid),
                new Filter<>(Review.Parameter.SET_CODE, Filters.EqOnly.EQUAL, set.hashCode()))
        ), null);
        if (rev == null) {
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
            state.sendString("OK", "text/plain");
        } else {
            state.sendString("Already registered!", "text/plain", 400);
        }
    }
}
