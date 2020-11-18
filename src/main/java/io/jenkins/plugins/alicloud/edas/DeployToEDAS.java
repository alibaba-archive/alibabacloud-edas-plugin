package io.jenkins.plugins.alicloud.edas;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.Saveable;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.DescribableList;
import io.jenkins.plugins.alicloud.BaseSetup;
import io.jenkins.plugins.alicloud.BaseSetupDescriptor;
import io.jenkins.plugins.alicloud.edas.ecs.EDASEcsDeploySetup;
import io.jenkins.plugins.alicloud.edas.ecs.EDASEcsInsertSetup;
import io.jenkins.plugins.alicloud.edas.k8s.EDASK8sDeploySetup;
import io.jenkins.plugins.alicloud.edas.k8s.EDASK8sInsertSetup;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

public class DeployToEDAS extends Recorder implements SimpleBuildStep {
    private DescribableList<BaseSetup, BaseSetupDescriptor> extensions;

    @DataBoundConstructor
    public DeployToEDAS(List<BaseSetup> extensions) {
        super();
        this.extensions = new DescribableList<>(
            Saveable.NOOP, Util.fixNull(extensions));
    }

    public DescribableList<BaseSetup, BaseSetupDescriptor> getExtensions() {
        if (extensions == null) {
            extensions = new DescribableList<>(Saveable.NOOP,Util.fixNull(extensions));
        }
        return extensions;
    }


    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Override public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher,
        @Nonnull TaskListener listener) throws InterruptedException, IOException {
        BaseSetup.perform(run, workspace, launcher, listener, getExtensions());
    }

    @Extension
    @Symbol("edasClient")
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Deploy to EDAS";
        }

        public List<BaseSetupDescriptor> getExtensionDescriptors() {
            List<BaseSetupDescriptor> extensions = new ArrayList<>(4);
            extensions.add(EDASEcsDeploySetup.getDesc());
            extensions.add(EDASEcsInsertSetup.getDesc());
            extensions.add(EDASK8sDeploySetup.getDesc());
            extensions.add(EDASK8sInsertSetup.getDesc());

            return extensions;
        }


        public DescriptorImpl() {
            load();
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
            save();
            return super.configure(req, json);
        }
    }

}
