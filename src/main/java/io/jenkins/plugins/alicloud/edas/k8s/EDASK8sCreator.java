package io.jenkins.plugins.alicloud.edas.k8s;

import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.edas.model.v20170801.GetClusterResponse;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import io.jenkins.plugins.alicloud.edas.ChangeOrderManager;
import io.jenkins.plugins.alicloud.edas.EDASService;
import io.jenkins.plugins.alicloud.edas.EDASUtils;
import io.jenkins.plugins.alicloud.edas.enumeration.ClusterType;
import io.jenkins.plugins.alicloud.edas.enumeration.PackageType;
import io.jenkins.plugins.alicloud.uploader.Uploader;
import io.jenkins.plugins.alicloud.uploader.UploaderFactory;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.StringUtils;

public class EDASK8sCreator {
    private final Run<?, ?> run;
    private final TaskListener listener;
    private final EDASK8sInsertSetup envSetup;
    private final FilePath workspace;


    private final String regionId;
    private final String applicationName;
    private final String versionLabel;
    private final String credentialId;
    private final boolean image;
    private static final Logger logger = Logger.getLogger(EDASK8sCreator.class.getName());
    private ChangeOrderManager changeOrderManager = new ChangeOrderManager();


    public EDASK8sCreator(Run<?, ?> run, FilePath workspace, TaskListener listener,
        EDASK8sInsertSetup setup) {
        this.run = run;
        this.listener = listener;
        this.envSetup = setup;
        this.workspace = workspace;

        applicationName = EDASUtils.getValue(run, listener, envSetup.getApplicationName());
        versionLabel = EDASUtils.getValue(run, listener, envSetup.getVersionLabelFormat());
        regionId =  envSetup.getNamespace().split(":")[0];
        credentialId = envSetup.getCredentialId();
        image = envSetup.getPackageType().equalsIgnoreCase("Image");
    }

    private String getRegionIdByClusterId(String clusterId) {
        DefaultAcsClient defaultAcsClient = EDASService.getAcsClient(credentialId, regionId, envSetup.getEndpoint());
        if (defaultAcsClient == null) {
            return "";
        }
        try {
            GetClusterResponse.Cluster cluster = EDASService.getClusterByClusterId(clusterId,
                defaultAcsClient);
            return cluster.getRegionId().split(":")[0];
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public boolean perform() throws Exception {
        if (!validateClusterId()) {
            logger.log(Level.SEVERE, "cluster does not belong to region:" + regionId);
            return false;
        }
        String appId = doCheckApplication();
        if (StringUtils.isNotBlank(appId)) {
            logger.log(Level.SEVERE, "application existed with name:" + applicationName);
            return false;
        }

        String downloadUrl = doUploader();
        if (StringUtils.isBlank(downloadUrl)) {
            logger.log(Level.SEVERE, "upload package failed");
            return false;
        }
        EDASUtils.edasLog(listener, String.format("package url %s\n", downloadUrl));
        logger.log(Level.INFO, String.format("package url %s\n", downloadUrl));

        String changeOrderIdInsert = doInsertApplication(downloadUrl);
        if (StringUtils.isBlank(changeOrderIdInsert)) {
            logger.log(Level.SEVERE, "insert application failed");
            return false;
        }
        doTrace(changeOrderIdInsert);
        return true;
    }

    private boolean validateClusterId() {
        String clusterId = envSetup.getClusterId();
        String regionIdByClusterId = getRegionIdByClusterId(clusterId);
        if (regionIdByClusterId.equalsIgnoreCase(regionId)) {
            return true;
        }
        return false;
    }

    public String doUploader() throws Exception {
        if (image) {
            return envSetup.getTargetObject();
        }
        DefaultAcsClient defaultAcsClient = EDASService.getAcsClient(credentialId, regionId, envSetup.getEndpoint());
        if (defaultAcsClient == null) {
            return "";
        }
        String targetObject = envSetup.getTargetObject();
        PackageType packageType = EDASUtils.getPackageType(targetObject);
        if (packageType.equals(PackageType.REMOTE_FILE)) {
            return targetObject;
        }

        File file= EDASUtils.getLocalFileObject(workspace, targetObject);
        if (file == null || !file.getAbsoluteFile().exists()) {
            logger.log(Level.SEVERE, "no file found in path " + targetObject);
            logger.log(Level.SEVERE, "no file found in abs path " + file.getAbsolutePath());
            return "";
        }

        Uploader uploader = UploaderFactory.getUploader(ClusterType.K8S_CLUSTER_TYPE.value(), regionId,
            "K8S_APP_ID", envSetup.getEndpoint());

        return uploader.upload(file, defaultAcsClient);
    }

    public String doCheckApplication() {
        DefaultAcsClient defaultAcsClient = EDASService.getAcsClient(credentialId, regionId, envSetup.getEndpoint());
        if (defaultAcsClient == null) {
            return "";
        }
        return EDASService.getAppId(defaultAcsClient, regionId, applicationName, envSetup.getNamespace(),
            ClusterType.K8S_CLUSTER_TYPE.value());
    }

    public String doInsertApplication(String url) {
        DefaultAcsClient defaultAcsClient = EDASService.getAcsClient(credentialId, regionId, envSetup.getEndpoint());
        if (defaultAcsClient == null) {
            return "";
        }
        EDASUtils.edasLog(listener, "start to deploy to k8s application");
        logger.log(Level.INFO, "start to deploy to k8s application");
        EDASK8sConfig edask8sConfig = new EDASK8sConfig();
        edask8sConfig.setPackageType(envSetup.getPackageType());

        if (!image) {
            edask8sConfig.setPackageUrl(url);
        } else {
            edask8sConfig.setImage(envSetup.getTargetObject());
        }
        edask8sConfig.setAppName(applicationName);
        edask8sConfig.setClusterId(envSetup.getClusterId());
        edask8sConfig.setK8sNamespace(envSetup.getK8sNamespace());
        edask8sConfig.setLogicalRegionId(envSetup.getNamespace());


        String desc = EDASUtils.getValue(run, listener, envSetup.getDescFormat());
        if (StringUtils.isNotBlank(desc)) {
            edask8sConfig.setDesc(desc);
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

        try {
            EDASService.EDASInsertStruct struct = EDASK8sService.insertToK8sCluster(defaultAcsClient, edask8sConfig, image);
            EDASUtils.edasLog(listener, String.format("k8s application created, appId %s", struct.getAppId()));
            return struct.getChangerOrderId();
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
