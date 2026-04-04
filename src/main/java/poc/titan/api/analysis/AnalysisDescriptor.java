package poc.titan.api.analysis;

import java.util.Objects;

/*
 * Represents a single analysis discovered from a TITAN plugin JAR.
 */
public class AnalysisDescriptor {
    public final String name; // "Free Choice", "Marked Graphs", etc.
    public final String className; // "org.pnpl.analysis.freechoice.analysis.AnalysisPNPL"
    public final String type; // "pnpl" or "products"


    public AnalysisDescriptor(String name, String className, String type) {
        this.name = name;
        this.className = className;
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AnalysisDescriptor other)) return false;
        return name.equals(other.name) && type.equals(other.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type);
    }

    @Override
    public String toString() {
        return name + " [" + type + "]";
    }
}