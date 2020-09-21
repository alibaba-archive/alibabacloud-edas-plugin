package io.jenkins.plugins.alicloud;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Run;
import hudson.model.TaskListener;
import java.io.IOException;
import java.util.List;

public abstract class BaseSetup extends AbstractDescribableImpl<BaseSetup> {
    public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener)
        throws InterruptedException, IOException {
        return;
    }


    public static void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener,
        List<BaseSetup> extensions){
        try {
            for (BaseSetup eb : extensions) {
                if (eb != null) {
                    eb.perform(run, workspace, launcher, listener);
                }
            }
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }
    
}
