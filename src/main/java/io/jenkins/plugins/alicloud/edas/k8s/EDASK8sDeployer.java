package io.jenkins.plugins.alicloud.edas.k8s;

import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.edas.model.v20170801.GetApplicationResponse;
import com.google.common.base.Strings;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import io.jenkins.plugins.alicloud.edas.EDASService;
import io.jenkins.plugins.alicloud.edas.enumeration.ClusterType;
import io.jenkins.plugins.alicloud.edas.EDASDeployer;
import io.jenkins.plugins.alicloud.edas.EDASUtils;
import io.jenkins.plugins.alicloud.edas.enumeration.PackageType;
import io.jenkins.plugins.alicloud.uploader.Uploader;
import io.jenkins.plugins.alicloud.uploader.UploaderFactory;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.StringUtils;

public class EDASK8sDeployer extends EDASDeployer {
    private final Run<?, ?> run;
    private final TaskListener listener;
    private final EDASK8sDeploySetup envSetup;
    private final FilePath workspace;


    private final String regionId;
    private final String appId;
    private final String versionLabel;
    private final String credentialId;
    private final String targetObject;
    private final Boolean image;
    private static final Logger logger = Logger.getLogger(EDASK8sDeployer.class.getName());

    public EDASK8sDeployer(Run<?, ?> run, FilePath workspace, TaskListener listener,
        EDASK8sDeploySetup setup) {
        super(setup.getCredentialId(),  EDASUtils.getValue(run, listener, setup.getRegion()), listener, setup.getEndpoint());
        this.run = run;
        this.listener = listener;
        this.envSetup = setup;
        this.workspace = workspace;

        appId = EDASUtils.getValue(run, listener, envSetup.getAppId());
        versionLabel = EDASUtils.getValue(run, listener, envSetup.getVersionLabelFormat());
        regionId =  envSetup.getRegion();
        credentialId = envSetup.getCredentialId();
        targetObject = EDASUtils.getValue(run, listener, envSetup.getTargetObject());
        image = envSetup.getImage();
    }

    @Override
    public String doUploader(String appId) throws Exception {
        if (image) {
            return targetObject;
        }
        DefaultAcsClient defaultAcsClient = EDASService.getAcsClient(credentialId, regionId, envSetup.getEndpoint());
        if (defaultAcsClient == null) {
            return "";
        }
        PackageType packageType = EDASUtils.getPackageType(targetObject);
        if (packageType.equals(PackageType.REMOTE_FILE)) {
            return targetObject;
        }

        Uploader uploader = UploaderFactory.getUploader(ClusterType.K8S_CLUSTER_TYPE.value(), regionId, appId, envSetup.getEndpoint());
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
        // 校检下appId

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
        EDASUtils.edasLog(listener, "start to deploy to k8s application");
        logger.log(Level.INFO, "start to deploy to k8s application");
        EDASK8sConfig edask8sConfig = new EDASK8sConfig();
        edask8sConfig.setAppId(appId);
        if (!image) {
            edask8sConfig.setPackageUrl(url);
        } else {
            edask8sConfig.setImage(targetObject);
        }
        if (StringUtils.isNotBlank(envSetup.getEdasContainerVersion())) {
            edask8sConfig.setEdasContainerVersion(envSetup.getEdasContainerVersion());
        }
        if (StringUtils.isNotBlank(envSetup.getJdk())) {
            edask8sConfig.setJdk(envSetup.getJdk());
        }
        if (StringUtils.isNotBlank(versionLabel)) {
            edask8sConfig.setVersionLabelFormat(versionLabel);
        } else {
            edask8sConfig.setVersionLabelFormat(EDASUtils.getCurrentTime());
        }

        if (StringUtils.isNotBlank(envSetup.getEnvs())) {
            edask8sConfig.setEnvs(envSetup.getEnvs());
        }
        if (StringUtils.isNotBlank(envSetup.getWebContainer())) {
            edask8sConfig.setWebContainer(envSetup.getWebContainer());
        }
        if (envSetup.getStartupCommand() != null && !"unchanging".equalsIgnoreCase(envSetup.getStartupCommand())) {
            edask8sConfig.setStartupCommand(envSetup.getStartupCommand());
        }
        if (StringUtils.isNotBlank(envSetup.getArgs())) {
            edask8sConfig.setArgs(envSetup.getArgs());
        }
        if (StringUtils.isNotBlank(envSetup.getCpuLimit())) {
            edask8sConfig.setCpuLimit(Integer.parseInt(envSetup.getCpuLimit()));
        }
        if (StringUtils.isNotBlank(envSetup.getMemoryLimit())) {
            edask8sConfig.setMemoryLimit(Integer.parseInt(envSetup.getMemoryLimit()));
        }
        if (StringUtils.isNotBlank(envSetup.getCpuRequest())) {
            edask8sConfig.setCpuRequest(Integer.parseInt(envSetup.getCpuRequest()));
        }
        if (StringUtils.isNotBlank(envSetup.getMemoryRequest())) {
            edask8sConfig.setMemoryRequest(Integer.parseInt(envSetup.getMemoryRequest()));
        }
        if (StringUtils.isNotBlank(envSetup.getReplicas())) {
            edask8sConfig.setReplicas(Integer.parseInt(envSetup.getReplicas()));
        }
        if (envSetup.getPostStart() != null && !"unchanging".equalsIgnoreCase(envSetup.getPostStart())) {
            edask8sConfig.setPostStart(envSetup.getPostStart());
        }
        if (envSetup.getPreStop() != null && !"unchanging".equalsIgnoreCase(envSetup.getPreStop())) {
            edask8sConfig.setPreStop(envSetup.getPreStop());
        }
        if (envSetup.getReadiness() != null && !"unchanging".equalsIgnoreCase(envSetup.getReadiness())) {
            edask8sConfig.setReadiness(envSetup.getReadiness());
        }
        if (envSetup.getLiveness() != null && !"unchanging".equalsIgnoreCase(envSetup.getLiveness())) {
            edask8sConfig.setLiveness(envSetup.getLiveness());
        }
        if (StringUtils.isNotBlank(envSetup.getUpdateStrategy())) {
            edask8sConfig.setUpdateStrategy(envSetup.getUpdateStrategy());
        }

        try {
            String changeOrderId = EDASK8sService.deployToK8sCluster(defaultAcsClient, edask8sConfig, image);
            return changeOrderId;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }



}
