package io.jenkins.plugins.alicloud;

import hudson.DescriptorExtensionList;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;

public abstract class BaseSetupDescriptor extends Descriptor<BaseSetup> {

    public static DescriptorExtensionList<BaseSetup, BaseSetupDescriptor> all() {
        return Jenkins.getInstance().getDescriptorList(BaseSetup.class);
    }
}
