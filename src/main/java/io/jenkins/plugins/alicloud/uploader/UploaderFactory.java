package io.jenkins.plugins.alicloud.uploader;

import io.jenkins.plugins.alicloud.edas.enumeration.ClusterType;

public class UploaderFactory {
    public static Uploader getUploader(int type, String regionId, String appId, String edasEndpoint) {
        if (type == ClusterType.ECS_CLUSTER_TYPE.value() || type == ClusterType.K8S_CLUSTER_TYPE.value()) {
            return new PublicOssUploader(edasEndpoint, regionId, appId);
        }
        return null;
    }
}
