package io.jenkins.plugins.alicloud.edas;

import com.aliyuncs.DefaultAcsClient;
import hudson.model.TaskListener;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.StringUtils;

public abstract class EDASDeployer {
    private static final Logger logger = Logger.getLogger(EDASDeployer.class.getName());

    private String credentialId;
    private String regionId;
    private ChangeOrderManager changeOrderManager;
    private TaskListener listener;
    private String endpoint;

    public EDASDeployer(String credentialId, String regionId, TaskListener listener, String endpoint) {
        this.credentialId = credentialId;
        this.regionId = regionId;
        this.listener = listener;
        this.endpoint = endpoint;
        this.changeOrderManager = new ChangeOrderManager();
    }


    public boolean perform() throws Exception {
        String appId = doCheckApplication();
        if (StringUtils.isBlank(appId)) {
            logger.log(Level.SEVERE, String.format("no application found with appId %s", appId));
            return false;
        }
        EDASUtils.edasLog(listener, String.format("appId %s found\n", appId));
        logger.log(Level.INFO, String.format("appId %s found\n", appId));
        String downloadUrl = doUploader(appId);
        if (StringUtils.isBlank(downloadUrl)) {
            logger.log(Level.SEVERE, "upload package failed");
            return false;
        }
        EDASUtils.edasLog(listener, String.format("package url %s\n", downloadUrl));
        logger.log(Level.INFO, String.format("package url %s\n", downloadUrl));
        String changeOrderId = doDeploy(appId, downloadUrl);
        if (StringUtils.isBlank(changeOrderId)) {
            return false;
        }
        EDASUtils.edasLog(listener, String.format("changeOrderId %s\n", changeOrderId));
        doTrace(changeOrderId);
        return true;
    }

    public abstract String doCheckApplication();

    public abstract String doUploader(String appId) throws Exception;

    public abstract String doDeploy(String appId, String url);

    private boolean doTrace(String changeOrderId) throws Exception {
        DefaultAcsClient defaultAcsClient = EDASService.getAcsClient(credentialId, regionId, endpoint);
        if (defaultAcsClient == null) {
            return false;
        }
        return changeOrderManager.trace(defaultAcsClient, changeOrderId);
    }

}
