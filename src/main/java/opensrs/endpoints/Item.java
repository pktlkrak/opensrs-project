/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package opensrs.endpoints;

import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import opensrs.SessionHolder;
import opensrs.Utils;
import opensrs.database.Database;
import opensrs.sets.GlobalStore;
import opensrs.sets.SRSObject;
import opensrs.sets.Set;
import org.autumn.State;
import org.autumn.annotations.Endpoint;
import org.autumn.annotations.FromCookie;
import org.autumn.annotations.FromPath;
import org.autumn.annotations.Route;

/**
 * Endpoint to access specific reviewable items.
 * @author piotr
 */
@Route("/api/{set}/item")
public class Item {

    private final Database database;

    public Item(Database database) {
        this.database = database;
    }

    /**
     * Endpoint to access a reviewable by it's family. 
     * @param state     Autumn State.
     * @param setName   The name of the set the reviewable is in.
     * @param family    The family of the reviewable (ex. Kanji).
     * @param query     The query - the reviewable itself.
     * @param session   The current user session.
     * @throws IOException 
     */
    @Endpoint("/by-family/{family}/{query}")
    public void byFamily(
            State state,
            @FromPath("set") String setName,
            @FromPath("family") String family,
            @FromPath("query") String query,
            @FromCookie("session") String session
    ) throws IOException {
        if (!GlobalStore.hasSet(setName)) {
            state.sendString("{\"errorCode\": 404, \"message\": \"No such set\"}", 404);
        }
        Set set = GlobalStore.getSet(setName);
        List<SRSObject> matching = set.stream().filter(n -> n.getFamily().equals(family) && n.getQuery().equals(query)).collect(Collectors.toList());

        ArrayNode node = Utils.MAPPER.valueToTree(matching);

        node.forEach(n -> Utils.appendAdditionalToJSON((ObjectNode) n, set));

        int uid = SessionHolder.getUID(session);
        if (uid != -1) {
            node.forEach(Utils.appendUserdataToJSON(database, setName, uid));
        }
        state.sendString(Utils.MAPPER.writeValueAsString(node), "application/JSON; charset=UTF-8");
    }
    
    /**
     * The endpoint to access all the reviewables of a certain level TODO: is this true chat?
     * @param state     Autumn State
     * @param setName   The name of the set the reviewable is in. 
     * @param levelS    The level of which we want the reviewables of. TODO: why levelS instead of level?
     * @param session   The current user session. 
     * @throws IOException 
     */
    @Endpoint("/by-level/{level}")
    public void byLevel(
            State state,
            @FromPath("set") String setName,
            @FromPath("level") String levelS,
            @FromCookie("session") String session
    ) throws IOException{
        if (!GlobalStore.hasSet(setName)) {
            state.sendString("{\"errorCode\": 404, \"message\": \"No such set\"}", 404);
        }
        Set set = GlobalStore.getSet(setName);
        int level;
        try{
            level = Integer.valueOf(levelS);
        }catch(NumberFormatException ex){
            state.sendString("{\"errorCode\": 404, \"message\": \"No such item\"}", 404);
            return;
        }
        ArrayNode matchingNode = Utils.MAPPER.valueToTree(set.stream()
                .filter(n -> n.getLevel() == level)
                .collect(Collectors.toList()));
        matchingNode.forEach(n -> Utils.appendAdditionalToJSON((ObjectNode) n, set));
        
        int uid = SessionHolder.getUID(session);
        if(uid != -1){
            matchingNode.forEach(Utils.appendUserdataToJSON(database, setName, uid));
        }
        state.sendString(Utils.MAPPER.writeValueAsString(matchingNode), "application/JSON; charset=UTF-8");
    }

    /**
     * Endpoint to access the reviewable by it's ID. TODO: is this the reviewable id in the database or in the set?
     * @param state     AutumnState
     * @param setName   The name of the set the reviewable is in. TODOP: Not actually in the params?
     * @param id        The ID of the reviewable.
     * @param session   The current user session.
     * @throws IOException 
     */
    @Endpoint("/by-id/{id}")
    public void byID(
            State state,
            @FromPath("set") String setName,
            @FromPath("id") String id,
            @FromCookie("session") String session
    ) throws IOException {
        if (!GlobalStore.hasSet(setName)) {
            state.sendString("{\"errorCode\": 404, \"message\": \"No such set\"}", 404);
        }
        int idI;
        try {
            idI = Integer.valueOf(id);
        } catch (NumberFormatException ex) {
            state.sendString("{\"errorCode\": 404, \"message\": \"No such item\"}", 404);
            return;
        }
        Set set = GlobalStore.getSet(setName);
        SRSObject matching = set.stream().filter(n -> n.getId() == idI).findFirst().orElse(null);
        if (matching == null) {
            state.sendString("{\"errorCode\": 404, \"message\": \"No such item\"}", 404);
            return;
        }
        int uid = SessionHolder.getUID(session);

        ObjectNode node = Utils.MAPPER.valueToTree(matching);
        Utils.appendAdditionalToJSON(node, set);

        if (uid != -1) {
            Utils.appendUserdataToJSON(database, setName, uid).accept(node);
        }
        state.sendString(Utils.MAPPER.writeValueAsString(node), "application/JSON; charset=UTF-8");

    }

}
