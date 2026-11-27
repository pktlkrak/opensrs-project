/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package opensrs.filetransformers;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import opensrs.InputStreamLength;
import opensrs.Log;
import opensrs.sets.Set;
import org.reflections.Reflections;
import org.reflections.scanners.FieldAnnotationsScanner;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.w3c.dom.Element;

/**
 *
 * @author asivery
 */
public class FileTransformers {
    private static final String TAG = "FileTransformers";
    private static final Map<String, Constructor<? extends FileTransformer>> AVAILABLE_TRANSFORMERS = new HashMap<>();
    
    public static void findTransformers() throws NoSuchMethodException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Reflections base = new Reflections("opensrs",
                new TypeAnnotationsScanner(),
                new SubTypesScanner(),
                new MethodAnnotationsScanner(),
                new FieldAnnotationsScanner());
        for(Class<? extends FileTransformer> x : base.getSubTypesOf(FileTransformer.class)) {
            FileTransformerName nameDecl = x.getAnnotation(FileTransformerName.class);
            if(nameDecl == null) {
                Log.log(Log.ERROR, TAG, "Transformer " + x.getCanonicalName() + " is missing a @FileTransformerName() annotation!");
                continue;
            }
            String name = nameDecl.value();
            AVAILABLE_TRANSFORMERS.put(name, x.getDeclaredConstructor(Set.class, Element.class));
            Log.log(Log.INFO, TAG, "Found transformer " + x.getName() + " (" + name + ")");
        }
    }
    
    public static FileTransformer initializeTransformer(String name, Set set, Element initializer) {
        try {
            Constructor <? extends FileTransformer> ftc = AVAILABLE_TRANSFORMERS.getOrDefault(name, null);
            if(ftc == null) throw new RuntimeException("No such transformer: " + name);
            return ftc.newInstance(set, initializer);
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            throw new RuntimeException(ex);
        }
    }
}
