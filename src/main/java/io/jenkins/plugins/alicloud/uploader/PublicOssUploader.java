package io.jenkins.plugins.alicloud.uploader;

import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.edas.model.v20170801.GetPackageStorageCredentialResponse;
import io.jenkins.plugins.alicloud.edas.EDASService;
import java.io.File;
import java.util.UUID;

public class PublicOssUploader extends BaseOssUploader {
    private static final String NAME = "Public OSS Uploader";
    private static final String OSS_KEY_PTN = "%s/apps/%s/%s/%s";

    private String edasEndpoint; //for private cloud
    private String regionId;
    private String appId;

    private String generateKey(File file, String keyPrefix) {
        return String.format(OSS_KEY_PTN, keyPrefix, this.appId, UUID.randomUUID().toString(), file.getName());
    }

    PublicOssUploader(String edasEndpoint, String regionId, String appId) {
        this.edasEndpoint = edasEndpoint;
        this.regionId = regionId;
        this.appId = appId;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String upload(File file, DefaultAcsClient client) throws Exception {
        GetPackageStorageCredentialResponse.Credential credential = EDASService.getUploadCredential(client);
        if (credential == null) {
            throw new Exception(String.format(
                    "Can not get token for uploading, please make sure your ak/sk is correct and network(%s) is ok.",
                    edasEndpoint
            ));
        }
        String bucket = credential.getBucket();
        String key = generateKey(file, credential.getKeyPrefix());
        String ak = credential.getAccessKeyId();
        String sk = credential.getAccessKeySecret();
        String token = credential.getSecurityToken();

        return doUpload(edasEndpoint, this.regionId, bucket, key, file, ak, sk, token, client);
    }
}