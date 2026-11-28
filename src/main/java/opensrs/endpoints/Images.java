/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package opensrs.endpoints;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;
import opensrs.SessionHolder;
import opensrs.database.Database;
import opensrs.database.Filter;
import opensrs.database.Filters;
import opensrs.dataobjects.Image;
import org.autumn.State;
import org.autumn.Utils;
import org.autumn.annotations.Endpoint;
import org.autumn.annotations.FromCookie;
import org.autumn.annotations.FromPath;
import org.autumn.annotations.FromQuery;
import org.autumn.annotations.Route;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Endpoints for uploading and listing images.
 *
 * @author piotr
 */
@Route("/api/images")
public class Images {

    public static final int MAX_IMAGE_LENGTH = 10_000_000; // ~10MiB
    public static final int PAGE_SIZE = 50;

    private final Database database;

    public Images(Database database) {
        this.database = database;
    }

    /**
     * Sends the image (shared w/ Misc)
     *
     * @param state Autumn State.
     * @param image The image.
     * @param contents The image's contents.
     * @throws IOException
     */
    static void sendImage(State state, Image image, byte[] contents) throws IOException {
        state.setOutputLength(contents.length);
        state.setHeader("Content-Type", image.getContentType());
        state.sendResponseHeaders(200);
        OutputStream os = state.getOutput();
        os.write(contents);
        os.flush();
    }

    /**
     * Lists the images by ID, a page at a time
     *
     * @param state Autumn State.
     * @param after The number of images to skip (optional).
     * @param session Current session.
     * @throws IOException
     */
    @Endpoint(value = "/list", allowedMethods = {"GET"})
    public void list(
            State state,
            @FromQuery(value = "after", optional = true) String after,
            @FromCookie(value = "session", optional = true) String session
    ) throws IOException {
        state.setHeader("Cache-Control", "no-store");
        if (SessionHolder.getUID(session) == -1) {
            state.sendString("Unauthorized", "text/plain", 403);
            return;
        }
        int skip = 0;
        if (after != null && !after.isEmpty()) {
            try {
                skip = Integer.parseInt(after.trim());
            } catch (NumberFormatException ex) {
                skip = -1;
            }
            if (skip < 0) {
                state.sendString("Invalid after", "text/plain", 400);
                return;
            }
        }
        // Use extra image as indicator of more images coming
        List<Image> images = database.listImages(skip, PAGE_SIZE + 1);
        if (images == null) {
            state.sendString("Could not list the images", "text/plain", 500);
            return;
        }
        ArrayNode an = opensrs.Utils.MAPPER.createArrayNode();
        for (Image image : images.subList(0, Math.min(images.size(), PAGE_SIZE))) {
            ObjectNode on = opensrs.Utils.MAPPER.createObjectNode();
            on.put("id", image.getId());
            on.put("type", image.getContentType());
            an.add(on);
        }
        ObjectNode root = opensrs.Utils.MAPPER.createObjectNode();
        root.set("images", an);
        root.put("hasMore", images.size() > PAGE_SIZE);
        state.sendJSON(opensrs.Utils.MAPPER.writeValueAsString(root));
    }

    /**
     * Returns the image with the given ID.
     *
     * @param state Autumn State.
     * @param id The image's ID.
     * @param session Current session.
     * @throws IOException
     */
    @Endpoint(value = "/{id}", allowedMethods = {"GET"})
    public void get(
            State state,
            @FromPath("id") String id,
            @FromCookie(value = "session", optional = true) String session
    ) throws IOException {
        if (SessionHolder.getUID(session) == -1) {
            state.sendString("Unauthorized", "text/plain", 403);
            return;
        }
        Image image;
        try {
            image = database.fetchImage(new Filter<>(Image.Parameter.IMAGE_ID, Filters.EqOnly.EQUAL, Integer.parseInt(id)), null);
        } catch (NumberFormatException ex) {
            image = null;
        }
        byte[] contents = image == null ? null : database.getImageContents(image.getId());
        if (contents == null) {
            state.sendString("Not found", "text/plain", 404);
            return;
        }
        // Images never change
        state.setHeader("Cache-Control", "private, max-age=86400");
        sendImage(state, image, contents);
    }

    /**
     * Stores the request body as a new image, typed by its Content-Type header. Responds with the image's ID.
     *
     * @param state Autumn State.
     * @param session Current session.
     * @throws IOException
     */
    @Endpoint(value = "/upload", allowedMethods = {"POST"}, tryParsingBody = false)
    public void upload(
            State state,
            @FromCookie(value = "session", optional = true) String session
    ) throws IOException {
        if (SessionHolder.getUID(session) == -1) {
            state.sendString("Unauthorized", "text/plain", 403);
            return;
        }
        List<String> lengths = state.getInboundHeaders().getOrDefault("Content-Length", new LinkedList<>());
        List<String> contentTypes = state.getInboundHeaders().getOrDefault("Content-Type", new LinkedList<>());
        if (lengths.size() != 1 || contentTypes.size() != 1) {
            state.sendString("Invalid request", "text/plain", 400);
            return;
        }
        String contentType = contentTypes.getFirst().trim();
        if (!contentType.toLowerCase().startsWith("image/")) {
            state.sendString("Only images can be uploaded", "text/plain", 415);
            return;
        }
        long length;
        try {
            length = Long.parseLong(lengths.getFirst().trim());
        } catch (NumberFormatException ex) {
            state.sendString("Invalid request", "text/plain", 400);
            return;
        }
        if (length <= 0) {
            state.sendString("Empty image", "text/plain", 400);
            return;
        }
        if (length >= MAX_IMAGE_LENGTH) {
            state.sendString("Image too long", "text/plain", 413);
            return;
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream((int) length);
        Utils.copyStreamLimited(state.getInput(), baos, 4096, length);
        if (baos.size() != length) {
            state.sendString("Incomplete image", "text/plain", 400);
            return;
        }
        Image image = new Image(contentType);
        database.createImage(image, baos.toByteArray());
        if (image.getId() == -1) {
            state.sendString("Could not store the image", "text/plain", 500);
            return;
        }
        ObjectNode on = opensrs.Utils.MAPPER.createObjectNode();
        on.put("id", image.getId());
        state.sendJSON(opensrs.Utils.MAPPER.writeValueAsString(on));
    }
}
