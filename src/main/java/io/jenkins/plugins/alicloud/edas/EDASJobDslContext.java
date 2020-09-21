package io.jenkins.plugins.alicloud.edas;

import io.jenkins.plugins.alicloud.BaseSetup;
import io.jenkins.plugins.alicloud.edas.ecs.EDASEcsDeploySetup;
import io.jenkins.plugins.alicloud.edas.ecs.EDASEcsInsertSetup;
import io.jenkins.plugins.alicloud.edas.k8s.EDASK8sDeploySetup;
import io.jenkins.plugins.alicloud.edas.k8s.EDASK8sInsertSetup;
import java.util.ArrayList;
import java.util.List;
import javaposse.jobdsl.dsl.Context;

public class EDASJobDslContext implements Context {
    List<BaseSetup> extensions = new ArrayList<>();


    void deployApplication(
        String namespace,
        String credentialsString,
        String group,
        String appId,
        String targetObject) {
        EDASEcsDeploySetup edasEcsSetup =
            new EDASEcsDeploySetup(namespace, credentialsString, group, appId, targetObject);
        extensions.add(edasEcsSetup);
    }


    void deployApplication(
        String namespace,
        String credentialsString,
        String group,
        String appId,
        String targetObject,
        String versionLabelFormat,
        String versionDescriptionFormat,
        String batch,
        String batchWaitTime,
        String releaseType) {
        EDASEcsDeploySetup edasEcsSetup =
            new EDASEcsDeploySetup(namespace, credentialsString, group, appId, targetObject);
        edasEcsSetup.setVersionLabelFormat(versionLabelFormat);
        edasEcsSetup.setVersionDescriptionFormat(versionDescriptionFormat);
        edasEcsSetup.setBatch(batch);
        edasEcsSetup.setBatchWaitTime(batchWaitTime);
        edasEcsSetup.setReleaseType(releaseType);
        extensions.add(edasEcsSetup);
    }

    void insertApplication(
        String namespace,
        String credentialsString,
        String applicationName,
        String packageType,
        String clusterId,
        String targetObject,
        String ecuInfo) {
        EDASEcsInsertSetup ecsInsertSetup =
            new EDASEcsInsertSetup(namespace, credentialsString, applicationName, targetObject, clusterId, packageType, ecuInfo);
        extensions.add(ecsInsertSetup);
    }

    void insertApplication(
        String namespace,
        String credentialsString,
        String applicationName,
        String packageType,
        String clusterId,
        String targetObject,
        String ecuInfo,
        String versionLabelFormat,
        String versionDescriptionFormat,
        String buildPackId,
        String healthCheckUrl) {
        EDASEcsInsertSetup ecsInsertSetup =
            new EDASEcsInsertSetup(namespace, credentialsString, applicationName, targetObject, clusterId, packageType, ecuInfo);
        ecsInsertSetup.setVersionLabelFormat(versionLabelFormat);
        ecsInsertSetup.setVersionDescriptionFormat(versionDescriptionFormat);
        ecsInsertSetup.setBuildPackId(buildPackId);
        ecsInsertSetup.setHealthCheckUrl(healthCheckUrl);
        extensions.add(ecsInsertSetup);
    }

    void deployK8sApplication(
        String namespace,
        String credentialsString,
        String appId,
        String targetObject,
        Boolean image) {
        EDASK8sDeploySetup edask8sSetup =
            new EDASK8sDeploySetup(namespace, credentialsString, appId, targetObject, image);
        extensions.add(edask8sSetup);
    }

    void deployK8sApplication(
        String namespace,
        String credentialsString,
        String appId,
        String targetObject,
        Boolean image,
        String edasContainerVersion,
        String webContainer,
        String jdk,
        String versionLabelFormat,
        String envs,
        String startupCommand,
        String args,
        String cpuLimit,
        String memoryLimit,
        String cpuRequest,
        String memoryRequest,
        String replicas,
        String postStart,
        String preStop,
        String readiness,
        String liveness,
        String updateStrategy) {
        EDASK8sDeploySetup edask8sSetup =
            new EDASK8sDeploySetup(namespace, credentialsString, appId, targetObject, image);
        edask8sSetup.setEdasContainerVersion(edasContainerVersion);
        edask8sSetup.setWebContainer(webContainer);
        edask8sSetup.setJdk(jdk);
        edask8sSetup.setVersionLabelFormat(versionLabelFormat);
        edask8sSetup.setEnvs(envs);
        edask8sSetup.setStartupCommand(startupCommand);
        edask8sSetup.setArgs(args);
        edask8sSetup.setCpuLimit(cpuLimit);
        edask8sSetup.setMemoryLimit(memoryLimit);
        edask8sSetup.setCpuRequest(cpuRequest);
        edask8sSetup.setMemoryRequest(memoryRequest);
        edask8sSetup.setReplicas(replicas);
        edask8sSetup.setPostStart(postStart);
        edask8sSetup.setPreStop(preStop);
        edask8sSetup.setReadiness(readiness);
        edask8sSetup.setLiveness(liveness);
        edask8sSetup.setUpdateStrategy(updateStrategy);
        extensions.add(edask8sSetup);
    }

    void insertK8sApplication(
        String namespace,
        String credentialsString,
        String applicationName,
        String packageType,
        String clusterId,
        String targetObject,
        String k8sNamespace,
        String jdk) {
        EDASK8sInsertSetup edask8sInsertSetup =
            new EDASK8sInsertSetup(credentialsString, namespace, clusterId, k8sNamespace, applicationName, targetObject, packageType, jdk);
        extensions.add(edask8sInsertSetup);
    }

    void insertK8sApplication(
        String namespace,
        String credentialsString,
        String applicationName,
        String packageType,
        String clusterId,
        String targetObject,
        String k8sNamespace,
        String jdk,
        String descFormat,
        String edasContainerVersion,
        String webContainer,
        String versionLabelFormat,
        String envs,
        String startupCommand,
        String args,
        String cpuLimit,
        String memoryLimit,
        String cpuRequest,
        String memoryRequest,
        String replicas,
        String postStart,
        String preStop,
        String readiness,
        String liveness) {
        EDASK8sInsertSetup edask8sInsertSetup =
            new EDASK8sInsertSetup(credentialsString, namespace, clusterId, k8sNamespace, applicationName, targetObject, packageType, jdk);
        edask8sInsertSetup.setDescFormat(descFormat);
        edask8sInsertSetup.setEdasContainerVersion(edasContainerVersion);
        edask8sInsertSetup.setWebContainer(webContainer);
        edask8sInsertSetup.setVersionLabelFormat(versionLabelFormat);
        edask8sInsertSetup.setEnvs(envs);
        edask8sInsertSetup.setStartupCommand(startupCommand);
        edask8sInsertSetup.setArgs(args);
        edask8sInsertSetup.setCpuLimit(cpuLimit);
        edask8sInsertSetup.setMemoryLimit(memoryLimit);
        edask8sInsertSetup.setCpuRequest(cpuRequest);
        edask8sInsertSetup.setMemoryRequest(memoryRequest);
        edask8sInsertSetup.setReplicas(replicas);
        edask8sInsertSetup.setPostStart(postStart);
        edask8sInsertSetup.setPreStop(preStop);
        edask8sInsertSetup.setReadiness(readiness);
        edask8sInsertSetup.setLiveness(liveness);
        extensions.add(edask8sInsertSetup);
    }

}
