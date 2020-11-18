package io.jenkins.plugins.alicloud.edas.ecs;

import com.aliyuncs.DefaultAcsClient;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import io.jenkins.plugins.alicloud.edas.ChangeOrderManager;
import io.jenkins.plugins.alicloud.edas.EDASService;
import io.jenkins.plugins.alicloud.edas.EDASUtils;
import io.jenkins.plugins.alicloud.edas.enumeration.ClusterType;
import io.jenkins.plugins.alicloud.uploader.Uploader;
import io.jenkins.plugins.alicloud.uploader.UploaderFactory;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.StringUtils;

public class EDASEcsCreator {
    private final Run<?, ?> run;
    private final TaskListener listener;
    private final EDASEcsInsertSetup envSetup;
    private final FilePath workspace;

    private final String regionId;
    private final String applicationName;
    private final String versionLabel;
    private final String desc;
    private final String credentialId;
    private final String targetObject;
    private final String namespace;
    private final String packageType;
    private static final Logger logger = Logger.getLogger(EDASEcsCreator.class.getName());
    private String appId;
    private ChangeOrderManager changeOrderManager = new ChangeOrderManager();


    public EDASEcsCreator(Run<?, ?> run, FilePath workspace, TaskListener listener,
        EDASEcsInsertSetup setup) {
        this.run = run;
        this.listener = listener;
        this.envSetup = setup;
        this.workspace = workspace;

        applicationName = EDASUtils.getValue(run, listener, envSetup.getApplicationName());
        versionLabel = EDASUtils.getValue(run, listener, envSetup.getVersionLabelFormat());
        desc = EDASUtils.getValue(run, listener, envSetup.getVersionDescriptionFormat());
        regionId =  envSetup.getRegion();
        credentialId = envSetup.getCredentialId();
        targetObject = EDASUtils.getValue(run, listener, envSetup.getTargetObject());
        namespace = envSetup.getNamespace();
        packageType = envSetup.getPackageType();

    }

    public boolean perform() throws Exception {
        // 检查应用名称
        String appId = doCheckApplication();
        if (StringUtils.isNotBlank(appId)) {
            logger.log(Level.SEVERE, "application existed with name:" + applicationName);
            return false;
        }
        String changeOrderIdInsert = doInsertApplication();
        if (StringUtils.isBlank(changeOrderIdInsert)) {
            logger.log(Level.SEVERE, "insert application failed");
            return false;
        }
        EDASUtils.edasLog(listener, String.format("application created, appId %s", appId));
        doTrace(changeOrderIdInsert);
        String downloadUrl = doUploader();
        if (StringUtils.isBlank(downloadUrl)) {
            logger.log(Level.SEVERE, "upload package failed");
            return false;
        }
        EDASUtils.edasLog(listener, String.format("package url %s", downloadUrl));
        logger.log(Level.INFO, String.format("package url %s\n", downloadUrl));
        String changeOrderId = doDeploy(downloadUrl);
        if (StringUtils.isBlank(changeOrderId)) {
            return false;
        }
        EDASUtils.edasLog(listener, String.format("deploy changerOrder %s", changeOrderId));
        doTrace(changeOrderId);
        return true;
    }

    public String doInsertApplication() {
        DefaultAcsClient defaultAcsClient = EDASService.getAcsClient(credentialId, regionId, envSetup.getEndpoint());
        if (defaultAcsClient == null) {
            return "";
        }
        logger.log(Level.INFO, "start to insert ecs application");
        EDASEcsConfig edasEcsConfig = new EDASEcsConfig();
        edasEcsConfig.setAppName(applicationName);
        edasEcsConfig.setPackageType(packageType);
        edasEcsConfig.setClusterId(envSetup.getClusterId());
        edasEcsConfig.setEcuInfo(envSetup.getEcuInfo());
        if (StringUtils.isNotBlank(envSetup.getBuildPackId())) {
            try {
                edasEcsConfig.setBuildPackId(Integer.parseInt(envSetup.getBuildPackId()));
            } catch (NumberFormatException e) {
                logger.log(Level.SEVERE, e.getMessage());
            }
        }
        edasEcsConfig.setDesc(envSetup.getVersionDescriptionFormat());
        edasEcsConfig.setHealthCheckUrl(envSetup.getHealthCheckUrl());
        edasEcsConfig.setNamespace(envSetup.getNamespace());

        try {
            EDASService.EDASInsertStruct struct =
                EDASEcsService.insertToEcsCluster(defaultAcsClient, edasEcsConfig);
            appId = struct.getAppId();
            EDASUtils.edasLog(listener, String.format("application created, appId %s", appId));
            return struct.getChangerOrderId();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }


    public String doUploader() throws Exception {
        DefaultAcsClient defaultAcsClient = EDASService.getAcsClient(credentialId, regionId, envSetup.getEndpoint());
        if (defaultAcsClient == null) {
            return "";
        }
        Uploader uploader = UploaderFactory.getUploader(ClusterType.ECS_CLUSTER_TYPE.value(), regionId, appId, envSetup.getEndpoint());
        File file= EDASUtils.getLocalFileObject(workspace, targetObject);
        if (file == null || !file.getAbsoluteFile().exists()) {
            logger.log(Level.SEVERE, "no file found in path " + targetObject);
            logger.log(Level.SEVERE, "no file found in abs path " + file.getAbsolutePath());
            return "";
        }

        return uploader.upload(file, defaultAcsClient);
    }

    public String doCheckApplication() {
        DefaultAcsClient defaultAcsClient = EDASService.getAcsClient(credentialId, regionId, envSetup.getEndpoint());
        if (defaultAcsClient == null) {
            return "";
        }
        return EDASService.getAppId(defaultAcsClient, regionId, applicationName, namespace,
            ClusterType.ECS_CLUSTER_TYPE.value());
    }

    public String doDeploy(String url) {
        DefaultAcsClient defaultAcsClient = EDASService.getAcsClient(credentialId, regionId, envSetup.getEndpoint());
        if (defaultAcsClient == null) {
            return "";
        }
        listener.getLogger().println("start to deploy to ecs application");
        logger.log(Level.INFO, "start to deploy to ecs application");
        EDASEcsConfig edasEcsConfig = new EDASEcsConfig();
        edasEcsConfig.setAppId(appId);
        edasEcsConfig.setPackageUrl(url);
        edasEcsConfig.setDesc(desc);

        String version = versionLabel;
        if (StringUtils.isBlank(versionLabel)) {
            version = EDASUtils.getCurrentTime();
        }
        edasEcsConfig.setVersion(version);
        edasEcsConfig.setGroup("all");

        try {
            return EDASEcsService.deployToEcsCluster(defaultAcsClient, edasEcsConfig);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    private boolean doTrace(String changeOrderId) throws Exception {
        DefaultAcsClient defaultAcsClient = EDASService.getAcsClient(credentialId, regionId, envSetup.getEndpoint());
        if (defaultAcsClient == null) {
            return false;
        }
        return changeOrderManager.trace(defaultAcsClient, changeOrderId);
    }
}
