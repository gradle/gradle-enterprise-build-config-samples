package com.gradle;

import org.apache.maven.MavenExecutionException;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.logging.Logger;

import java.util.Optional;

import static java.util.Comparator.comparing;

final class ApiAccessor {

    private static final String GRADLE_ENTERPRISE_API_PACKAGE = "com.gradle.maven.extension.api";
    private static final String GRADLE_ENTERPRISE_LISTENER_CLASS = GRADLE_ENTERPRISE_API_PACKAGE + ".GradleEnterpriseListener";

    static boolean ensureGradleEnterpriseListenerIsAccessible(PlexusContainer container, Class<?> extensionClass) throws MavenExecutionException {
        ensureClassIsAccessible(extensionClass, GRADLE_ENTERPRISE_API_PACKAGE);
        try {
            extensionClass.getClassLoader().loadClass(GRADLE_ENTERPRISE_LISTENER_CLASS);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    static void registerExtension(Class<?> extensionClass, Logger logger) {
        try {
            // force loading of the class
            new CommonCustomUserDataGradleEnterpriseListener(logger);
            ClassLoader classLoader = extensionClass.getClassLoader();
            if (classLoader instanceof ClassRealm) {
                ClassRealm extensionRealm = (ClassRealm) classLoader;
                if (!"maven.ext".equals(extensionRealm.getId())) {
                    Optional<ClassRealm> sourceRealm = extensionRealm.getWorld().getRealms().stream()
                            .filter(realm -> realm.getId().contains("com.gradle:gradle-enterprise-maven-extension") || realm.getId().equals("maven.ext"))
                            .max(comparing((ClassRealm realm) -> realm.getId().length()));
                    if (sourceRealm.isPresent()) {
                        try {
                            sourceRealm.get().importFrom(extensionRealm.getId(), "com.gradle");
                        } catch (Exception e) {
                            throw new MavenExecutionException("Could not import package to realm with id " + sourceRealm.get().getId(), e);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Problem registering Common Custom User Data Maven Extension:");
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * Workaround for https://issues.apache.org/jira/browse/MNG-6906
     */
    @SuppressWarnings("SameParameterValue")
    private static void ensureClassIsAccessible(Class<?> extensionClass, String componentPackage) throws MavenExecutionException {
        ClassLoader classLoader = extensionClass.getClassLoader();
        if (classLoader instanceof ClassRealm) {
            ClassRealm extensionRealm = (ClassRealm) classLoader;
            if (!"maven.ext".equals(extensionRealm.getId())) {
                Optional<ClassRealm> sourceRealm = extensionRealm.getWorld().getRealms().stream()
                        .filter(realm -> realm.getId().contains("com.gradle:gradle-enterprise-maven-extension") || realm.getId().equals("maven.ext"))
                        .max(comparing((ClassRealm realm) -> realm.getId().length()));
                if (sourceRealm.isPresent()) {
                    String sourceRealmId = sourceRealm.get().getId();
                    try {
                        extensionRealm.importFrom(sourceRealmId, componentPackage);
                    } catch (Exception e) {
                        throw new MavenExecutionException("Could not import package from realm with id " + sourceRealmId, e);
                    }
                }
            }
        }
    }

    @SuppressWarnings("SameParameterValue")
    private static boolean componentExists(String component, PlexusContainer container) {
        return container.hasComponent(component);
    }

    private ApiAccessor() {
    }

}
