package io.jenkins.plugins.alicloud.edas.k8s;

import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import io.jenkins.plugins.alicloud.AliCloudCredentials;
import io.jenkins.plugins.alicloud.BaseSetup;
import io.jenkins.plugins.alicloud.BaseSetupDescriptor;
import java.io.IOException;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

public class EDASK8sDeploySetup extends BaseSetup {
    private String credentialsString;
    private String regionId;
    private String namespace;
    private String appId;
    private String targetObject;
    private Boolean image;

    // application environment
    private String edasContainerVersion;
    private String webContainer;
    private String jdk;
    private String versionLabelFormat;
    private String envs;
    // start command
    private String startupCommand = "unchanging";
    private String args;
    // resource quota
    private String cpuLimit;
    private String memoryLimit;
    private String cpuRequest;
    private String memoryRequest;
    private String replicas;
    // application management
    private String postStart = "unchanging";
    private String preStop = "unchanging";
    private String readiness = "unchanging";
    private String liveness = "unchanging";
    private String updateStrategy;

    @DataBoundConstructor
    public EDASK8sDeploySetup(
            String namespace,
            String credentialsString,
            String appId,
            String targetObject,
            Boolean image) {
        this.namespace = namespace;
        this.credentialsString = credentialsString;
        this.appId = appId;
        this.image = image;
        this.targetObject = targetObject;
        this.regionId = namespace.split(":")[0];

    }


    public String getRegion() {
        return regionId;
    }


    public String getVersionLabelFormat() {
        return versionLabelFormat == null ? "" : versionLabelFormat;
    }

    public String getCredentialsString() {
        return credentialsString;
    }

    @Override
    public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
        EDASK8sDeployer deployer = new EDASK8sDeployer(run, workspace, listener, this);

        try {
            boolean result = deployer.perform();
            if (!result) {
                throw new AbortException("edas k8s deploy failed");
            }
        } catch (AbortException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new AbortException(String.format("edas k8s deploy failed for %s", e.getMessage()));
        }
    }

    public static DescriptorImpl getDesc() {
        return new DescriptorImpl();
    }

    public String getNamespace() {
        return namespace;
    }

    public String getTargetObject() {
        return targetObject;
    }

    public String getJdk() {
        return jdk;
    }

    @DataBoundSetter
    public void setJdk(String jdk) {
        this.jdk = jdk;
    }

    public String getEdasContainerVersion() {
        return edasContainerVersion;
    }

    @DataBoundSetter
    public void setEdasContainerVersion(String edasContainerVersion) {
        this.edasContainerVersion = edasContainerVersion;
    }

    @DataBoundSetter
    public void setVersionLabelFormat(String versionLabelFormat) {
        this.versionLabelFormat = versionLabelFormat;
    }


    public String getEnvs() {
        return envs;
    }

    @DataBoundSetter
    public void setEnvs(String envs) {
        this.envs = envs;
    }

    public String getStartupCommand() {
        return startupCommand;
    }

    @DataBoundSetter
    public void setStartupCommand(String startupCommand) {
        this.startupCommand = startupCommand;
    }

    public String getArgs() {
        return args;
    }

    @DataBoundSetter
    public void setArgs(String args) {
        this.args = args;
    }

    public String getCpuLimit() {
        return cpuLimit;
    }

    @DataBoundSetter
    public void setCpuLimit(String cpuLimit) {
        this.cpuLimit = cpuLimit;
    }

    public String getMemoryLimit() {
        return memoryLimit;
    }

    @DataBoundSetter
    public void setMemoryLimit(String memoryLimit) {
        this.memoryLimit = memoryLimit;
    }

    public String getCpuRequest() {
        return cpuRequest;
    }

    @DataBoundSetter
    public void setCpuRequest(String cpuRequest) {
        this.cpuRequest = cpuRequest;
    }

    public String getMemoryRequest() {
        return memoryRequest;
    }

    @DataBoundSetter
    public void setMemoryRequest(String memoryRequest) {
        this.memoryRequest = memoryRequest;
    }

    public String getReplicas() {
        return replicas;
    }

    @DataBoundSetter
    public void setReplicas(String replicas) {
        this.replicas = replicas;
    }

    public String getPostStart() {
        return postStart;
    }

    @DataBoundSetter
    public void setPostStart(String postStart) {
        this.postStart = postStart;
    }

    public String getPreStop() {
        return preStop;
    }

    @DataBoundSetter
    public void setPreStop(String preStop) {
        this.preStop = preStop;
    }

    public String getReadiness() {
        return readiness;
    }

    @DataBoundSetter
    public void setReadiness(String readiness) {
        this.readiness = readiness;
    }

    public String getLiveness() {
        return liveness;
    }

    @DataBoundSetter
    public void setLiveness(String liveness) {
        this.liveness = liveness;
    }

    public String getUpdateStrategy() {
        return updateStrategy;
    }

    @DataBoundSetter
    public void setUpdateStrategy(String updateStrategy) {
        this.updateStrategy = updateStrategy;
    }

    public Boolean getImage() {
        return image;
    }

    public void setImage(Boolean image) {
        this.image = image;
    }

    public String getWebContainer() {
        return webContainer;
    }

    @DataBoundSetter
    public void setWebContainer(String webContainer) {
        this.webContainer = webContainer;
    }

    public String getAppId() {
        return appId;
    }

    @Extension
    @Symbol("deployK8sApplication")
    public static class DescriptorImpl extends BaseSetupDescriptor {
        @Override
        public String getDisplayName() {
            return "EDAS K8s Application";
        }

        public ListBoxModel doFillCredentialsStringItems(@QueryParameter String credentials) {
            ListBoxModel items = new ListBoxModel();
            items.add("");
            for (AliCloudCredentials creds : AliCloudCredentials.getCredentials()) {

                items.add(creds, creds.toString());
                if (creds.toString().equals(credentials)) {
                    items.get(items.size() - 1).selected = true;
                }
            }

            return items;
        }

        public FormValidation doCheckcredentialsString(@QueryParameter String value) {
            if (value.length() == 0) {
                return FormValidation.error("Please choose EDAS Credentials");
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckNamespace(@QueryParameter String value) {
            if (value.length() == 0) {
                return FormValidation.error("Please set Namespace");
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckAppId(@QueryParameter String value) {
            if (value.length() == 0) {
                return FormValidation.error("Please set Application Id");
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckTargetObject(@QueryParameter String value) {
            if (value.length() == 0) {
                return FormValidation.error("Please set Target Object");
            }
            return FormValidation.ok();
        }

    }

}
