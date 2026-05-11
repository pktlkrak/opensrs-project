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
 *
 * @author piotr
 */


public class GlobalStore {
    private static final Map<String, Set> SETSTORE = new HashMap<>();
    private static final File COMMONS_ROOT = new File("common");
    private static final File SETS_ROOT = new File("sets");
    static{
        reload();
    }
    
    public static void reload(){
        SETSTORE.clear();
        for(File file : SETS_ROOT.listFiles()){
            if(file.isDirectory() && file.getName().endsWith(".set")){
                SETSTORE.put(file.getName(), Set.getSetFromFilesystem(file));
            }
        }
    }
    
    public static String[] getAvailableSets(){
        return SETSTORE.keySet().toArray(new String[0]);
    }
    
    public static Set getSet(String name){
        return SETSTORE.get(name);
    }
    
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

    public static boolean hasSet(String set) {
        return SETSTORE.containsKey(set);
    }
}
