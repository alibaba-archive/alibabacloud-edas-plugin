package io.jenkins.plugins.alicloud.edas;

import lombok.Data;

@Data
public class RunningEnvironment {
    private String jdk;
    private String webContainer;
}
