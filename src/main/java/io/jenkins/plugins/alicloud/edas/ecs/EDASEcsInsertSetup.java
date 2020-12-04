package io.jenkins.plugins.alicloud.edas.ecs;

import com.alibabacloud.credentials.plugin.auth.AlibabaCredentials;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Item;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import io.jenkins.plugins.alicloud.BaseSetup;
import io.jenkins.plugins.alicloud.BaseSetupDescriptor;
import io.jenkins.plugins.alicloud.edas.EDASService;
import java.io.IOException;
import java.util.Collections;
import java.util.Objects;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

public class EDASEcsInsertSetup extends BaseSetup {
    private String credentialId;
    private String namespace;
    private String endpoint;
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
            String credentialId,
            String applicationName,
            String targetObject,
            String clusterId,
            String packageType,
            String ecuInfo) {
        this.regionId = namespace.split(":")[0];
        this.namespace = namespace;
        this.credentialId = credentialId;
        this.clusterId = clusterId;
        this.applicationName = applicationName;
        this.targetObject = targetObject;
        this.packageType = packageType;
        this.ecuInfo = ecuInfo;
    }

    public String getCredentialId() {
        return credentialId;
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

    public String getEndpoint() {
        if (StringUtils.isBlank(endpoint)) {
            return "edas.aliyuncs.com";
        }
        return endpoint;
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

    @DataBoundSetter
    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
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
    @Symbol("InsertEDASApplication")
    public static class DescriptorImpl extends BaseSetupDescriptor {
        @Override
        public String getDisplayName() {
            return "Create EDAS ECS Application";
        }

        public ListBoxModel doFillCredentialIdItems(@AncestorInPath Item owner) {
            if (Objects.isNull(owner) || !owner.hasPermission(Item.CONFIGURE)) {
                return new ListBoxModel(new ListBoxModel.Option(StringUtils.EMPTY));
            }

            return new StandardListBoxModel()
                .withEmptySelection()
                .withMatching(
                    CredentialsMatchers.always(),
                    CredentialsProvider.lookupCredentials(AlibabaCredentials.class,
                        owner.getParent(), ACL.SYSTEM, Collections.EMPTY_LIST));
        }

        public FormValidation doCheckCredentialId(@QueryParameter String value, @AncestorInPath Item owner) {
            return EDASService.checkCredentialId(value, owner);
        }

        public FormValidation doCheckNamespace(@QueryParameter String value) {
            if (StringUtils.isBlank(value)) {
                return FormValidation.error("Please set Namespace");
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckClusterId(@QueryParameter String value) {
            if (StringUtils.isBlank(value)) {
                return FormValidation.error("Please set Cluster ID");
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckApplicationName(@QueryParameter String value) {
            if (StringUtils.isBlank(value)) {
                return FormValidation.error("Please set Application Name");
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckTargetObject(@QueryParameter String value) {
            if (StringUtils.isBlank(value)) {
                return FormValidation.error("Please set Target Object");
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckPackageType(@QueryParameter String value) {
            if (StringUtils.isBlank(value)) {
                return FormValidation.error("Please set Package Type");
            }
            if ("jar".equalsIgnoreCase(value)
                || "war".equalsIgnoreCase(value)) {
                return FormValidation.ok();
            }
            return FormValidation.error("Please set Package Type as one of jar/war");
        }

        public FormValidation doCheckEcuInfo(@QueryParameter String value) {
            if (StringUtils.isBlank(value)) {
                return FormValidation.error("Please set ecu info");
            }
            return FormValidation.ok();
        }

        public FormValidation doPingEDAS(
            @AncestorInPath Item owner,
            @QueryParameter("credentialId") String credentialId,
            @QueryParameter("namespace") String namespace,
            @QueryParameter("endpoint") String endpoint) {
            if (Objects.isNull(owner) || !owner.hasPermission(Item.CONFIGURE)) {
                return FormValidation.error("No permission");
            }
            return EDASService.pingEDAS(credentialId, namespace, endpoint);
        }
    }
}
