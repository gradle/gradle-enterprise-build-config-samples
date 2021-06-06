package com.gradle;

import com.acme.KeyManagementService;
import com.acme.WebserviceClient;
import com.gradle.maven.extension.api.cache.BuildCacheApi;
import com.gradle.maven.extension.api.scan.BuildScanApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provide standardized Gradle Enterprise configuration.
 * By applying the extension, these settings will automatically be applied.
 */
final class CustomGradleEnterpriseConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomGradleEnterpriseConfig.class);

    static void configureBuildScanPublishing(BuildScanApi buildScans) {
        buildScans.value("status", String.valueOf(WebserviceClient.queryStatus()));
    }

    static void configureBuildCache(BuildCacheApi buildCache) {
        try {
            buildCache.getRemote().getServer().getCredentials()
                    .setPassword(KeyManagementService.getBuildCachePassword());
        } catch (Exception e) {
            LOGGER.warn("Failed to read build cache password", e);
        }
    }

    private CustomGradleEnterpriseConfig() {
    }

}
