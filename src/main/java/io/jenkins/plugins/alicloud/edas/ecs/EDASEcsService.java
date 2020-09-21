package io.jenkins.plugins.alicloud.edas.ecs;

import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.edas.model.v20170801.DeployApplicationRequest;
import com.aliyuncs.edas.model.v20170801.DeployApplicationResponse;
import com.aliyuncs.edas.model.v20170801.InsertApplicationRequest;
import com.aliyuncs.edas.model.v20170801.InsertApplicationResponse;
import io.jenkins.plugins.alicloud.edas.EDASService;
import io.jenkins.plugins.alicloud.edas.EDASUtils;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.StringUtils;

public class EDASEcsService {
    private static final Logger logger = Logger.getLogger(EDASUtils.class.getName());
    public static String deployToEcsCluster(DefaultAcsClient acsClient, EDASEcsConfig ecsConfig) throws Exception {
        DeployApplicationRequest request = new DeployApplicationRequest();
        request.setAppId(ecsConfig.getAppId());
        request.setGroupId(ecsConfig.getGroup());
        request.setDeployType("url");
        request.setWarUrl(ecsConfig.getPackageUrl());
        request.setBatch(ecsConfig.getBatch());
        request.setBatchWaitTime(ecsConfig.getBatchWaitTime());
        request.setPackageVersion(ecsConfig.getVersion());
        request.setDesc(ecsConfig.getDesc());
        request.setReleaseType(ecsConfig.getReleaseType());

        request.putHeaderParameter("X-EDAS-DEPLOY-SOURCE", "Jenkins-plugin-1.0");


        //send request
        logger.log(Level.INFO,String.format("Sending deploy request to EDAS, appId %s", request.getAppId()));
        DeployApplicationResponse response = acsClient.getAcsResponse(request);
        if (response.getCode() == 200) {
            String changeOrderId = response.getChangeOrderId();
            logger.log(Level.INFO,"Deploy request sent, changeOrderId is: " + changeOrderId);
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

    public static EDASService.EDASInsertStruct insertToEcsCluster(DefaultAcsClient acsClient, EDASEcsConfig ecsConfig) throws Exception {
        InsertApplicationRequest request = new InsertApplicationRequest();
        request.setApplicationName(ecsConfig.getAppName());
        request.setPackageType(ecsConfig.getPackageType());
        request.setClusterId(ecsConfig.getClusterId());
        request.setEcuInfo(ecsConfig.getEcuInfo());
        request.putHeaderParameter("X-EDAS-DEPLOY-SOURCE", "Jenkins-plugin-1.0");


        if (ecsConfig.getBuildPackId() != null) {
            request.setBuildPackId(ecsConfig.getBuildPackId());
        }

        if (StringUtils.isNotBlank(ecsConfig.getDesc())) {
            request.setDescription(ecsConfig.getDesc());
        }

        if (StringUtils.isNotBlank(ecsConfig.getHealthCheckUrl())) {
            request.setHealthCheckURL(ecsConfig.getHealthCheckUrl());
        }

        if (StringUtils.isNotBlank(ecsConfig.getNamespace())) {
            request.setLogicalRegionId(ecsConfig.getNamespace());
        }

        logger.log(Level.INFO, "Sending create request to EDAS...");
        InsertApplicationResponse response = acsClient.getAcsResponse(request);
        if (response.getCode() == 200 && response.getApplicationInfo() != null) {
            String changeOrderId = response.getApplicationInfo().getChangeOrderId();
            String appId = response.getApplicationInfo().getAppId();
            logger.log(Level.INFO,"Create request sent, changeOrderId is: " + changeOrderId);
            return new EDASService.EDASInsertStruct(changeOrderId, appId);
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
