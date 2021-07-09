package com.gradle;

import com.gradle.maven.extension.api.GradleEnterpriseApi;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.monitor.logging.DefaultLog;
import org.codehaus.plexus.logging.Logger;

import javax.annotation.Nullable;
import java.io.File;

final class GroovyScriptUserData {

    static void evaluate(@Nullable MavenSession session, GradleEnterpriseApi gradleEnterprise, Logger logger) throws MavenExecutionException {
        if (session == null) {
            logger.warn("Cannot evaluate groovy script when the Maven session in not available");
            return;
        }
        File groovyScript = getGroovyScript(session);
        if (groovyScript.exists()) {
            logger.debug("Evaluating custom user data Groovy script: " + groovyScript);
            evaluateGroovyScript(session, gradleEnterprise, logger, groovyScript);
        } else {
            logger.debug("Skipping evaluation of custom user data Groovy script because it does not exist: " + groovyScript);
        }
    }

    private static File getGroovyScript(MavenSession session) {
        File rootDir = session.getRequest().getMultiModuleProjectDirectory();
        return new File(rootDir, ".mvn/gradle-enterprise-custom-user-data.groovy");
    }

    private static void evaluateGroovyScript(MavenSession session, GradleEnterpriseApi gradleEnterprise, Logger logger, File groovyScript) throws MavenExecutionException {
        try {
            Binding binding = prepareBinding(session, gradleEnterprise, logger);
            new GroovyShell(GroovyScriptUserData.class.getClassLoader(), binding).evaluate(groovyScript);
        } catch (Exception e) {
            throw new MavenExecutionException("Failed to evaluate custom user data Groovy script: " + groovyScript, e);
        }
    }

    private static Binding prepareBinding(MavenSession session, GradleEnterpriseApi gradleEnterprise, Logger logger) {
        Binding binding = new Binding();
        binding.setVariable("project", session.getTopLevelProject());
        binding.setVariable("session", session);
        binding.setVariable("gradleEnterprise", gradleEnterprise);
        binding.setVariable("buildScan", gradleEnterprise.getBuildScan());
        binding.setVariable("buildCache", gradleEnterprise.getBuildCache());
        binding.setVariable("log", new DefaultLog(logger));
        return binding;
    }

    private GroovyScriptUserData() {
    }

}
