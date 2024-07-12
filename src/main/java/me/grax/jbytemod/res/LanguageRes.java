package me.grax.jbytemod.res;

import de.xbrowniecodez.jbytemod.Main;
import me.grax.jbytemod.utils.ErrorDisplay;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.*;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class LanguageRes {
    private final HashMap<String, String> map = new HashMap<>();
    private final HashMap<String, String> defaultMap = new HashMap<>();

    public LanguageRes() {
        Main.INSTANCE.getLogger().log("Reading Language XML..");
        this.readXML(map, getXML());
        this.readXML(defaultMap, LanguageRes.class.getResourceAsStream("/locale/en.xml"));
        Main.INSTANCE.getLogger().log("Successfully loaded " + map.size() + " local resources and " + defaultMap.size() + " default resources");
    }


    private Font fixFont(Font font) {
        return new Font(null, font.getStyle(), font.getSize());
    }

    public String getResource(String desc) {
        return map.getOrDefault(desc, defaultMap.getOrDefault(desc, desc));
    }

    private void readXML(Map<String, String> m, InputStream is) {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(is);
            doc.getDocumentElement().normalize();
            Element resources = doc.getDocumentElement();
            NodeList nodes = resources.getChildNodes();
            for (int i = 0; i < nodes.getLength(); i++) {
                Node e = (Node) nodes.item(i);
                if (e.getNodeName().equals("string")) {
                    Element el = (Element) e;
                    m.put(el.getAttribute("name"), e.getTextContent());
                }
            }
        } catch (Exception e) {
             Main.INSTANCE.getLogger().err("Failed to load resources: " + e.getMessage());
            e.printStackTrace();
            new ErrorDisplay(e);
        }
    }

    private InputStream getXML() {
        return LanguageRes.class.getResourceAsStream("/locale/en.xml");

    /*
    InputStream is = LanguageRes.class.getResourceAsStream("/locale/" + this.getLanguage() + ".xml");
    if (is == null) {
       Main.INSTANCE.getLogger().warn("Locale not found, using default en.xml");
      is = LanguageRes.class.getResourceAsStream("/locale/en.xml");
      if (is == null) {
         Main.INSTANCE.getLogger().err("en.xml not found!");
      }
    }
    return is;
     */
    }

    private String getLanguage() {
        return System.getProperty("user.language").toLowerCase().replace('_', '-');
    }
}
