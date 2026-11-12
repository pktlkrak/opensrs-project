/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package opensrs.filetransformers.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import opensrs.InputStreamLength;
import opensrs.filetransformers.FileTransformer;
import opensrs.sets.GlobalStore;
import opensrs.sets.Set;
import org.autumn.Utils;

/**
 *
 * @author asivery
 */
public class EmbeddingFileTransformer implements FileTransformer {

    @Override
    public String getName() {
        return "embedText";
    }

    @Override
    public InputStreamLength apply(String args, String path, Set set, InputStreamLength is) throws IOException {
        ByteArrayOutputStream rootBaos = new ByteArrayOutputStream();
        Utils.copyStream(is.getInputStream(), rootBaos, 4096);
        String asString = rootBaos.toString("utf-8");
        Pattern p = Pattern.compile("\\{embed:[\\s]*(.*)\\}");
        Matcher stringMatcher = p.matcher(asString);
        StringBuilder out = new StringBuilder();
        while (stringMatcher.find()) {
            String fileName = stringMatcher.group(1).strip();
            InputStreamLength fullFile;
            if (set != null) {
                fullFile = set.getFile(fileName);
            } else {
                fullFile = GlobalStore.getCommonFile(fileName);
            }

            ByteArrayOutputStream subBaos = new ByteArrayOutputStream();
            Utils.copyStream(fullFile.getInputStream(), subBaos, 4096);

            stringMatcher.appendReplacement(out, subBaos.toString("UTF-8"));
        }

        stringMatcher.appendTail(out);

        byte[] bytes = out.toString().getBytes("UTF-8");
        return new InputStreamLength(new ByteArrayInputStream(bytes), bytes.length);
    }
}
