package io.jenkins.plugins.alicloud.edas;

import com.alibabacloud.credentials.plugin.auth.AlibabaCredentials;
import com.alibabacloud.credentials.plugin.client.AlibabaClient;
import com.alibabacloud.credentials.plugin.util.CredentialsHelper;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.edas.model.v20170801.GetApplicationRequest;
import com.aliyuncs.edas.model.v20170801.GetApplicationResponse;
import com.aliyuncs.edas.model.v20170801.GetClusterRequest;
import com.aliyuncs.edas.model.v20170801.GetClusterResponse;
import com.aliyuncs.edas.model.v20170801.GetK8sApplicationRequest;
import com.aliyuncs.edas.model.v20170801.GetK8sApplicationResponse;
import com.aliyuncs.edas.model.v20170801.GetPackageStorageCredentialRequest;
import com.aliyuncs.edas.model.v20170801.GetPackageStorageCredentialResponse;
import com.aliyuncs.edas.model.v20170801.ListApplicationRequest;
import com.aliyuncs.edas.model.v20170801.ListApplicationResponse;
import com.aliyuncs.edas.model.v20170801.QueryRegionConfigRequest;
import com.aliyuncs.edas.model.v20170801.QueryRegionConfigResponse;
import com.aliyuncs.endpoint.EndpointResolver;
import com.aliyuncs.endpoint.ResolveEndpointRequest;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.exceptions.ServerException;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.google.common.base.Strings;
import hudson.model.Item;
import hudson.util.FormValidation;
import io.jenkins.plugins.alicloud.edas.enumeration.ClusterType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.model.Jenkins;
import lombok.Data;
import org.apache.commons.lang.StringUtils;

public abstract class EDASService {
    @Data
    public static class EDASInsertStruct {
        private String changerOrderId;
        private String appId;
        public EDASInsertStruct(String changerOrderId, String appId) {
            this.appId = appId;
            this.changerOrderId = changerOrderId;
        }
    }

    private static final Logger logger = Logger.getLogger(EDASService.class.getName());

    public static GetPackageStorageCredentialResponse.Credential getUploadCredential(DefaultAcsClient acsClient) {
        try {
            GetPackageStorageCredentialRequest request = new GetPackageStorageCredentialRequest();
            GetPackageStorageCredentialResponse response = acsClient.getAcsResponse(request);

            return response.getCredential();

        } catch (ServerException e) {
            e.printStackTrace();
        } catch (ClientException e) {
            e.printStackTrace();
        }
        return null;
    }


