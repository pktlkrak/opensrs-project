/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package opensrs.filetransformers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import opensrs.InputStreamLength;
import opensrs.Utils;
import opensrs.sets.GlobalStore;
import org.w3c.dom.Element;

/**
 *
 * @author piotr
 */
public class Bindings {

    public static enum BindingType {
        REDIRECT, TRANSFER
    }

    private class Binding {

        private String[] tags = null;
        private BindingType type;
        private String target;

        public Binding(String target, BindingType type) {
            this(target, type, null);
        }

        public Binding(String target, BindingType type, String[] tags) {
            this.target = target;
            this.type = type;
            this.tags = tags;
        }

        public BindingType getType() {
            return type;
        }

        public String getTarget() {
            return target;
        }

        public String[] getTags() {
            return tags;
        }

    }

    private class MimeMapping {

        private String[] tags = null;
        private String mime;
        private Pattern localFileMatch = null;
        private String localFileExtension = null;
        private Pattern urlFileMatch = null;
        private String urlFileExtension = null;

        public MimeMapping(String mime) {
            this.mime = mime;
        }

        public boolean doesMatch(String urlProvidedFile, String fileName) {
            if (urlFileMatch != null) {
                if (!urlFileMatch.matcher(urlProvidedFile).matches()) {
                    return false;
                }
            }
            if (localFileMatch != null) {
                if (!localFileMatch.matcher(fileName).matches()) {
                    return false;
                }
            }
            if (localFileExtension != null) {
                if (!fileName.toLowerCase().endsWith(localFileExtension.toLowerCase())) {
                    return false;
                }
            }
            if (urlFileExtension != null) {
                if (!urlProvidedFile.toLowerCase().endsWith(urlFileExtension.toLowerCase())) {
                    return false;
                }
            }

            // Fallthrough positive
            return true;
        }

        public String[] getTags() {
            return this.tags;
        }

        public String getMime() {
            return this.mime;
        }
    }

    public static class BindingsOutput {
        private String redirectDestination;
        private InputStreamLength fileStream;
        private String mimeType;
        
        private String rootOfFileStream;

        public String getRedirectDestination() {
            return redirectDestination;
        }

        public InputStreamLength getFileStream() {
            return fileStream;
        }

        public String getMimeType() {
            return mimeType;
        }

        private BindingsOutput(String redirectDestination, InputStreamLength fileStream, String mimeType, String rootOfFileStream) {
            this.redirectDestination = redirectDestination;
            this.fileStream = fileStream;
            this.mimeType = mimeType;
            this.rootOfFileStream = rootOfFileStream;
        }
    }

    private opensrs.sets.Set set;

    /**
     * Aliases of a certain set file to a page address.
     */
    private Map<String, Binding> pageBindings = new HashMap<>();

    /**
     * Aliases of a certain set file to a page address with regex (e.g. a
     * certain flashcard).
     */
    private Map<Pattern, Binding> regexPageBindings = new HashMap<>();

    /**
     * Mime type mappings
     *
     * I'm not building a separate list of extension -> mapping, then regex ->
     * mapping Because of the relatively small amount of bindings which will be
     * defined. Iterating over an array of ~4 elements should not cause a
     * meaningful impact on the speed of the entire system.
     */
    private List<MimeMapping> mimeTypesAndMappings = new ArrayList<>();

    /**
     * Transformers' list
     */
    private record FileTransformerRef(String[] tagsRequired, FileTransformer ft) {

    }
    ;
    private List<FileTransformerRef> transformersToApply = new ArrayList<>();
    private String root;
    
    public String getRoot() {
        return root;
    }

