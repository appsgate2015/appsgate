package appsgate.ard.protocol.fuchsia;

import appsgate.ard.protocol.controller.ARDController;
import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.impl.ComponentBrokerImpl;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.annotations.*;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.ow2.chameleon.fuchsia.core.component.AbstractImporterComponent;
import org.ow2.chameleon.fuchsia.core.declaration.ImportDeclaration;
import org.ow2.chameleon.fuchsia.core.exceptions.BinderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Component
@Provides
public class ARDBrigdeImporter extends AbstractImporterComponent {

    private Logger logger = LoggerFactory.getLogger(ARDBrigdeImporter.class);

    @ServiceProperty(name = "target")
    private String filter;

    @ServiceProperty(name = Factory.INSTANCE_NAME_PROPERTY)
    private String name;

    private BundleContext context;

    private final Executor executor= Executors.newCachedThreadPool();

    final Map<String,ARDController> mapIpControllers =new HashMap<String,ARDController>();

    final Map<ARDController,Set<String>> mapDeclarationIdController =new HashMap<ARDController,Set<String>>();

    public ARDBrigdeImporter(BundleContext context){
        this.context=context;
    }

    @PostRegistration
    public void registration(ServiceReference serviceReference) {
        setServiceReference(serviceReference);
    }

    @Override
    protected void useImportDeclaration(ImportDeclaration importDeclaration) throws BinderException {

        executor.execute(new ARDSideTask(this, mapIpControllers, mapDeclarationIdController,importDeclaration));

        super.handleImportDeclaration(importDeclaration);
    }

    @Override
    protected void denyImportDeclaration(final ImportDeclaration importDeclaration) throws BinderException {

        executor.execute(new Runnable() {
            @Override
            public void run() {
                String declarationId=importDeclaration.getMetadata().get("id").toString();
                String appsgateInstanceName = declarationId;
                logger.info("ARD Door device removed, removing instance {} ..", appsgateInstanceName);
                ((ComponentBrokerImpl) CST.componentBroker).disappearedComponent(appsgateInstanceName);
                logger.info("Appsgate instance {} removed", appsgateInstanceName);
                removeFromSet(declarationId);
                cleanUpNotUsedControllers();
            }
        });

        super.unhandleImportDeclaration(importDeclaration);
    }

    public void removeFromSet(String idDeclaration){
        for(Map.Entry<ARDController,Set<String>> entry:mapDeclarationIdController.entrySet()){
            for(String v:entry.getValue()){
                if(v.equals(idDeclaration)){
                    entry.getValue().remove(v);
                }
            }
        }
    }

    public void cleanUpNotUsedControllers(){

        for(Map.Entry<String,ARDController> entry:mapIpControllers.entrySet()){
            if(mapDeclarationIdController.get(entry.getValue()).size()==0){
                mapDeclarationIdController.remove(entry.getValue());
                mapIpControllers.remove(entry.getKey());
                try {
                    entry.getValue().invalidate();
                    logger.info("Stop monitoring ARD Controller {}",entry.getKey());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    @Override
    public String getName() {
        return name;
    }
}
