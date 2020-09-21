package io.jenkins.plugins.alicloud.uploader;

import com.aliyuncs.DefaultAcsClient;
import java.io.File;

public interface Uploader {
    String getName();
    String upload(File fileName, DefaultAcsClient client) throws Exception;
}
