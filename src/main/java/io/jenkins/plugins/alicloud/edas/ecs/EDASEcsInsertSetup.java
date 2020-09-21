package io.jenkins.plugins.alicloud.edas.ecs;

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

public class EDASEcsInsertSetup extends BaseSetup {
    private String credentialsString;
    private String namespace;
    private String regionId;
    private String applicationName;
    private String targetObject;
    private String packageType;
    private String clusterId;
    private String ecuInfo;

    private String versionLabelFormat;
    private String versionDescriptionFormat;
    private String buildPackId;
    private String healthCheckUrl;

    @DataBoundConstructor
    public EDASEcsInsertSetup(
            String namespace,
            String credentialsString,
            String applicationName,
            String targetObject,
            String clusterId,
            String packageType,
            String ecuInfo) {
        this.regionId = namespace.split(":")[0];
        this.namespace = namespace;
        this.credentialsString = credentialsString;
        this.clusterId = clusterId;
        this.applicationName = applicationName;
        this.targetObject = targetObject;
        this.packageType = packageType;
        this.ecuInfo = ecuInfo;
    }

    public String getCredentialsString() {
        return credentialsString;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getRegion() {
        return regionId;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public String getTargetObject() {
        return targetObject;
    }

    public String getClusterId() {
        return clusterId;
    }


    public String getEcuInfo() {
        return ecuInfo;
    }

    public String getVersionLabelFormat() {
        return versionLabelFormat;
    }

    public String getVersionDescriptionFormat() {
        return versionDescriptionFormat;
    }

    public String getBuildPackId() {
        return buildPackId;
    }

    public String getHealthCheckUrl() {
        return healthCheckUrl;
    }

    @DataBoundSetter
    public void setVersionLabelFormat(String versionLabelFormat) {
        this.versionLabelFormat = versionLabelFormat;
    }

    @DataBoundSetter
    public void setVersionDescriptionFormat(String versionDescriptionFormat) {
        this.versionDescriptionFormat = versionDescriptionFormat;
    }

    @DataBoundSetter
    public void setBuildPackId(String buildPackId) {
        this.buildPackId = buildPackId;
    }

    @DataBoundSetter
    public void setHealthCheckUrl(String healthCheckUrl) {
        this.healthCheckUrl = healthCheckUrl;
    }

    @Override
    public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
        EDASEcsCreator creator = new EDASEcsCreator(run, workspace, listener, this);

        try {
            boolean result = creator.perform();
            if (!result) {
                throw new AbortException("edas application create failed");
            }
        } catch (AbortException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new AbortException(String.format("edas application create failed for %s", e.getMessage()));
        }
    }


    public static DescriptorImpl getDesc() {
        return new DescriptorImpl();
    }

    public String getPackageType() {
        return packageType;
    }

    @Extension
    @Symbol("InsertApplication")
    public static class DescriptorImpl extends BaseSetupDescriptor {
        @Override
        public String getDisplayName() {
            return "Create EDAS ECS Application";
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

        public FormValidation doCheckClusterId(@QueryParameter String value) {
            if (value.length() == 0) {
                return FormValidation.error("Please set Cluster ID");
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckApplicationName(@QueryParameter String value) {
            if (value.length() == 0) {
                return FormValidation.error("Please set Application Name");
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckTargetObject(@QueryParameter String value) {
            if (value.length() == 0) {
                return FormValidation.error("Please set Target Object");
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckPackageType(@QueryParameter String value) {
            if (value.length() == 0) {
                return FormValidation.error("Please set Package Type");
            }
            if ("jar".equalsIgnoreCase(value)
                || "war".equalsIgnoreCase(value)) {
                return FormValidation.ok();
            }
            return FormValidation.error("Please set Package Type as one of jar/war");
        }

        public FormValidation doCheckEcuInfo(@QueryParameter String value) {
            if (value.length() == 0) {
                return FormValidation.error("Please set ecu info");
            }
            return FormValidation.ok();
        }
    }
}
