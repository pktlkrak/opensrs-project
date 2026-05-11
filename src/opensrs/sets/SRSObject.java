/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package opensrs.sets;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * TODO: FIX
 * Class defining a single SRS object.
 *
 * <p>Provides an object for interacting with a single SRS object, which is a single,
 * reviewable thing like a kanji. </P>
 * 
 * <p>Example Usage</p>
 * TODO: ADD
 *
 * @author piotr
 */
public class SRSObject {
    /**
     * The id of the object.
     */
    private final int id;
    
    /**
     * What type of object this is (ex. Kanji, Hiragana)
     */
    private final String family;
    
    /**
     * What level of mastery the user is currently on (TODO: the masty names cuz i dont remember them)
    */
    private final int level;
    
    /**
     * The thing it's gonna show you on the rev. page, like a kanji
     * This is basicly the "question"
    */
    private final String query; //The thing it's gonna show you on the rev. page, like a kanji 
    
    /**
     * TODO: ??? is this
     */
    private final LinkedHashMap<String, List<String>> reviewable = new LinkedHashMap<>();
    
    /** 
     * TODO: don't member
     */
    private final ObjectNode clientData;
    
    /**
     * The prerequsites required for the 
     */
    private final List<Integer> dependencies = new ArrayList<>();
    
    /**
     * This are hints that are shown if the user is a bit off the correct anwser
     * (TODO: Add a good example)
     */
    private final Map<String, String> coerce = new HashMap<>();

    /**
     * The constuctor  (TODO ig)
     * @param id 
     * @param type
     * @param stage
     * @param query
     * @param renderInfoTree 
     */
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
