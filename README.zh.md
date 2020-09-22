# Alibabacloud EDAS Publisher plugin
该插件用来部署应用到阿里云 [EDAS](https://www.aliyun.com/product/edas?spm=5176.12825654.h2v3icoap.479.e9392c4afgWdXy)上。

# 使用
使用前需要先配置全局证书

## 配置全局证书
在系统配置页面设置阿里云的证书

![Global Config](images/globalConfig.png)

配置完成后可以使用`Ping EDAS` 检测配置结果。

## 自由风格job使用
在`Post-build Actions`区域选择`Deploy to EDAS`，根据具体的部署场景选择`Add`下的子选框；

![deploy_type](images/deployType.png) 

| Name  |  Description |
| :-----|:----------|
|EDAS ECS Application| 部署到 EDAS ECS 应用上 |
|Create EDAS ECS Application| 创建 EDAS ECS 应用并部署 |
|EDAS K8s Application| 部署到 EDAS K8s 应用上 |
|Create EDAS K8s Application| 创建 EDAS K8s 应用并部署 |

## 流水线使用
![pipeline](images/pipeline.png)
可以使用片段生成器辅助生成流水线脚本。
![snippet_generator](images/snippet_generator.png)

## Job-dsl 使用
```
job('edas') {    
  publishers { 
    edasClient { 
      deployApplication('', '', '', '', '') 
    }
  }
}
```
支持的方法如下：
```
void deployApplication(
        String namespace,
        String credentialsString,
        String group,
        String appId,
        String targetObject)；

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
        String releaseType)；

void insertApplication(
        String namespace,
        String credentialsString,
        String applicationName,
        String packageType,
        String clusterId,
        String targetObject,
        String ecuInfo)；

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
        String healthCheckUrl)；

void deployK8sApplication(
        String namespace,
        String credentialsString,
        String appId,
        String targetObject,
        Boolean image)；

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
        String updateStrategy)；

void insertK8sApplication(
        String namespace,
        String credentialsString,
        String applicationName,
        String packageType,
        String clusterId,
        String targetObject,
        String k8sNamespace,
        String jdk)；

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
        String liveness)；
```
