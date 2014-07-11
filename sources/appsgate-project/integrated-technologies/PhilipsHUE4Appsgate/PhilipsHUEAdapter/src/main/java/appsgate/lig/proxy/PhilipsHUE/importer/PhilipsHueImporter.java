package appsgate.lig.proxy.PhilipsHUE.importer;

import com.philips.lighting.hue.sdk.bridge.impl.PHBridgeImpl;
import com.philips.lighting.model.PHLight;
import org.apache.felix.ipojo.annotations.*;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.ow2.chameleon.fuchsia.core.FuchsiaUtils;
import org.ow2.chameleon.fuchsia.core.component.AbstractImporterComponent;
import org.ow2.chameleon.fuchsia.core.component.ImporterIntrospection;
import org.ow2.chameleon.fuchsia.core.component.ImporterService;
import org.ow2.chameleon.fuchsia.core.declaration.ImportDeclaration;
import org.ow2.chameleon.fuchsia.core.exceptions.BinderException;
import org.ow2.chameleon.fuchsia.importer.philipshue.util.PhilipsHueImportDeclarationWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.apache.felix.ipojo.Factory.INSTANCE_NAME_PROPERTY;

@Component
@Provides(specifications = {ImporterService.class, ImporterIntrospection.class})
public class PhilipsHueImporter extends AbstractImporterComponent {

    private static final Logger LOG = LoggerFactory.getLogger(PhilipsHueImporter.class);

    private final BundleContext context;

    private ServiceReference serviceReference;

    private Map<String,ServiceRegistration> lamps=new HashMap<String, ServiceRegistration>();

    @ServiceProperty(name = "target", value = "(discovery.philips.device.name=*)")
    private String filter;

    @ServiceProperty(name = INSTANCE_NAME_PROPERTY)
    private String name;

    public PhilipsHueImporter(BundleContext context) {
        this.context = context;
    }

    @PostRegistration
    public void registration(ServiceReference serviceReference) {
        super.setServiceReference(serviceReference);
    }

    @Validate
    public void validate() {
        LOG.info("Appsgate philips hue importer is up and running");
    }

    @Invalidate
    public void invalidate() {

        LOG.info("Cleaning up instances into Appsgate philips hue importer ");

        cleanup();

    }

    private void cleanup(){

        for(Map.Entry<String,ServiceRegistration> lampEntry:lamps.entrySet()){
            lamps.remove(lampEntry.getKey()).unregister();
        }

        for(ImportDeclaration id:super.getImportDeclarations()){
            super.unhandleImportDeclaration(id);
        }

    }

    @Requires(specification = PHBridgeImpl.class,optional = true)
    List<PHBridgeImpl> bridges;

    @Override
    protected void useImportDeclaration(final ImportDeclaration importDeclaration) throws BinderException {

        LOG.info("Appsgate philips hue importer was invoked");

        PhilipsHueImportDeclarationWrapper pojo= PhilipsHueImportDeclarationWrapper.create(importDeclaration);

        try {

            FuchsiaUtils.loadClass(context, pojo.getType());

            Dictionary<String, Object> props = new Hashtable<String, Object>();

            ServiceRegistration lampService=context.registerService(pojo.getType(),pojo.getObject(),props);

            lamps.put(pojo.getId(),lampService);

            //This is done in a different thread duo to an Apam blockage
            new Thread(new PhilipsHueFactoryExecutor(bridges.get(0), (PHLight) pojo.getObject(),this,importDeclaration)).start();


        } catch (ClassNotFoundException e) {
            LOG.error("Failed to load type {}, importing process aborted.", pojo.getType(), e);
        }

    }





    @Override
    protected void denyImportDeclaration(final ImportDeclaration importDeclaration) throws BinderException {

        PhilipsHueImportDeclarationWrapper pojo= PhilipsHueImportDeclarationWrapper.create(importDeclaration);

        try {
            lamps.remove(pojo.getId()).unregister();
        }catch(IllegalStateException e){
            LOG.error("failed unregistering lamp", e);
        }

        unhandleImportDeclaration(importDeclaration);

    }


    public String getName() {
        return name;
    }
}