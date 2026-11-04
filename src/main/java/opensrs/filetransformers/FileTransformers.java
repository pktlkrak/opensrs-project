/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package opensrs.filetransformers;

import java.io.IOException;
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

/**
 *
 * @author asivery
 */
public class FileTransformers {
    private static final String TAG = "FileTransformers";
    private static final Map<String, FileTransformer> AVAILABLE_TRANSFORMERS = new HashMap<>();
    
    public static void findTransformers() throws NoSuchMethodException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Reflections base = new Reflections("opensrs",
                new TypeAnnotationsScanner(),
                new SubTypesScanner(),
                new MethodAnnotationsScanner(),
                new FieldAnnotationsScanner());
        for(Class<? extends FileTransformer> x : base.getSubTypesOf(FileTransformer.class)) {
            FileTransformer transformer = x.getConstructor().newInstance();
            AVAILABLE_TRANSFORMERS.put(transformer.getName(), transformer);
            Log.log(Log.INFO, TAG, "Found transformer " + x.getName() + " (" + transformer.getName() + ")");
        }
    }
    
    // Transformer invocation: transformerName
    // or                      transformerName!arg1/arg2...
    public static InputStreamLength apply(String fullInvocation, String path, Set set, InputStreamLength is) throws IOException {
        // The transformer name is just the first part of the invocation, before the first exclamation mark
        int exclIndex = fullInvocation.indexOf('!');
        String name, arguments = null;
        if(exclIndex == -1) {
            name = fullInvocation;
        } else {
            name = fullInvocation.substring(0, exclIndex);
            arguments = fullInvocation.substring(exclIndex + 1);
        }
        if(!AVAILABLE_TRANSFORMERS.containsKey(name)) throw new RuntimeException(String.format("No such transformer: %s", name));
        return AVAILABLE_TRANSFORMERS.get(name).apply(arguments, path, set, is);
    }
}
