package de.tum.in.www1.artemis.service.compass.umlmodel.usecase;

import java.util.List;

import de.tum.in.www1.artemis.service.compass.strategy.NameSimilarity;
import de.tum.in.www1.artemis.service.compass.umlmodel.Similarity;
import de.tum.in.www1.artemis.service.compass.umlmodel.UMLContainerElement;
import de.tum.in.www1.artemis.service.compass.umlmodel.UMLElement;

public class UMLSystemBoundary extends UMLContainerElement {

    public final static String UML_SYSTEM_BOUNDARY_TYPE = "SystemBoundary";

    private String name;

    public UMLSystemBoundary(String name, List<UMLElement> subElements, String jsonElementID) {
        super(jsonElementID, subElements);
        this.name = name;

    }

    @Override
    public double similarity(Similarity<UMLElement> reference) {
        double similarity = 0;

        if (reference instanceof UMLSystemBoundary) {
            UMLSystemBoundary referencePackage = (UMLSystemBoundary) reference;
            similarity += NameSimilarity.levenshteinSimilarity(name, referencePackage.getName());
        }

        return ensureSimilarityRange(similarity);
    }

    @Override
    public String toString() {
        return "System Boundary " + name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getType() {
        return UML_SYSTEM_BOUNDARY_TYPE;
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) {
            return false;
        }

        UMLSystemBoundary systemBoundary = (UMLSystemBoundary) obj;

        return name.equals(systemBoundary.name);
    }
}
