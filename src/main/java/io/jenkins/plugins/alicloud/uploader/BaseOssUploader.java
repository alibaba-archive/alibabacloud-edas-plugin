package io.jenkins.plugins.alicloud.uploader;

import com.aliyun.oss.ClientConfiguration;
import com.aliyun.oss.OSSClient;
import com.aliyun.oss.model.AbortMultipartUploadRequest;
import com.aliyun.oss.model.CompleteMultipartUploadRequest;
import com.aliyun.oss.model.InitiateMultipartUploadRequest;
import com.aliyun.oss.model.InitiateMultipartUploadResult;
import com.aliyun.oss.model.PartETag;
import com.aliyun.oss.model.PutObjectRequest;
import com.aliyun.oss.model.UploadPartRequest;
import com.aliyun.oss.model.UploadPartResult;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.edas.model.v20170801.QueryRegionConfigResponse;
import io.jenkins.plugins.alicloud.edas.EDASService;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class BaseOssUploader implements Uploader {
    private static final String OSS_ENDPOINT_PTN = "http://oss-%s.aliyuncs.com";
    private static final String OSS_DOWNLOAD_URL_PTN = "https://%s.oss-%s-internal.aliyuncs.com/%s";
    private static final long MULTIPART_THRESHOLD = 100 * 1024 * 1024L;
    private static final int MULTIPART_UPLOAD_COUNT = 5;
    private static final Logger logger = Logger.getLogger(BaseOssUploader.class.getName());

    protected String doUpload(
            String edasEndpoint, String regionId, String bucket,
            String key, File file, String ak, String sk, String token, DefaultAcsClient client) throws Exception {
        String ossEndpoint = String.format(OSS_ENDPOINT_PTN, regionId);
        String downloadUrl = String.format(OSS_DOWNLOAD_URL_PTN, bucket, regionId,
                URLEncoder.encode(key, "UTF-8").replaceAll("%2F", "/"));
        if (!edasEndpoint.endsWith("aliyuncs.com") ) {
            QueryRegionConfigResponse.RegionConfig regionConfig = EDASService.queryRegionConfigResponse(client);
            if (regionConfig == null) {
                logger.info(
                    String.format("User set endpoint: %s, but region config pop api return non-200 ",
                        edasEndpoint));
            } else {
                if ("oss".equalsIgnoreCase(regionConfig.getFileServerType())
                    && regionConfig.getFileServerConfig() != null) {
                    String internalHost = regionConfig.getFileServerConfig().getInternalUrl();
                    String publicHost = regionConfig.getFileServerConfig().getPublicUrl();
                    if (publicHost != null && internalHost != null) {
                        ossEndpoint = "http://" + publicHost;
                        downloadUrl = "http://" + bucket + "." + internalHost + "/"
                            + URLEncoder.encode(key, "UTF-8").replaceAll("%2F", "/");
                        logger.info("Use oss endpoint: " + ossEndpoint);
                        logger.info("Use oss downloadUrl: " + downloadUrl);
                    } else {
                        logger.info("InternalUrl in region config: " + internalHost);
                        logger.info("PublicUrl in region config: " + publicHost);
                    }
                } else {
                    logger.info("Fs type: " + regionConfig.getFileServerType());
                    logger.info("Fs server config: " + regionConfig.getFileServerConfig());
                }
            }
        }
        ClientConfiguration configuration = new ClientConfiguration();
        configuration.setSupportCname(false);
        OSSClient ossClient = (token == null) ? new OSSClient(ossEndpoint, ak, sk, configuration) : new OSSClient(ossEndpoint, ak,
                sk, token, configuration);
        upload(file, ossClient, bucket, key);
        ossClient.shutdown();

        file.delete();
        return downloadUrl;
    }

    private void upload(File file, OSSClient ossClient, String bucket, String key) throws Exception {
        if (file.length() < MULTIPART_THRESHOLD) {
            ossClient.putObject(new PutObjectRequest(bucket, key, new FileInputStream(file)));
        } else {
            logger.log(Level.INFO,"using multi part uploading " + file.getName());
            multipartUpload(bucket, key, file, ossClient);
        }
    }

    private void multipartUpload(String bucketName, String objectName, File file, OSSClient ossClient) throws Exception {
        String uploadId = getMultipartUploadId(bucketName, objectName, ossClient);
        try {
            multiUploadInner(uploadId, bucketName, objectName, file, ossClient);
        } catch (Exception e) {
            // 分片上传失败后，取消分片上传
            try {
                AbortMultipartUploadRequest abortMultipartUploadRequest =
                    new AbortMultipartUploadRequest(bucketName, objectName, uploadId);
                ossClient.abortMultipartUpload(abortMultipartUploadRequest);
            } catch (Exception ex) {
                logger.log(Level.SEVERE,ex.getMessage());
            }
            throw e;
        }
    }

    private String getMultipartUploadId(String bucketName, String objectName, OSSClient ossClient) {
        InitiateMultipartUploadRequest request = new InitiateMultipartUploadRequest(bucketName, objectName);
        InitiateMultipartUploadResult result = ossClient.initiateMultipartUpload(request);
        return result.getUploadId();
    }

    private void multiUploadInner(String uploadId, String bucketName, String objectName, File file, final OSSClient ossClient)
        throws InterruptedException {
        final long fileLength = file.length();
        long uploadSize = fileLength / MULTIPART_UPLOAD_COUNT;
        List<PartETag> tags = new ArrayList<>();
        ExecutorService threadPool = Executors.newFixedThreadPool(MULTIPART_UPLOAD_COUNT);
        for (int i = 0; i < MULTIPART_UPLOAD_COUNT; i++) {
            int finali = i;
            threadPool.execute(() -> {
                try {
                    long startPos = finali * uploadSize;
                    long curPartSize = (finali + 1 == MULTIPART_UPLOAD_COUNT) ? (fileLength - startPos) : uploadSize;
                    InputStream instream = new FileInputStream(file);
                    instream.skip(startPos);
                    UploadPartRequest uploadPartRequest = new UploadPartRequest();
                    uploadPartRequest.setBucketName(bucketName);
                    uploadPartRequest.setKey(objectName);
                    uploadPartRequest.setUploadId(uploadId);
                    uploadPartRequest.setInputStream(instream);
                    uploadPartRequest.setPartSize(curPartSize);
                    uploadPartRequest.setPartNumber(finali + 1);
                    UploadPartResult uploadPartResult = ossClient.uploadPart(uploadPartRequest);
                    tags.add(uploadPartResult.getPartETag());
                } catch (IOException e) {
                    logger.log(Level.SEVERE, e.getMessage());
                    throw new RuntimeException(e);
                }
            });
        }
        threadPool.shutdown();
        threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
        if (tags.size() != MULTIPART_UPLOAD_COUNT) {
            throw new IllegalStateException("wrong tags count " + tags.size());
        }
        tags.sort(Comparator.comparingInt(PartETag::getPartNumber));
        CompleteMultipartUploadRequest completeMultipartUploadRequest =
            new CompleteMultipartUploadRequest(bucketName, objectName, uploadId, tags);
        ossClient.completeMultipartUpload(completeMultipartUploadRequest);
        System.out.print("\n");
    }
}