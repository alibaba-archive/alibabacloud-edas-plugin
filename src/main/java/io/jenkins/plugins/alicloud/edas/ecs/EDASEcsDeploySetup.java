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
import io.jenkins.plugins.alicloud.edas.enumeration.ReleaseType;
import java.io.IOException;
import java.util.Collections;
import java.util.Objects;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

public class EDASEcsDeploySetup extends BaseSetup {
    private String credentialId;
    private String namespace;
    private String endpoint;
    private String regionId;
    private String appId;
    private String group;
    private String versionLabelFormat;
    private String versionDescriptionFormat;
    private String batch = "1";
    private String batchWaitTime = "0";
    private String targetObject;
    private String releaseType = "auto";


    @DataBoundConstructor
    public EDASEcsDeploySetup(
            String namespace,
            String credentialId,
            String group,
            String appId,
            String targetObject) {
        this.namespace = namespace;
        this.credentialId = credentialId;
        this.group = group;
        this.appId = appId;
        this.targetObject = targetObject;
        this.regionId = namespace.split(":")[0];
    }

    public String getRegion() {
        return regionId;
    }

    public String getAppId() {
        return appId == null ? "" : appId;
    }

    public String getVersionLabelFormat() {
        return versionLabelFormat == null ? "" : versionLabelFormat;
    }

    public String getVersionDescriptionFormat() {
        return versionDescriptionFormat == null ? "" : versionDescriptionFormat;
    }

    public String getCredentialId() {
        return credentialId == null ? "" : credentialId;
    }

    public String getNamespace() {
        return namespace == null ? "" : namespace;
    }

    public String getGroup() {
        return group == null ? "" : group;
    }

    public String getBatch() {
        return batch == null ? "" : batch;
    }

    public String getBatchWaitTime() {
        return batchWaitTime == null ? "" : batchWaitTime;
    }

    public String getTargetObject() {
        return targetObject == null ? "" : targetObject;
    }

    public String getReleaseType() {
        if (releaseType == null) {
            return ReleaseType.AUTO.getName();
        }
        return releaseType;
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
    public void setBatch(String batch) {
        this.batch = batch;
    }

    @DataBoundSetter
    public void setBatchWaitTime(String batchWaitTime) {
        this.batchWaitTime = batchWaitTime;
    }


    @DataBoundSetter
    public void setReleaseType(String releaseType) {
        ReleaseType.fromName(releaseType);
        this.releaseType = releaseType;
    }

    @DataBoundSetter
    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    @Override
    public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
        EDASEcsDeployer deployer = new EDASEcsDeployer(run, workspace, listener, this);

        try {
            boolean result = deployer.perform();
            if (!result) {
                throw new AbortException("edas deploy failed");
            }
        } catch (AbortException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new AbortException(String.format("edas deploy failed for %s", e.getMessage()));
        }
    }


    public static DescriptorImpl getDesc() {
        return new DescriptorImpl();
    }

    @Extension
    @Symbol("deployEDASApplication")
    public static class DescriptorImpl extends BaseSetupDescriptor {
        @Override
        public String getDisplayName() {
            return "EDAS ECS Application";
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

        public ListBoxModel doFillReleaseTypeItems() {
            ListBoxModel items = new ListBoxModel();
            ReleaseType[] releaseTypes = ReleaseType.values();
            for (ReleaseType releaseType : releaseTypes) {
                items.add(releaseType.getName(), releaseType.getName());
            }

            return items;
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

        public FormValidation doCheckAppId(@QueryParameter String value) {
            if (StringUtils.isBlank(value)) {
                return FormValidation.error("Please set Application ID");
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckGroup(@QueryParameter String value) {
            if (StringUtils.isBlank(value)) {
                return FormValidation.error("Please set group");
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckTargetObject(@QueryParameter String value) {
            if (StringUtils.isBlank(value)) {
                return FormValidation.error("Please set Target Object");
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
