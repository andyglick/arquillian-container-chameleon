package org.arquillian.container.chameleon;

import java.util.HashMap;
import java.util.Map;

import org.arquillian.container.chameleon.controller.DistributionController;
import org.arquillian.container.chameleon.controller.TargetController;
import org.arquillian.container.chameleon.spi.model.ContainerAdapter;
import org.jboss.arquillian.config.descriptor.api.ContainerDef;
import org.jboss.arquillian.container.spi.client.container.ContainerConfiguration;
import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.spi.client.container.LifecycleException;
import org.jboss.arquillian.container.spi.client.protocol.ProtocolDescription;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.arquillian.core.api.Injector;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.threading.ExecutorService;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.descriptor.api.Descriptor;

public class ChameleonContainer implements DeployableContainer<ContainerConfiguration> {

    private TargetController target;
    private DistributionController distribution;
    private ChameleonConfiguration configuration;
    private Map<String, String> originalContainerConfiguration;
    private Map<String, String> currentContainerConfiguration;

    @Inject
    private Instance<Injector> injectorInst;

    @Inject
    private Instance<ExecutorService> executorServiceInst;

    @Override
    public Class<ContainerConfiguration> getConfigurationClass() {
        return target.getConfigurationClass();
    }

    @Override
    public ProtocolDescription getDefaultProtocol() {
        return new ProtocolDescription("Servlet 3.0");
    }

    public boolean isInitiated() {
        return this.configuration != null;
    }

    Map<String, String> getOriginalContainerConfiguration() {
        return originalContainerConfiguration;
    }

    Map<String, String> getCurrentContainerConfiguration() {
        return currentContainerConfiguration;
    }

    public void init(ChameleonConfiguration configuration, ContainerDef targetConfiguration) {
        this.configuration = configuration;
        if(this.originalContainerConfiguration == null) {
            this.originalContainerConfiguration = new HashMap<String, String>(targetConfiguration.getContainerProperties());
        }
        try {
            ContainerAdapter adapter = configuration.getConfiguredAdapter();
            this.target = new TargetController(
                    adapter,
                    injectorInst.get(),
                    configuration.getChameleonResolveCacheFolder());
            this.distribution = new DistributionController(
                    adapter,
                    configuration.getChameleonDistributionDownloadFolder());

            distribution.setup(targetConfiguration, executorServiceInst.get());
        } catch (Exception e) {
            throw new IllegalStateException("Could not setup chameleon container", e);
        }
        this.currentContainerConfiguration = targetConfiguration.getContainerProperties();
    }

    public Class<?> resolveTargetClass(String className) throws ClassNotFoundException {
        if(isInitiated()) {
            return this.target.getClassLoader().loadClass(className);
        }
        throw new RuntimeException("Chameleon container is not yet initialized. No Classloader to load from");
    }

    @Override
    public void setup(final ContainerConfiguration targetConfiguration) {
        try {
            target.setup(targetConfiguration);
        } catch (Exception e) {
            throw new RuntimeException("Could not setup Chameleon container for " + configuration.getChameleonTarget(), e);
        }
    }

    @Override
    public void start() throws LifecycleException {
        target.start();
    }

    @Override
    public void stop() throws LifecycleException {
        target.stop();
    }

    @Override
    public ProtocolMetaData deploy(final Archive<?> archive) throws DeploymentException {
        return target.deploy(archive);
    }

    @Override
    public void undeploy(final Archive<?> archive) throws DeploymentException {
        target.undeploy(archive);
    }

    @Override
    public void deploy(final Descriptor descriptor) throws DeploymentException {
        target.deploy(descriptor);
    }

    @Override
    public void undeploy(final Descriptor descriptor) throws DeploymentException {
        target.undeploy(descriptor);
    }
}
