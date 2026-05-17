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
import opensrs.sets.GlobalStore;
import opensrs.sets.SRSObject;
import opensrs.sets.Set;
import org.autumn.State;
import org.autumn.annotations.Endpoint;
import org.autumn.annotations.FromCookie;
import org.autumn.annotations.FromPath;
import org.autumn.annotations.Route;

/**
 * Endpoint to monitor the progress of the user.
 * @author piotr
 */
@Route("/api/{set}/progress")
public class UserProgress {

    private Database database;

    public UserProgress(Database database) {
        this.database = database;
    }

    /**
     * The endpoint that allows to view how many of each type of reviewable the user has learned at what level. TODO: is this true?
     * @param state     Autumn State.
     * @param setName   The name of the set.
     * @param session   Current session.
     * @throws IOException 
     */
    @Endpoint("/phases")
    public void phases(
            State state,
            @FromPath("set") String setName,
            @FromCookie("session") String session
    ) throws IOException {
        state.setHeader("Cache-Control", "no-store");
        int uid = SessionHolder.getUID(session);
        if (uid == -1) {
            Utils.sendNotLoggedInJSON(state);
            return;
        }
        if (Utils.isSetInvalid(setName, state)) {
            return;
        }
        Set set = GlobalStore.getSet(setName);
        List<SRSObjectState> srsstate = database.fetchState(
                new FilterChain<>(FilterChain.Junction.AND, Arrays.asList(
                        new Filter<>(SRSObjectState.Parameter.SET_CODE, Filters.EqOnly.EQUAL, setName.hashCode()),
                        new Filter<>(SRSObjectState.Parameter.UID, Filters.EqOnly.EQUAL, uid)
                )), -1);
        Map<String, Map<Integer, Integer>> typeMapped = new HashMap<>();
        for (SRSObjectState obj : srsstate) {
            if (obj.getWeight() < 1) {
                continue;
            }
            String type = set.get(obj.getItemSUID()).getFamily();
            if (!typeMapped.containsKey(type)) {
                typeMapped.put(type, new HashMap<>());
            }
            typeMapped.get(type).put(obj.getItemSUID(), set.config.getPhaseForStage(obj.getWeight()));
        }
        ObjectNode main = Utils.MAPPER.valueToTree(typeMapped);
        ArrayNode types = Utils.MAPPER.createArrayNode();
        set.stream().map(SRSObject::getFamily).distinct().forEach(types::add);
        ObjectNode rootNode = Utils.MAPPER.createObjectNode();
        rootNode.set("counts", main);
        rootNode.set("types", types);
        List<SRSObjectState> everVisited = database.fetchState(
                new FilterChain<>(FilterChain.Junction.AND, Arrays.asList(
                        new Filter<>(SRSObjectState.Parameter.SET_CODE, Filters.EqOnly.EQUAL, setName.hashCode()),
                        new Filter<>(SRSObjectState.Parameter.UID, Filters.EqOnly.EQUAL, uid))), -1);
        rootNode.put("level", everVisited.stream().map(n -> set.get(n.getItemSUID()).getLevel()).max(Integer::compare).orElse(0));
        state.sendString(Utils.MAPPER.writeValueAsString(rootNode), "application/json; charset=utf-8");
    }
}