    public static List<ListApplicationResponse.Application> getAllApps(DefaultAcsClient acsClient, String regionId,
        String namespace, int type) throws Exception {
        List<ListApplicationResponse.Application> apps = new ArrayList<>();
        try {
            ListApplicationRequest request = new ListApplicationRequest();
            request.setRegionId(regionId);

            ListApplicationResponse response = acsClient.getAcsResponse(request);
            if (response.getCode() == 200) {
                for (ListApplicationResponse.Application application : response.getApplicationList()) {
                    int clusterType = application.getClusterType();
                    if (StringUtils.isNotBlank(namespace)) {
                        if (!application.getRegionId().equals(namespace)) {
                            continue;
                        }
                    }
                    if (type != 0) {
                        if (clusterType != type) {
                            continue;
                        }
                    }

                    apps.add(application);
                }
            } else {
                logger.log(Level.SEVERE, "request failed, requestId:" + response.getRequestId() + "\n");
                logger.log(Level.SEVERE, response.getMessage());
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }

        Collections.sort(apps, new Comparator<ListApplicationResponse.Application>() {
            @Override
            public int compare(ListApplicationResponse.Application o1, ListApplicationResponse.Application o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });

        return apps;
    }


    public static String getAppId(DefaultAcsClient acsClient, String regionId, String appName,
        String namespace, int type) {
        try {
            List<ListApplicationResponse.Application> applications = getAllApps(acsClient, regionId, namespace, type);
            for (ListApplicationResponse.Application application : applications) {
                if (application.getName().equals(appName)) {
                    return application.getAppId();
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage());
        }
        return "";
    }

    public static RunningEnvironment getK8sAppRunningEnvironment(String appId, DefaultAcsClient defaultAcsClient) throws Exception {
        if (StringUtils.isBlank(appId)) {
            throw new Exception("Application must be selected");
        }

        RunningEnvironment runningEnvironment = new RunningEnvironment();
        GetK8sApplicationRequest request = new GetK8sApplicationRequest();

        request.setAppId(appId);
        request.setFrom("deploy");

        GetK8sApplicationResponse response = defaultAcsClient.getAcsResponse(request);
        if (response.getCode() == 200) {
            if (response.getApplcation().getDeployGroups().size() > 0) {
                for (GetK8sApplicationResponse.Applcation.DeployGroup.ComponentsItem componentsItem : response.getApplcation().getDeployGroups().get(0).getComponents()) {
                    if (componentsItem.getComponentKey().startsWith("Open JDK")) {
                        runningEnvironment.setJdk(componentsItem.getComponentKey());
                    }
                }
            }
            if (response.getApplcation().getApp().getBuildpackId() == -1 && response.getApplcation().getApp().getApplicationType().equals("War")) {
                if (response.getApplcation().getDeployGroups().size() > 0) {
                    for (GetK8sApplicationResponse.Applcation.DeployGroup.ComponentsItem componentsItem : response.getApplcation().getDeployGroups().get(0).getComponents()) {
                        if (componentsItem.getComponentKey().startsWith("apache-tomcat-")) {
                            runningEnvironment.setWebContainer(componentsItem.getComponentKey());
                        }
                    }
                } else {
                    runningEnvironment.setWebContainer("apache-tomcat-7.0.91");
                }
            } else {
                runningEnvironment.setWebContainer("");
            }
            return runningEnvironment;
        } else {
            throw new Exception(response.getMessage());
        }
    }

    public static GetClusterResponse.Cluster getClusterByClusterId(String clusterId, DefaultAcsClient defaultAcsClient) throws Exception {
        GetClusterRequest request = new GetClusterRequest();
        request.setClusterId(clusterId);
        GetClusterResponse response = defaultAcsClient.getAcsResponse(request);

        if (response.getCode() == 200 ) {
            return response.getCluster();
        } else {
            String msg = String.format(
                "Failed to get cluster, requestId: %s, code: %d, msg: %s",
                response.getRequestId(),
                response.getCode(),
                response.getMessage());
            throw new Exception(msg);
        }
    }

    public static GetApplicationResponse.Applcation getApplication(String appId, DefaultAcsClient defaultAcsClient) throws Exception {
        GetApplicationRequest request = new GetApplicationRequest();
        request.setAppId(appId);
        GetApplicationResponse response = defaultAcsClient.getAcsResponse(request);

        if (response.getCode() == 200 ) {
            return response.getApplcation();
        } else {
            String msg = String.format(
                "Failed to get application, requestId: %s, code: %d, msg: %s",
                response.getRequestId(),
                response.getCode(),
                response.getMessage());
            throw new Exception(msg);
        }
    }

    public static QueryRegionConfigResponse.RegionConfig queryRegionConfigResponse(DefaultAcsClient client) {
        try {
            QueryRegionConfigRequest request = new QueryRegionConfigRequest();


            QueryRegionConfigResponse response = client.getAcsResponse(request);
            if (response.getCode() == 200) {
                return response.getRegionConfig();
            } else {
                String msg = String.format(
                    "Failed to get queryRegionConfig, requestId: %s, code: %d, msg: %s",
                    response.getRequestId(),
                    response.getCode(),
                    response.getMessage());
                throw new Exception(msg);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }

        return null;
    }

    public static FormValidation pingEDAS(String credentialId, String namespace, String endpoint) {
        if (StringUtils.isBlank(credentialId)) {
            return FormValidation.error("CredentialId cannot be empty");
        }

        if (StringUtils.isBlank(namespace)) {
            return FormValidation.error("Namespace cannot be empty");
        }
        String region = namespace.split(":")[0];

        try {
            List<ListApplicationResponse.Application> apps = EDASService.getAllApps(
                getAcsClient(credentialId, namespace, endpoint),
                region, EDASUtils.ALL_NAMESPACE, ClusterType.ALL_KINDS.value());
        } catch (Exception e) {
            return FormValidation.error(e.getMessage());
        }

        return FormValidation.ok("success");
    }

    public static FormValidation checkCredentialId(String value, Item owner) {
        if (owner == null) {
            if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
                return FormValidation.ok();
            }
        } else {
            if (!owner.hasPermission(Item.EXTENDED_READ)
                && !owner.hasPermission(CredentialsProvider.USE_ITEM)) {
                return FormValidation.ok();
            }
        }
        if (StringUtils.isBlank(value)) {
            return FormValidation.error("Please choose EDAS Credentials");
        }
        if (value.startsWith("${") && value.endsWith("}")) {
            return FormValidation.warning("Cannot validate expression based credentials");
        }
        if (CredentialsHelper.getCredentials(value) == null) {
            return FormValidation.error("Cannot find currently selected credentials");
        }
        return FormValidation.ok();
    }

    public static DefaultAcsClient getAcsClient(String credentialId, String regionId, String endpoint) {
        AlibabaCredentials credentials = CredentialsHelper.getCredentials(credentialId);
        if (credentials == null) {
            logger.log(Level.SEVERE,"no credentials found");
            return null;
        }
        String region = regionId.split(":")[0];
        AlibabaClient client = new AlibabaClient(credentials, region);
        DefaultAcsClient defaultAcsClient = (DefaultAcsClient)client.getClient();
        defaultAcsClient.setEndpointResolver(new EndpointResolver() {
            @Override public String resolve(ResolveEndpointRequest request) throws ClientException {
                if (Strings.isNullOrEmpty(endpoint) || endpoint.endsWith("aliyuncs.com")) {
                    return String.format("edas.%s.aliyuncs.com", client.getRegionNo());
                } else {
                    return endpoint;
                }
            }
        });
        return defaultAcsClient;
    }
}
