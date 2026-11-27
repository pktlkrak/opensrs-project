/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package opensrs.filetransformers.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import opensrs.InputStreamLength;
import opensrs.filetransformers.FileTransformer;
import opensrs.sets.GlobalStore;
import opensrs.sets.Set;
import org.autumn.Utils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 *
 * @author piotr
 */
public class TranslationFileTransformer implements FileTransformer {

    @Override
    public String getName() {
        return "translate";
    }

    @Override
    public InputStreamLength apply(String arguments, String path, Set set, InputStreamLength is) throws IOException {
        String args[] = arguments.split("!");
        InputStreamLength translationFileInput = null;
        if (args.length != 2) {
            throw new IOException("Invalid translation transformer argumetns - expected 'format (w/ locale)!allow1,allow2,...'");
        }

        try {
            String format = args[0].trim();
            String allowed[] = args[1].trim().toLowerCase().split(",");

            String locale = null;
            for(String validFormat : format.split(Pattern.quote("|"))) {
                Map<String, String> formatMatch = Utils.parse(validFormat, path);
                if (formatMatch.containsKey("locale")) {
                    locale = formatMatch.get("locale").toLowerCase();
                    break;
                }
            }
            if(locale == null) return is;
            boolean found = false;

            for (String entry : allowed) {
                if (locale.equals(entry)) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                return is; // If no locale found, send as-is
            }
            translationFileInput = set == null
                    ? GlobalStore.getCommonFile("translations." + locale + ".json")
                    : set.getFile("translations." + locale + ".json");
        }catch(StringIndexOutOfBoundsException ex) {
            
        }

        JsonNode node = translationFileInput == null ?
                opensrs.Utils.MAPPER.createObjectNode() :
                opensrs.Utils.MAPPER.readTree(translationFileInput.getInputStream());

        if (node instanceof ObjectNode on) {
            ByteArrayOutputStream rootBaos = new ByteArrayOutputStream();
            Utils.copyStream(is.getInputStream(), rootBaos, 4096);
            String asString = rootBaos.toString("utf-8");
            Pattern p = Pattern.compile("\\{trs(\\.[^:]*)?:([^\\}]*)\\}");
            Matcher stringMatcher = p.matcher(asString);
            StringBuilder out = new StringBuilder();
            while (stringMatcher.find()) {
                String subkey = stringMatcher.group(1);
                String key = stringMatcher.group(2).strip();
                
                Optional<String> resolved = Optional.empty();
                if(subkey != null) {
                    resolved = on.optional(subkey.strip() + "." + key).flatMap(e -> e.asStringOpt());
                }
                
                resolved = resolved.or(() -> on.optional(key).flatMap(e -> e.asStringOpt()));

                stringMatcher.appendReplacement(out, resolved.orElse(key));
            }

            stringMatcher.appendTail(out);

            byte[] bytes = out.toString().getBytes("UTF-8");
            return new InputStreamLength(new ByteArrayInputStream(bytes), bytes.length);
        } else {
            // Fall through to original contents.
            return is;
        }
    }
}
