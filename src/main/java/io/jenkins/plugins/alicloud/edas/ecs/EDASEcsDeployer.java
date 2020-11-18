package io.jenkins.plugins.alicloud.edas.ecs;

import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.edas.model.v20170801.GetApplicationResponse;
import com.google.common.base.Strings;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import io.jenkins.plugins.alicloud.edas.EDASService;
import io.jenkins.plugins.alicloud.edas.EDASUtils;
import io.jenkins.plugins.alicloud.edas.enumeration.ClusterType;
import io.jenkins.plugins.alicloud.edas.EDASDeployer;
import io.jenkins.plugins.alicloud.edas.enumeration.ReleaseType;
import io.jenkins.plugins.alicloud.uploader.Uploader;
import io.jenkins.plugins.alicloud.uploader.UploaderFactory;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.StringUtils;

public class EDASEcsDeployer extends EDASDeployer {
    private final Run<?, ?> run;
    private final TaskListener listener;
    private final EDASEcsDeploySetup envSetup;
    private final FilePath workspace;

    private final String regionId;
    private final String appId;
    private final String versionLabel;
    private final String desc;
    private final String credentialId;
    private final String targetObject;
    private static final Logger logger = Logger.getLogger(EDASEcsDeployer.class.getName());

    public EDASEcsDeployer(Run<?, ?> run, FilePath workspace, TaskListener listener,
        EDASEcsDeploySetup setup) {
        super(setup.getCredentialId(), setup.getRegion(), listener, setup.getEndpoint());
        this.run = run;
        this.listener = listener;
        this.envSetup = setup;
        this.workspace = workspace;

        appId = EDASUtils.getValue(run, listener, envSetup.getAppId());
        versionLabel = EDASUtils.getValue(run, listener, envSetup.getVersionLabelFormat());
        desc = EDASUtils.getValue(run, listener, envSetup.getVersionDescriptionFormat());
        regionId =  envSetup.getRegion();
        credentialId = envSetup.getCredentialId();
        targetObject = EDASUtils.getValue(run, listener, envSetup.getTargetObject());

    }

    @Override
    public String doUploader(String appId) throws Exception {
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
        EDASUtils.edasLog(listener, String.format("start to upload file %s", file.getAbsolutePath()));
        return uploader.upload(file, defaultAcsClient);
    }

    @Override
    public String doCheckApplication() {
        DefaultAcsClient defaultAcsClient = EDASService.getAcsClient(credentialId, regionId, envSetup.getEndpoint());
        if (defaultAcsClient == null) {
            return "";
        }
        try {
            GetApplicationResponse.Applcation applcation = EDASService.getApplication(appId, defaultAcsClient);
            if (applcation == null || Strings.isNullOrEmpty(applcation.getAppId())) {
                logger.log(Level.INFO,"no k8s application found wiht appId", appId);
                return "";
            }

            return applcation.getAppId();
        } catch (Exception e) {
            e.printStackTrace();
            logger.log(Level.SEVERE, e.getMessage());
            return "";
        }
    }

    @Override
    public String doDeploy(String appId, String url) {
        DefaultAcsClient defaultAcsClient = EDASService.getAcsClient(credentialId, regionId, envSetup.getEndpoint());
        if (defaultAcsClient == null) {
            return "";
        }
        EDASUtils.edasLog(listener, "start to deploy to ecs application");
        logger.log(Level.INFO, "start to deploy to ecs application");
        EDASEcsConfig edasEcsConfig = new EDASEcsConfig();
        edasEcsConfig.setAppId(appId);
        edasEcsConfig.setPackageUrl(url);
        edasEcsConfig.setDesc(desc);
        int batch;
        try {
            batch = Integer.parseInt(envSetup.getBatch());
        } catch (NumberFormatException e) {
            batch = 1;
        }
        edasEcsConfig.setBatch(batch);

        int batchWaitTime;
        try {
            batchWaitTime = Integer.parseInt(envSetup.getBatchWaitTime());
        } catch (NumberFormatException e) {
            batchWaitTime = 0;
        }
        edasEcsConfig.setBatchWaitTime(batchWaitTime);

        long releaseType;
        if (envSetup.getReleaseType().equals(ReleaseType.MANUAL)) {
            releaseType = 1;
        } else {
            releaseType = 0;
        }
        edasEcsConfig.setReleaseType(releaseType);

        String version = versionLabel;
        if (StringUtils.isBlank(versionLabel)) {
            version = EDASUtils.getCurrentTime();
        }
        edasEcsConfig.setVersion(version);

        String group = envSetup.getGroup();
        if (group.equalsIgnoreCase("all")) {
            group = "all";
        }
        edasEcsConfig.setGroup(group);

        try {
            return EDASEcsService.deployToEcsCluster(defaultAcsClient, edasEcsConfig);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }
}
