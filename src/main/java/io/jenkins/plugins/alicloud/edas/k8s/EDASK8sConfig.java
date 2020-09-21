package io.jenkins.plugins.alicloud.edas.k8s;

import lombok.Data;

@Data
public class EDASK8sConfig {
    private String appId;
    private String image;
    private String packageUrl;

    // application environment
    private String edasContainerVersion;
    private String jdk;
    private String versionLabelFormat;
    private String envs;
    private String webContainer;
    // start command
    private String startupCommand;
    private String args;
    // resource quota
    private Integer cpuLimit;
    private Integer memoryLimit;
    private Integer cpuRequest;
    private Integer memoryRequest;
    private Integer replicas;
    // application management
    private String postStart;
    private String preStop;
    private String readiness;
    private String liveness;
    private String updateStrategy;

    /*
     * insert
     */
    private String appName;
    private String clusterId;
    private String k8sNamespace;
    private String logicalRegionId;
    private String packageType;


    private String desc;


}
