package com.gradle;

import org.apache.maven.MavenExecutionException;
import org.apache.maven.eventspy.AbstractEventSpy;
import org.apache.maven.eventspy.EventSpy;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.logging.Logger;

import javax.inject.Inject;

@Component(
        role = EventSpy.class,
        hint = "common-custom-user-data",
        description = "Install the Common Custom User Data Maven extension as a GradleEnterpriseListener"
)
public final class CommonCustomUserDataMavenExtensionEventSpy extends AbstractEventSpy {

    private final PlexusContainer container;
    private final Logger logger;

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    public CommonCustomUserDataMavenExtensionEventSpy(PlexusContainer container, Logger logger) {
        this.container = container;
        this.logger = logger;
    }

    @Override
    public void init(Context context) {
        try {
            if (ApiAccessor.ensureGradleEnterpriseListenerIsAccessible(container, getClass())) {
                logger.info("Installing Common Custom User Data Maven extension");
                ApiAccessor.registerExtension(getClass(), logger);
            }
        } catch (Exception e) {
            logger.error("Common Custom User Data Maven extension could not register itself with the Gradle Enterprise Maven extension.");
            logger.error("Did you apply the Gradle Enterprise Maven extension?");
        }
    }

}
