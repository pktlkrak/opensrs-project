/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package opensrs.filetransformers;

import java.io.File;
import java.io.IOException;
import opensrs.InputStreamLength;
import opensrs.sets.Set;

/**
 *
 * @author asivery
 */
public interface FileTransformer {
    // Constructor:
    // public FileTransformer(@Nullable Set set, Element decl);
    public InputStreamLength apply(String initialPath, File sourceFile, java.util.Set<String> tags, InputStreamLength is) throws IOException;
}
