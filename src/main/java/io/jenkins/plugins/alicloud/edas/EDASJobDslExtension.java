package io.jenkins.plugins.alicloud.edas;

import hudson.Extension;
import javaposse.jobdsl.dsl.RequiresPlugin;
import javaposse.jobdsl.dsl.helpers.publisher.PublisherContext;
import javaposse.jobdsl.plugin.ContextExtensionPoint;
import javaposse.jobdsl.plugin.DslExtensionMethod;

@Extension(optional = true)
public class EDASJobDslExtension extends ContextExtensionPoint {
    @RequiresPlugin(id = "edas-jenkins-plugin", minimumVersion = "1.0")
    @DslExtensionMethod(context = PublisherContext.class)
    public Object edasClient(Runnable closure) {
        EDASJobDslContext context = new EDASJobDslContext();
        executeInContext(closure, context);
        return new DeployToEDAS(context.extensions);
    }
}
