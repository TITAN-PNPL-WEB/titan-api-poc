package pocfreechoice.analysis;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

@Component
public class PluginScanner {

    private static final String PLUGINS_DIR = "plugins";
    private static final String EXT_PNPL = "org.pnpl.analysis.analyzer.pnpl";
    private static final String EXT_PRODUCTS = "org.pnpl.analysis.analyzer.products";

    private final List<AnalysisDescriptor> analyses = new ArrayList<>();

    /**
     * This method scans plugins folder, reads plugins and registers analysis
     *
     * @throws Exception
     */
    @PostConstruct //annotation: Spring will execute this automatically after create and inject the bean
    public void scan() throws Exception {
        Path pluginsPath = Paths.get(PLUGINS_DIR);
        if (!Files.exists(pluginsPath)) {
            System.out.println("[scanner] plugins/ directory not found, skipping.");
            return;
        }

        List<Path> jars = Files.list(pluginsPath)
                .filter(p -> p.toString().endsWith(".jar"))
                .toList();

        for (Path jar : jars) {
            scanJar(jar);
        }

        System.out.println("[scanner] Found analyses: " + analyses);
    }

    private void scanJar(Path jarPath) throws Exception {
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            JarEntry entry = jar.getJarEntry("plugin.xml");
            if (entry == null) return;

            InputStream is = jar.getInputStream(entry);
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = builder.parse(is);

            NodeList extensions = doc.getElementsByTagName("extension");
            for (int i = 0; i < extensions.getLength(); i++) {
                Element ext = (Element) extensions.item(i);
                String point = ext.getAttribute("point");

                if (!point.equals(EXT_PNPL) && !point.equals(EXT_PRODUCTS)) continue;

                NodeList clients = ext.getElementsByTagName("client");
                for (int j = 0; j < clients.getLength(); j++) {
                    Element client = (Element) clients.item(j);
                    String name = client.getAttribute("name");
                    String clazz = client.getAttribute("class");
                    String type = point.equals(EXT_PNPL) ? "pnpl" : "products";

                    AnalysisDescriptor descriptor = new AnalysisDescriptor(name, clazz, type);
                    if (!analyses.contains(descriptor)) {
                        analyses.add(descriptor);
                    }
                }
            }
        }
    }

    public List<AnalysisDescriptor> getAnalyses() {
        return Collections.unmodifiableList(analyses);
    }
}