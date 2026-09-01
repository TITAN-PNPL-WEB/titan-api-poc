package poc.titan.api.analysis;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;

/**
 * Discovers available analyses by scanning the JARs in the plugins folder.
 */
@Component
public class PluginScanner {

    private static final String PLUGINS_DIR = "plugins";
    private static final String EXT_PNPL     = "org.pnpl.analysis.analyzer.pnpl";
    private static final String EXT_PRODUCTS = "org.pnpl.analysis.analyzer.products";

    private final List<AnalysisDescriptor> analyses = new ArrayList<>();
    private URLClassLoader pluginClassLoader;

    /**
     * Scans every JAR in plugins/ for analyses and builds the shared class loader.
     * Runs once at startup.
     */
    @PostConstruct
    public void scan() throws Exception {
        Path pluginsPath = Paths.get(PLUGINS_DIR);
        if (!Files.exists(pluginsPath)) {
            System.out.println("[scanner] plugins/ directory not found, skipping.");
            return;
        }

        List<URL> jarUrls = new ArrayList<>();
        List<Path> jars = Files.list(pluginsPath)
                .filter(p -> p.toString().endsWith(".jar"))
                .toList();

        for (Path jar : jars) {
            jarUrls.add(jar.toUri().toURL());
            scanJar(jar);
        }

        pluginClassLoader = new URLClassLoader(
                jarUrls.toArray(new URL[0]),
                Thread.currentThread().getContextClassLoader()
        );

        System.out.println("[scanner] Found analyses: " + analyses);
    }

    /**
     * Reads a single JAR's plugin.xml and registers any analyses it declares.
     */
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
                    String name  = client.getAttribute("name");
                    String clazz = client.getAttribute("class");
                    String type  = point.equals(EXT_PNPL) ? "pnpl" : "products";

                    AnalysisDescriptor descriptor = new AnalysisDescriptor(name, clazz, type);
                    if (!analyses.contains(descriptor)) {
                        analyses.add(descriptor);
                    }
                }
            }
        }
    }

    /**
     * Returns the descriptor matching the given name and type, or empty if not found.
     */
    public Optional<AnalysisDescriptor> find(String name, String type) {
        return analyses.stream()
                .filter(d -> d.name.equals(name) && d.type.equals(type))
                .findFirst();
    }

    /**
     * Returns all discovered analyses.
     */
    public List<AnalysisDescriptor> getAnalyses() {
        return Collections.unmodifiableList(analyses);
    }

    /**
     * Returns the class loader used to load analysis classes from the plugin JARs.
     */
    public URLClassLoader getPluginClassLoader() {
        return pluginClassLoader;
    }
}