    /**
     * Layout:
     * <pre>
     * <bindings>
     *  <routing>
     *   <bind target="my_file.html" source="dashboard" type="regex" tag="dash main html"/>
     *  </routing>
     *  <mappings>
     *   <mime extension=".html">text/html</mime>
     *   <mime tag="html" filematch="[^\.]+\.txt">text/html</mime>
     *   <assign tag="whatever" filematch="..."/>
     *  </mappings>
     *  <transformers>
     *   <layer tag="translation" apply="FileTranslation">
     *    <.../>
     *   </layer>
     *  </transformers>
     * </bindings>
     * </pre>
     *
     * @param set Source of these bindings
     * @param rootBindingsElement
     */
    public Bindings(opensrs.sets.Set set, Element rootBindingsElement) {
        this.set = set;
        if (rootBindingsElement == null) {
            return;
        }

        Element routingElement = (Element) rootBindingsElement.getElementsByTagName("routing").item(0);
        Element mappingsElement = (Element) rootBindingsElement.getElementsByTagName("mappings").item(0);
        Element transformersElement = (Element) rootBindingsElement.getElementsByTagName("transformers").item(0);
        
        Element rootElement = (Element) rootBindingsElement.getElementsByTagName("root").item(0);
        if(rootElement != null) {
            root = rootElement.getTextContent().trim();
        } else {
            root = "index.html";
        }

        if (routingElement != null) {
            Utils.asStream(routingElement.getChildNodes()).forEach(n -> {
                String type = n.getAttribute("type");
                BindingType bindingType = n.getTagName().equals("redirect") ? BindingType.REDIRECT : BindingType.TRANSFER;
                Binding bind = new Binding(n.getAttribute("target"), bindingType);
                if (n.hasAttribute("tag")) {
                    bind.tags = n.getAttribute("tag").split(" ");
                }
                switch (type) {
                    case "":
                    case "literal":
                        pageBindings.put(n.getAttribute("source"), bind);
                        break;
                    case "regex":
                        regexPageBindings.put(Pattern.compile(n.getAttribute("source")), bind);
                        break;
                    default:
                        throw new RuntimeException("Invalid binding type " + type);
                }
            });
        }

        if (mappingsElement != null) {
            Utils.asStream(mappingsElement.getChildNodes()).forEach(n -> {
                // <mime> or <assign tag=...>
                MimeMapping mm = new MimeMapping(n.getTagName().equals("mime") ? n.getTextContent().trim() : null);
                if (n.hasAttribute("tag")) {
                    mm.tags = n.getAttribute("tag").split(" ");
                }
                if (n.hasAttribute("urlmatch")) {
                    mm.urlFileMatch = Pattern.compile(n.getAttribute("urlmatch"));
                }
                if (n.hasAttribute("urlextension")) {
                    mm.urlFileExtension = n.getAttribute("urlextension");
                }
                if (n.hasAttribute("localmatch")) {
                    mm.localFileMatch = Pattern.compile(n.getAttribute("localmatch"));
                }
                if (n.hasAttribute("localextension")) {
                    mm.localFileExtension = n.getAttribute("localextension");
                }
                mimeTypesAndMappings.add(mm);
            });
        }

        if (transformersElement != null) {
            Utils.asStream(transformersElement.getChildNodes()).forEach(n -> {
                String[] tags = n.getAttribute("tag").split(" "); // Allow empty list.
                String nameToApply = n.getAttribute("apply");
                FileTransformer ft = FileTransformers.initializeTransformer(nameToApply, set, n);
                transformersToApply.add(new FileTransformerRef(tags, ft));
            });
        }
    }
    
    private String getMimeTypeFor(String urlProvidedFile, String bindingTarget, Set<String> tags) {
        for (MimeMapping mm : mimeTypesAndMappings) {
            if (mm.doesMatch(urlProvidedFile, bindingTarget)) {
                if (tags != null & mm.getTags() != null) {
                    Collections.addAll(tags, mm.getTags());
                }
                if(mm.getMime() != null)
                    return mm.getMime();
            }
        }
        
        return null;
    }
    
    public String getMimeTypeFor(String urlProvided, BindingsOutput bo) {
        return getMimeTypeFor(urlProvided, bo.rootOfFileStream, null);
    }

    public BindingsOutput process(String pathRelativeToBindingsRoot) throws IOException {
        Binding response = pageBindings.get(pathRelativeToBindingsRoot);

        if (response == null) {
            //Try the regexes
            for (Pattern pattern : regexPageBindings.keySet()) {
                Matcher m = pattern.matcher(pathRelativeToBindingsRoot);
                if (!m.matches()) {
                    continue;
                }
                Binding found = regexPageBindings.get(pattern);
                // Apply regex substitutions:
                String[] newTags;
                if(found.getTags() != null) {
                    newTags = new String[found.getTags().length];
                    for(int i = 0; i < newTags.length; i++) {
                        newTags[i] = m.replaceAll(found.getTags()[i]);
                    }
                } else newTags = new String[0];
                response = new Binding(m.replaceAll(found.getTarget()), found.getType(), newTags);
                break;
            }
        }
        // Still null? Default case.
        if (response == null) {
            response = new Binding(pathRelativeToBindingsRoot, BindingType.TRANSFER);
        }

        Set<String> tags = new HashSet<>();
        if (response.getTags() != null) {
            Collections.addAll(tags, response.getTags());
        }

        // Now apply the mime type mappings
        String mimeType = getMimeTypeFor(pathRelativeToBindingsRoot, response.getTarget(), tags);

        if (response.getType() == BindingType.REDIRECT) {
            // Cannot remap a redirect.
            return new BindingsOutput(response.getTarget(), null, null, null);
        }

        File initialFile = this.set == null ? GlobalStore.getCommonFile(response.getTarget()) : set.getFile(response.getTarget());
        if (initialFile == null) {
            return null;
        }
        InputStreamLength isl = new InputStreamLength(new FileInputStream(initialFile), initialFile.length());

        transformerLoop:
        for (FileTransformerRef ref : transformersToApply) {
            for (String tagRequired : ref.tagsRequired()) {
                if (!tags.contains(tagRequired)) {
                    continue transformerLoop; // Skip it - conditions not met
                }
            }

            isl = ref.ft().apply(pathRelativeToBindingsRoot, initialFile, tags, isl);
        }

        // Apply the layers from tags
        return new BindingsOutput(null, isl, mimeType, response.getTarget());
    }
}
