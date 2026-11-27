/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package opensrs.filetransformers;

import java.io.File;
import opensrs.InputStreamLength;

/**
 *
 * @author piotr
 */
public class Bindings {
    public record BindingsOutput(String contentType, InputStreamLength fileContent, String redirect) {}
    
    public BindingsOutput process(String pathRelativeToBindingsRoot) {
        
    }
}
