/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package opensrs.sets;

import tools.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author piotr
 */
public class SRSObject {
    private final int id;
    private final String family;
    private final int level;
    private final String query; //The thing it's gonna show you on the rev. page, like a kanji 
    
    private final LinkedHashMap<String, List<String>> reviewable = new LinkedHashMap<>();
    private final ObjectNode clientData;
    private final List<Integer> dependencies = new ArrayList<>();
    private final Map<String, String> coerce = new HashMap<>();

    public SRSObject(int id, String type, int stage, String query, ObjectNode renderInfoTree) {
        this.id = id;
        this.family = type;
        this.level = stage;
        this.query = query;
        this.clientData = renderInfoTree;
    }

    public String getQuery() {
        return query;
    }
    
    public Map<String, String> getCoerce() {
        return coerce;
    }

    public int getId() {
        return id;
    }

    public String getFamily() {
        return family;
    }

    public int getLevel() {
        return level;
    }

    public LinkedHashMap<String, List<String>> getReviewable() {
        return reviewable;
    }

    public ObjectNode getClientData() {
        return clientData;
    }

    public List<Integer> getDependencies() {
        return dependencies;
    }
    
    
}
