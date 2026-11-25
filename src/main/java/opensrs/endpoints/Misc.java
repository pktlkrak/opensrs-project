/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package opensrs.endpoints;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import tools.jackson.databind.node.ObjectNode;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.LinkedList;
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
import opensrs.dataobjects.User;
import opensrs.filetransformers.FileTransformers;
import opensrs.sets.GlobalStore;
import opensrs.sets.Set;
import opensrs.sets.Set.SRSConfig.Binding;
import opensrs.sets.Set.SRSConfig.BindingType;
import org.autumn.State;
import org.autumn.annotations.Endpoint;
import org.autumn.annotations.FromCookie;
import org.autumn.annotations.FromPath;
import tools.jackson.databind.node.ArrayNode;

/**
 * Miscellaneous endpoints.
 * @author piotr
 */
public class Misc {
    
    public static final int MAX_IMAGE_LENGTH = 10_000_000; // ~10MiB

    private static Map<String, String> extMap;
    private final Database database;

    static {
        try {
            extMap = java.nio.file.Files.readAllLines(new File("mappings").toPath())
                    .stream()
                    .map(n -> n.split(" ", 2))
                    .filter(e -> e.length == 2)
                    .collect(Collectors.toMap(n -> n[0], n -> n[1]));
        } catch (IOException ex) {
            Log.handleException("Files", ex);
        }
    }

    public Misc(Database database) {
        this.database = database;
    }

    /**
     * Endpoint for accessing static paths.
     * @param state Autumn State
     * @param path  Path the generic endpoint is trying to access.
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
        InputStreamLength toRead;
        Set thisSet;
        String lastSubPath = pathTokens[pathTokens.length - 1];
        if (GlobalStore.hasSet(pathTokens[0])) {
            if (pathTokens.length == 1) {
                state.setHeader("Location", "index.html");
                state.sendResponseHeaders(302);
                return;
            }
            thisSet = GlobalStore.getSet(pathTokens[0]);
            String subPath = Arrays.asList(pathTokens).stream().skip(1).collect(Collectors.joining("/"));
            Binding setBinding = thisSet.config.getBinding(subPath);
            if(setBinding.getType() == BindingType.REDIRECT) {
                state.setHeader("Location", setBinding.getTarget());
                state.sendResponseHeaders(302);
                return;
            } else {
                subPath = setBinding.getTarget();
            }
            lastSubPath = subPath;
            toRead = thisSet.getFile(subPath);
        } else {
            toRead = GlobalStore.getCommonFile(path);
            thisSet = null;
        }
        if (toRead == null) {
            state.sendString("404 Not found", 404);
            return;
        }
        String extension = lastSubPath;
        if (extension.contains(".")) {
            extension = extension.substring(extension.lastIndexOf(".") + 1);
        }
        if (extMap.containsKey(extension)) {
            String contentType = extMap.get(extension);
            if(contentType.contains("~")) {
                String[] ctSteps = contentType.split(Pattern.quote("~"));
                contentType = ctSteps[0].strip();
                for(int i = 1; i < ctSteps.length; i++) {
                    toRead = FileTransformers.apply(ctSteps[i].strip(), path, thisSet, toRead);
                }
            }
            state.setHeader("Content-Type", contentType);
        }

        state.setOutputLength((int) toRead.getLength());
        state.sendResponseHeaders(200);
        Utils.copyStream(toRead.getInputStream(), state.getOutput(), 4096);
    }

    /**
     * The endpoint for information about the currently logged in user.
     * @param state     Autumn State.
     * @param session   Current session.
     * @throws IOException 
     */
    @Endpoint("/api/user")
    public void user(
            State state,
            @FromCookie( value = "session", optional = true) String session
    ) throws IOException {
        state.setHeader("Cache-Control", "no-store");
        int uid = SessionHolder.getUID(session);
        ObjectNode rootNode = opensrs.Utils.MAPPER.createObjectNode();
        rootNode.put("username", uid == -1 ? null : SessionHolder.getNameOfUser(session));
        rootNode.put("uid", uid);
        state.sendString(opensrs.Utils.MAPPER.writeValueAsString(rootNode), "application/json; charset=utf-8");
    }

    /**
     * The endpoint for retrieving the user's picture
     * @param state     Autumn State.
     * @param session   Current session.
     * @throws IOException 
     */
    @Endpoint(value = "/api/user/pic", allowedMethods = { "GET" })
    public void userPicture(
            State state,
            @FromCookie( value = "session", optional = true) String session
    ) throws IOException {
        int uid = SessionHolder.getUID(session);
        User.Image userImage = database.getUserImage(uid);
        if(userImage == null) {
            state.sendString("Not found", "text/plain", 404);
        } else {
            state.setOutputLength(userImage.image.length);
            state.setHeader("Content-Type", userImage.contentType);
            state.sendResponseHeaders(200);
            OutputStream os = state.getOutput();
            os.write(userImage.image);
            os.flush();
        }
    }

    /**
     * The endpoint for setting the user's picture
     * @param state     Autumn State.
     * @param session   Current session.
     * @throws IOException 
     */
    @Endpoint(value = "/api/user/pic", allowedMethods = { "POST" })
    public void setUserPicture(
            State state,
            @FromCookie( value = "session", optional = true) String session
    ) throws IOException {
        int uid = SessionHolder.getUID(session);
        if(uid == -1) {
            state.sendString("Unauthorized", "text/plain", 403);
        } else {
            List<String> length = state.getInboundHeaders().getOrDefault("Content-Length", new LinkedList<>());
            List<String> contentTypes = state.getInboundHeaders().getOrDefault("Content-Type", new LinkedList<>());
            if(length.size() != 1 || contentTypes.size() != 1) {
                state.sendString("Invalid request", "text/plain", 400);
            } else {
                long longLength = Long.parseLong(length.getFirst());
                if(longLength >= MAX_IMAGE_LENGTH) {
                    state.sendString("Image too long", "text/plain", 400);
                } else {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    Utils.copyStreamLimited(state.getInput(), baos, 4096, longLength);
                    database.updateUserImage(uid, new User.Image(baos.toByteArray(), contentTypes.getFirst()));
                    state.sendString("OK", "text/plain", 200);
                }
            }
        }
    }

    /**
     * Endpoint for the about section.
     * <p>It displays generic info: The version of the system and how many sets are Loaded.</p>
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
     * <p>Not every user might want to use every available Set so the users first need to register for the sets they actually want to study.</p>
     * @param state     Autumn State.
     * @param set       The set to register for.
     * @param seskey    The session key.
     * @throws IOException 
     */
    @Endpoint(value = "/api/{set}/register", allowedMethods = { "POST" })
    public void registerUserForSet(
            State state,
            @FromPath("set") String set,
            @FromCookie("session") String seskey
    ) throws IOException {
        int uid = SessionHolder.getUID(seskey);
        if(uid == -1) {
            state.sendString("Not logged in!", "text/plain", 403);
            return;
        }
        Review rev = database.fetchReview(new FilterChain<>(FilterChain.Junction.AND, Arrays.asList(
                new Filter<>(Review.Parameter.UID, Filters.EqOnly.EQUAL, uid),
                new Filter<>(Review.Parameter.SET_CODE, Filters.EqOnly.EQUAL, set.hashCode()))
        ), null);
        if(rev == null) {
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
