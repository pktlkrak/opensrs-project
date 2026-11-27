/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package opensrs.sets;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import opensrs.InputStreamLength;
import opensrs.Log;
import opensrs.Utils;
import opensrs.filetransformers.Bindings;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * The object that holds the information about the currently loaded sets.
 * <p>This is the main object through which data about the sets is accessed</p>
 * @author piotr
 */
public class GlobalStore {
    private static final Map<String, Set> SETSTORE = new HashMap<>();
    private static final File COMMONS_ROOT = new File("common");
    private static final File SETS_ROOT = new File("sets");
    private static Bindings globalStoreBindings = null;
    public static void initialize() {
        if(globalStoreBindings != null) return;
        reload();
        File mappingsFile = new File("mappings.XML");
        if(!mappingsFile.isFile()) {
            Log.log(Log.ERROR, "GlobalStore", "No mappings.XML file found - mime types will be toast!");
        } else {
            try {
                Document configDocument = Utils.documentBuilder.parse(mappingsFile);
                Element root = (Element) configDocument.getElementsByTagName("bindings").item(0);
                globalStoreBindings = new Bindings(null, root);
                if(root == null) throw new RuntimeException("The config file mappings.XML has no <bindings> root!");
            } catch (SAXException | IOException ex) {
                throw new RuntimeException(ex); // At-init, this should kill the server.
            }
        }
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
     * @return      The file or null if not found.
     */
    public static File getCommonFile(String name){
        Log.log(Log.INFO, "GlobalStore", "Trying to access public file " + name);
        if(name.startsWith("/")) name = name.substring(1);
        File file = new File(COMMONS_ROOT, name);
        if(!file.isFile()) return null;
        return file;
    }

    /**
     * Checks if the given set exists in {@link GlobalStore#SETSTORE}.
     * @param set   The set name.
     * @return      True of False.
     */
    public static boolean hasSet(String set) {
        return SETSTORE.containsKey(set);
    }

    /**
     * Retrieves the global store bindings
     
     * @return      Global store bindings
     */
    public static Bindings getBindings() {
        return globalStoreBindings;
    }
}
