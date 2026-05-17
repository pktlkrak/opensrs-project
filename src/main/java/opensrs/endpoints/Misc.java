/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package opensrs.endpoints;

import tools.jackson.databind.node.ObjectNode;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
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
import opensrs.sets.GlobalStore;
import opensrs.sets.Set;
import org.autumn.State;
import org.autumn.annotations.Endpoint;
import org.autumn.annotations.FromCookie;
import org.autumn.annotations.FromPath;
import tools.jackson.databind.node.ArrayNode;

/**
 * Miscelanious endpoints.
 * @author piotr
 */
public class Misc {

    private static Map<String, String> extMap;
    private final Database database;

    static {
        try {
            extMap = java.nio.file.Files.readAllLines(new File("mappings").toPath())
                    .stream()
                    .map(n -> n.split(" "))
                    .collect(Collectors.toMap(n -> n[0], n -> n[1]));
        } catch (IOException ex) {
            Log.handleException("Files", ex);
        }
    }

    public Misc(Database database) {
        this.database = database;
    }

    /**
     * Generic endpoint to access a path TODO: is this true?
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
        if (GlobalStore.hasSet(pathTokens[0])) {
            if (pathTokens.length == 1) {
                state.setHeader("Location", "index.html");
                state.sendResponseHeaders(302);
                return;
            }
            Set thisSet = GlobalStore.getSet(pathTokens[0]);
            String subPath = Arrays.asList(pathTokens).stream().skip(1).collect(Collectors.joining("/"));
            subPath = thisSet.config.getBinding(subPath);
            toRead = thisSet.getFile(subPath);
        } else {
            toRead = GlobalStore.getCommonFile(path);
        }
        if (toRead == null) {
            state.sendString("404 Not found", 404);
            return;
        }
        String extension = pathTokens[pathTokens.length - 1];
        if (extension.contains(".")) {
            extension = extension.substring(extension.lastIndexOf(".") + 1);
        }
        if (extMap.containsKey(extension)) {
            state.setHeader("Content-Type", extMap.get(extension));
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
     * @param state Autumn State
     * @throws IOException 
     */
    @Endpoint("/api/sets")
    public void listSets(
            State state
    ) throws IOException {
        ArrayNode an = opensrs.Utils.MAPPER.createArrayNode();
        for(String string : GlobalStore.getAvailableSets()) an.add(string);
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
