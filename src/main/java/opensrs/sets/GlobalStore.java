/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package opensrs.sets;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import opensrs.InputStreamLength;
import opensrs.Log;

/**
 * The object that holds the information about the currently loaded sets.
 * <p>This is the main object through which data about the sets is accesed</p>
 * @author piotr
 */
public class GlobalStore {
    private static final Map<String, Set> SETSTORE = new HashMap<>();
    private static final File COMMONS_ROOT = new File("common");
    private static final File SETS_ROOT = new File("sets");
    static{
        reload();
    }
    
    /**
     * Reloads the data in {@link GlobalStore#SETSTORE}.
     */
    public static void reload(){
        SETSTORE.clear();
        for(File file : SETS_ROOT.listFiles()){
            if(file.isDirectory() && file.getName().endsWith(".set")){
                SETSTORE.put(file.getName(), Set.getSetFromFilesystem(file));
            }
        }
    }
    
    /**
     * Gets the available sets from {@link GlobalStore#SETSTORE}.
     * @return  returns the array of set names
     */
    public static String[] getAvailableSets(){
        return SETSTORE.keySet().toArray(new String[0]);
    }
    
    /**
     * Gets a set from {@link GlobalStore#SETSTORE} by name.
     * @param name  The name of the set.
     * @return      Returns the set.
     */
    public static Set getSet(String name){
        return SETSTORE.get(name);
    }
    
    /**
     * Returns a file from the common store.
     * @param name  The name of the file.
     * @return      The file returned in the {@link InputStreamLength} format.
     */
    public static InputStreamLength getCommonFile(String name){
        Log.log(Log.INFO, "GlobalStore", "Trying to access public file " + name);
        try {
            if(name.startsWith("/")) name = name.substring(1);
            File file = new File(COMMONS_ROOT, name);
            if(!file.isFile()) return null;
            return new InputStreamLength(new FileInputStream(file), file.length());
        } catch (FileNotFoundException ex) {
            return null;
        }
    }

    /**
     * Checks if the given set exists in {@link GlobalStore#SETSTORE}.
     * @param set   The set name.
     * @return      True of False.
     */
    public static boolean hasSet(String set) {
        return SETSTORE.containsKey(set);
    }
}
