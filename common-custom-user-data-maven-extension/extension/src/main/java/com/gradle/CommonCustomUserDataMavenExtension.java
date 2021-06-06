package com.gradle;

import com.gradle.maven.extension.api.cache.BuildCacheApi;
import com.gradle.maven.extension.api.scan.BuildScanApi;
import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.logging.Logger;

import javax.inject.Inject;

@Component(
    role = AbstractMavenLifecycleParticipant.class,
    hint = "common-custom-user-data",
    description = "Captures common custom user data in Maven build scans"
)
public final class CommonCustomUserDataMavenExtension extends AbstractMavenLifecycleParticipant {

    private final PlexusContainer container;
    private final Logger logger;

    @Inject
    public CommonCustomUserDataMavenExtension(PlexusContainer container, Logger logger) {
        this.container = container;
        this.logger = logger;
    }

    @Override
    public void afterProjectsRead(MavenSession session) throws MavenExecutionException {
        logger.debug("Executing extension: " + getClass().getSimpleName());

        BuildScanApi buildScan = ApiAccessor.lookupBuildScanApi(container, getClass());
        if (buildScan != null) {
            logger.debug("Configuring build scan publishing");
            CustomGradleEnterpriseConfig.configureBuildScanPublishing(buildScan);
            logger.debug("Finished configuring build scan publishing");

            logger.debug("Applying build scan enhancements");
            CustomBuildScanEnhancements.configureBuildScan(buildScan, session);
            logger.debug("Finished applying build scan enhancements");
        }

        BuildCacheApi buildCache = ApiAccessor.lookupBuildCacheApi(container, getClass());
        if (buildCache != null) {
            logger.debug("Configuring build cache");
            CustomGradleEnterpriseConfig.configureBuildCache(buildCache);
            logger.debug("Finished configuring build cache");
        }

        if (buildScan != null || buildCache != null) {
            GroovyScriptUserData.addToApis(session, buildScan, buildCache, logger);
        } else {
            logger.debug("Skipping evaluation of custom user data Groovy script because BuildScanApi and BuildCacheApi are both not available");
        }
    }

}
