package com.gradle;

import com.gradle.maven.extension.api.GradleEnterpriseApi;
import com.gradle.maven.extension.api.GradleEnterpriseListener;
import com.gradle.maven.extension.api.cache.BuildCacheApi;
import com.gradle.maven.extension.api.scan.BuildScanApi;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.logging.Logger;

import javax.annotation.Nullable;
import javax.inject.Inject;

@Component(
        role = GradleEnterpriseListener.class,
        hint = "common-custom-user-data",
        description = "Captures common custom user data in Maven build scans"
)
public final class CommonCustomUserDataGradleEnterpriseListener implements GradleEnterpriseListener {

    private final Logger logger;

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    public CommonCustomUserDataGradleEnterpriseListener(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void configure(GradleEnterpriseApi api, @Nullable MavenSession session) {
        try {
            logger.debug("Executing extension: " + getClass().getSimpleName());
            CustomGradleEnterpriseConfig customGradleEnterpriseConfig = new CustomGradleEnterpriseConfig();

            logger.debug("Configuring Gradle Enterprise");
            customGradleEnterpriseConfig.configureGradleEnterprise(api);
            logger.debug("Finished configuring Gradle Enterprise");

            logger.debug("Configuring build scan publishing and applying build scan enhancements");
            BuildScanApi buildScan = api.getBuildScan();
            customGradleEnterpriseConfig.configureBuildScanPublishing(buildScan);
            new CustomBuildScanEnhancements(buildScan, session).apply();
            logger.debug("Finished configuring build scan publishing and applying build scan enhancements");

            logger.debug("Configuring build cache");
            BuildCacheApi buildCache = api.getBuildCache();
            customGradleEnterpriseConfig.configureBuildCache(buildCache);
            logger.debug("Finished configuring build cache");

            GroovyScriptUserData.evaluate(session, api, logger);
            SystemPropertyOverrides.configureBuildCache(buildCache);
        } catch (MavenExecutionException e) {
            logger.error("Error executing Common Custom User Data Maven extension:");
            logger.error(e.getMessage(), e);
        }
    }

}
