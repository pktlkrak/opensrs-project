/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package opensrs.sets;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import opensrs.InputStreamLength;
import opensrs.Log;
import opensrs.Utils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import org.xml.sax.InputSource;
import java.nio.charset.StandardCharsets;

/**
 *
 * @author piotr
 */
public abstract class Set {

    private static final String TAG = "Set";

    private Set() {
    }

    private Map<Integer, SRSObject> srsObjects = new HashMap<>();
    public SRSConfig config;

    public static Set getSetFromFilesystem(File root) {
        try {
            final File fileRoot = new File(root, "files");
            Set set = new Set() {
                @Override
                public InputStreamLength getInternalFile(String name) {
                    File f = new File(fileRoot, name);
                    if (!f.isFile()) {
                        return null;
                    }
                    try {
                        return new InputStreamLength(new FileInputStream(f), f.length());
                    } catch (IOException ex) {
                        return null;
                    }
                }
            };
            //Read config.XML and content.XML from the root
            
            Document configDocument = Utils.documentBuilder.parse(new File(root, "config.XML"));
            Document contentDocument = Utils.documentBuilder.parse(new File(root, "content.XML"));
            set.parseContent(contentDocument);
            set.parseConfig(configDocument);

            return set;
        } catch (SAXException | IOException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    private void parseContent(Document document) {
        Element srsDataRoot = (Element) document.getElementsByTagName("SRSData").item(0);

        Utils.asStream(srsDataRoot.getElementsByTagName("SRSObject")).forEach((srsObjectRepresentation) -> {
            int id = Integer.valueOf(srsObjectRepresentation.getAttribute("id"));
            try {
                String type = Utils.getTextOfElement(srsObjectRepresentation, "family");
                String query = Utils.getTextOfElement(srsObjectRepresentation, "query");
                int level = Integer.valueOf(Utils.getTextOfElement(srsObjectRepresentation, "level"));
                String data = Utils.getTextOfElement(srsObjectRepresentation, "data");
                SRSObject srsObject = new SRSObject(id, type, level, query, (ObjectNode) Utils.MAPPER.readTree(data));
                Element srsReviewable = (Element) srsObjectRepresentation.getElementsByTagName("reviewable").item(0);
                Element srsDependencies = (Element) srsObjectRepresentation.getElementsByTagName("dependencies").item(0);
                Utils.asStream(srsDependencies.getChildNodes()).map(Element::getTextContent).map(Integer::valueOf).forEach(srsObject.getDependencies()::add);
                Utils.asStream(srsReviewable.getChildNodes()).forEach(e -> {
                    if (e.getTagName().equals("coerce")) {
                        srsObject.getCoerce().put(e.getAttribute("for"), e.getTextContent());
                    } else {
                        String name = e.getAttribute("name");
                        if (!srsObject.getReviewable().containsKey(name)) {
                            srsObject.getReviewable().put(name, new LinkedList<>());
                        }
                        srsObject.getReviewable().get(e.getAttribute("name")).add(e.getTextContent());
                    }
                });
                srsObjects.put(srsObject.getId(), srsObject);
            } catch (IOException ex) {
                Log.log(Log.WARNING, TAG, "Couldn't parse object with id: " + id);
                Log.handleException(TAG, ex);
            }
        });
    }

    private void parseConfig(Document document) {
        Element srsConfigRoot = (Element) document.getElementsByTagName("SRSConfig").item(0);

        Element phases = (Element) srsConfigRoot.getElementsByTagName("phases").item(0);
        Element penalties = (Element) srsConfigRoot.getElementsByTagName("penalties").item(0);
        Element timings = (Element) srsConfigRoot.getElementsByTagName("timings").item(0);
        Element bindings = (Element) srsConfigRoot.getElementsByTagName("bindings").item(0);

        //Parse the phases
        config = new SRSConfig();
        Utils.asStream(phases.getChildNodes()).forEach(n -> {
            int number = Integer.parseInt(n.getAttribute("number"));
            int fromStage = Integer.parseInt(n.getAttribute("fromStage"));
            String name = n.getTextContent();
            config.phases.put(fromStage, number);
            config.phaseNames.put(number, name);
        });
        Utils.asStream(penalties.getChildNodes()).forEach(n -> config.penalties.put(Integer.valueOf(n.getAttribute("number")), Integer.valueOf(n.getTextContent())));
        Utils.asStream(timings.getChildNodes()).forEach(n -> config.timings.put(Integer.valueOf(n.getAttribute("number")), Integer.valueOf(n.getTextContent())));
        Utils.asStream(bindings.getChildNodes()).forEach(n -> {
            String type = n.getAttribute("type");
            switch(type){
                case "":
                case "literal":
                    config.pageBindings.put(n.getAttribute("source"), n.getAttribute("target"));
                    break;
                case "regex":
                    config.regexPageBindings.put(Pattern.compile(n.getAttribute("source")), n.getAttribute("target"));
                    break;
            }
        });
    }

    public Stream<SRSObject> stream() {
        return srsObjects.values().parallelStream();
    }

    public SRSObject get(int id) {
        return srsObjects.get(id);
    }

    protected abstract InputStreamLength getInternalFile(String name);

    public InputStreamLength getFile(String name) {
        if (name.startsWith("/")) {
            name = name.substring(1);
        }
        InputStreamLength thisSetss = getInternalFile(name);
        if (thisSetss == null) {
            return GlobalStore.getCommonFile(name);
        }
        return thisSetss;
    }

    public class SRSConfig {

        private Map<Integer, Integer> penalties = new HashMap<>();
        private Map<Integer, Integer> timings = new HashMap<>();
        private Map<Integer, Integer> phases = new HashMap<>();
        private Map<Integer, String> phaseNames = new HashMap<>();
        private Map<String, String> pageBindings = new HashMap<>();
        private Map<Pattern, String> regexPageBindings = new HashMap<>();

        private int getValueOfHighestToKey(Map<Integer, Integer> map, int key, int defaultV) {
            return map
                    .keySet()
                    .stream()
                    .sorted((a, b) -> b - a)
                    .filter(a -> key >= a)
                    .findFirst()
                    .map(n -> map.get(n))
                    .orElse(defaultV);
        }

        public int getPenalty(int currentStage) {
            return getValueOfHighestToKey(penalties, currentStage, 1);
        }

        public int getDelayInHours(int currentStage) {
            return timings.getOrDefault(currentStage, -1);
        }

        public String getNameOfPhase(int phase) {
            return phaseNames.get(phase);
        }

        public int getPhaseForStage(int currentStage) {
            return getValueOfHighestToKey(phases, currentStage, 1);
        }

        public String getBinding(String source) {
            String response = pageBindings.get(source);
            if(response == null){
                //Try the regexes
                for(Pattern pattern : regexPageBindings.keySet()){
                    Matcher m = pattern.matcher(source);
                    if(!m.matches()) continue;
                    response = m.replaceAll(regexPageBindings.get(pattern));
                }
                if(response == null) return source;
            }
            return response;
        }
    }
}
