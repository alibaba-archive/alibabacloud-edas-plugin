package io.jenkins.plugins.alicloud.edas.ecs;

import lombok.Data;

@Data
public class EDASEcsConfig {
    /*
     * deploy
     */
    private String appId;
    private String group;
    private String packageUrl;

    // advanced
    private Integer batch;
    private Integer batchWaitTime;
    private String version;
    private String desc;
    private Long releaseType;


    /*
     * insert
     */
    private String appName;
    private String packageType;
    private String clusterId;
    private String ecuInfo;
//    private String packageUrl;

    // advanced
    private Integer buildPackId;
//    private String desc;
    private String healthCheckUrl;
    private String namespace;
//    private String version;

}
