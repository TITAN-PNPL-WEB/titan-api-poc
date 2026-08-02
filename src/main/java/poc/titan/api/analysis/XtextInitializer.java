package poc.titan.api.analysis;

import com.google.inject.Injector;
import jakarta.annotation.PostConstruct;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.xtext.resource.IResourceServiceProvider;
import org.pnpl.model.variability.editor.PNPL_variabilityStandaloneSetup;
import org.pnpl.model.variability.editor.providers.PetriNetsResourceServiceProvider;
import org.springframework.stereotype.Component;
import PetriNets.PetriNetsPackage;
import variability.VariabilityPackage;

/**
 * Initializes the Xtext language infrastructure in standalone mode (no Eclipse).
 * Runs once at Spring Boot startup.
 */
@Component
public class XtextInitializer {

    private Injector injector;

    @PostConstruct
    public void init() {
        // Force registration of EMF packages
        PetriNetsPackage.eINSTANCE.eClass();
        VariabilityPackage.eINSTANCE.eClass();

        // Register the Xtext language and obtain the Guice injector
        injector = new PNPL_variabilityStandaloneSetup().createInjectorAndDoEMFRegistration();

        // Register the resource service provider for .petrinets files
        // so Xtext can resolve cross-references to places/transitions/arcs
        IResourceServiceProvider.Registry.INSTANCE.getExtensionToFactoryMap()
                .put("petrinets", new PetriNetsResourceServiceProvider());

        // Register XMI factory for .petrinets files
        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap()
                .put("petrinets", new XMIResourceFactoryImpl());

        System.out.println("[xtext] Xtext standalone initialized for .vrb validation");
    }

    public Injector getInjector() {
        return injector;
    }
}