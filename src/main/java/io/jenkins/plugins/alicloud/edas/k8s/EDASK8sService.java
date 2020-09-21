package io.jenkins.plugins.alicloud.edas.k8s;

import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.edas.model.v20170801.DeployK8sApplicationRequest;
import com.aliyuncs.edas.model.v20170801.DeployK8sApplicationResponse;
import com.aliyuncs.edas.model.v20170801.InsertK8sApplicationRequest;
import com.aliyuncs.edas.model.v20170801.InsertK8sApplicationResponse;
import io.jenkins.plugins.alicloud.edas.EDASService;
import io.jenkins.plugins.alicloud.edas.EDASUtils;
import io.jenkins.plugins.alicloud.edas.RunningEnvironment;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EDASK8sService {
    private static final Logger logger = Logger.getLogger(EDASUtils.class.getName());
    public static String deployToK8sCluster(DefaultAcsClient acsClient, EDASK8sConfig config, boolean image) throws Exception {
        RunningEnvironment runningEnvironment = EDASService.getK8sAppRunningEnvironment(config.getAppId(), acsClient);
        DeployK8sApplicationRequest request = new DeployK8sApplicationRequest();
        request.putHeaderParameter("X-EDAS-DEPLOY-SOURCE", "Jenkins-plugin-1.0");

        request.setAppId(config.getAppId());
        if (image) {
            request.setImage(config.getImage());
        } else {
            request.setPackageUrl(config.getPackageUrl());
            request.setPackageVersion(config.getVersionLabelFormat());
        }

        if (config.getJdk() != null) {
            request.setJDK(config.getJdk());
        } else {
            request.setJDK(runningEnvironment.getJdk());
        }

        if (config.getWebContainer() != null) {
            request.setWebContainer(config.getWebContainer());
        } else {
            request.setWebContainer(runningEnvironment.getWebContainer());
        }
        if (config.getEdasContainerVersion() != null) {
            request.setEdasContainerVersion(config.getEdasContainerVersion());
        }

        if (config.getEnvs() != null) {
            request.setEnvs(config.getEnvs());
        }

        if (config.getStartupCommand() != null) {
            request.setCommand(config.getStartupCommand());
        }

        if (config.getArgs() != null) {
            request.setArgs(config.getArgs());
        }

        if (config.getCpuLimit() != null) {
            request.setMcpuLimit(config.getCpuLimit());
        }
        if (config.getMemoryLimit() != null) {
            request.setMemoryLimit(config.getMemoryLimit());
        }
        if (config.getCpuRequest() != null) {
            request.setMcpuRequest(config.getCpuRequest());
        }
        if (config.getMemoryRequest() != null) {
            request.setMemoryRequest(config.getMemoryRequest());
        }
        if (config.getReplicas() != null) {
            request.setReplicas(config.getReplicas());
        }

        if (config.getPostStart() != null) {
            request.setPostStart(config.getPostStart());
        }

        if (config.getPreStop() != null) {
            request.setPreStop(config.getPreStop());
        }

        if (config.getLiveness() != null) {
            request.setLiveness(config.getLiveness());
        }

        if (config.getReadiness() != null) {
            request.setReadiness(config.getReadiness());
        }

        if (config.getUpdateStrategy() != null) {
            request.setUpdateStrategy(config.getUpdateStrategy());
        }

        logger.log(Level.INFO, String.format("DeployK8sApplicationRequest send, appId %s", request.getAppId()));
        DeployK8sApplicationResponse response = acsClient.getAcsResponse(request);
        if (response.getCode() == 200) {
            String changeOrderId = response.getChangeOrderId();
            logger.log(Level.INFO,"Deploy k8s application request sent, changeOrderId is: " + changeOrderId);
            return changeOrderId;
        } else {
            String msg = String.format(
                "Failed to send deploy request, requestId: %s, code: %d, msg: %s",
                response.getRequestId(),
                response.getCode(),
                response.getMessage());
            logger.log(Level.SEVERE, msg);
            throw new Exception(msg);
        }

    }

    public static EDASService.EDASInsertStruct insertToK8sCluster(DefaultAcsClient acsClient, EDASK8sConfig config, boolean image) throws Exception {
        InsertK8sApplicationRequest request = new InsertK8sApplicationRequest();

        request.setAppName(config.getAppName());
        request.setClusterId(config.getClusterId());
        request.setPackageType(config.getPackageType());
        request.setNamespace(config.getK8sNamespace());
        request.putHeaderParameter("X-EDAS-DEPLOY-SOURCE", "Jenkins-plugin-1.0");


        if (image) {
            request.setImageUrl(config.getImage());
        } else {
            request.setPackageUrl(config.getPackageUrl());
            request.setPackageVersion(config.getVersionLabelFormat());
        }

        if (config.getDesc() != null) {
            request.setApplicationDescription(config.getDesc());
        }

        if (config.getJdk() != null) {
            request.setJDK(config.getJdk());
        }

        if (config.getWebContainer() != null) {
            request.setWebContainer(config.getWebContainer());
        }

        if (config.getEdasContainerVersion() != null) {
            request.setEdasContainerVersion(config.getEdasContainerVersion());
        }

        if (config.getEnvs() != null) {
            request.setEnvs(config.getEnvs());
        }

        if (config.getStartupCommand() != null) {
            request.setCommand(config.getStartupCommand());
        }

        if (config.getArgs() != null) {
            request.setCommandArgs(config.getArgs());
        }

        if (config.getCpuLimit() != null) {
            request.setLimitmCpu(config.getCpuLimit());
        }
        if (config.getMemoryLimit() != null) {
            request.setLimitMem(config.getMemoryLimit());
        }
        if (config.getCpuRequest() != null) {
            request.setRequestsmCpu(config.getCpuRequest());
        }
        if (config.getMemoryRequest() != null) {
            request.setRequestsMem(config.getMemoryRequest());
        }
        if (config.getReplicas() != null) {
            request.setReplicas(config.getReplicas());
        }

        if (config.getPostStart() != null) {
            request.setPostStart(config.getPostStart());
        }

        if (config.getPreStop() != null) {
            request.setPreStop(config.getPreStop());
        }

        if (config.getLiveness() != null) {
            request.setLiveness(config.getLiveness());
        }

        if (config.getReadiness() != null) {
            request.setReadiness(config.getReadiness());
        }

        logger.log(Level.INFO, String.format("InsertK8sApplicationRequest send, appName %s", request.getAppName()));
        InsertK8sApplicationResponse response = acsClient.getAcsResponse(request);
        if (response.getCode() == 200 && response.getApplicationInfo() != null) {
            String changeOrderId = response.getApplicationInfo().getChangeOrderId();
            String appId = response.getApplicationInfo().getAppId();
            EDASService.EDASInsertStruct struct = new EDASService.EDASInsertStruct(changeOrderId, appId);
            logger.log(Level.INFO,"Deploy k8s application request sent, changeOrderId is: " + changeOrderId);
            return struct;
        } else {
            String msg = String.format(
                "Failed to send deploy request, requestId: %s, code: %d, msg: %s",
                response.getRequestId(),
                response.getCode(),
                response.getMessage());
            logger.log(Level.SEVERE, msg);
            throw new Exception(msg);
        }

    }
}
