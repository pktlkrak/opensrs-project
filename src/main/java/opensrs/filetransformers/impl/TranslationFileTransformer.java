/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package opensrs.filetransformers.impl;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import opensrs.InputStreamLength;
import opensrs.filetransformers.FileTransformer;
import opensrs.filetransformers.FileTransformerName;
import opensrs.sets.GlobalStore;
import opensrs.sets.Set;
import org.autumn.Utils;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 *
 * @author piotr
 */
 
@FileTransformerName("Translate")
public class TranslationFileTransformer implements FileTransformer {
 
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{trs(\\.[^:]*)?:([^\\}]*)\\}");
 
    private final Set set;
    private final java.util.Set<String> allowed = new HashSet<>();
 
    public TranslationFileTransformer(Set set, Element decl) {
        this.set = set;
 
        NodeList localeNodes = decl.getElementsByTagName("locale");
        for (int i = 0; i < localeNodes.getLength(); i++) {
            allowed.add(localeNodes.item(i).getTextContent().trim().toLowerCase());
        }
 
        if (allowed.isEmpty()) {
            throw new IllegalArgumentException("Invalid translation transformer - expected <locale>...</locale>");
        }
    }
 
    @Override
    public InputStreamLength apply(String initialPath, File sourceFile, java.util.Set<String> tags, InputStreamLength is) throws IOException {
        File translationFile = null;
        try {
            String locale = null;
            for (String tag : tags) {
                if(tag.startsWith("translate-lang-")) {
                    String destLang = tag.substring(("translate-lang-").length());
                    if(allowed.contains(destLang)) {
                        locale = destLang;
                        break;
                    }
                }
            }
            if (locale == null) {
                return is; // No allowed locale in path, send as-is
            }
            String name = "translations." + locale + ".json";
            translationFile = set == null ? GlobalStore.getCommonFile(name) : set.getFile(name);
        } catch (StringIndexOutOfBoundsException ex) {
            // Can't parse - replace placeholders.
        }
 
        JsonNode node;
        if (translationFile == null) {
            node = opensrs.Utils.MAPPER.createObjectNode();
        } else {
            try (InputStream in = new FileInputStream(translationFile)) {
                node = opensrs.Utils.MAPPER.readTree(in);
            }
        }
        if (!(node instanceof ObjectNode on)) {
            return is; // Fall through to original contents.
        }
 
        String source = new String(is.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        Matcher matcher = PLACEHOLDER.matcher(source);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            String subkey = matcher.group(1);
            String key = matcher.group(2).strip();
 
            Optional<String> resolved = Optional.empty();
            if (subkey != null) {
                resolved = on.optional(subkey.strip() + "." + key).flatMap(e -> e.asStringOpt());
            }
            resolved = resolved.or(() -> on.optional(key).flatMap(e -> e.asStringOpt()));
 
            matcher.appendReplacement(out, Matcher.quoteReplacement(resolved.orElse(key)));
        }
        matcher.appendTail(out);
 
        byte[] bytes = out.toString().getBytes(StandardCharsets.UTF_8);
        return new InputStreamLength(new ByteArrayInputStream(bytes), bytes.length);
    }
}
