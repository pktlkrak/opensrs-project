/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package opensrs.filetransformers;

import java.io.IOException;
import opensrs.InputStreamLength;
import opensrs.sets.Set;

/**
 *
 * @author asivery
 */
public interface FileTransformer {
    public String getName();
    public InputStreamLength apply(String arguments, String path, Set set, InputStreamLength is) throws IOException;
}
