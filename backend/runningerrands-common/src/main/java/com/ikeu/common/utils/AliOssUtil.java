package com.ikeu.common.utils;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;

/**
 * 阿里云OSS工具类
 */
@Data
@AllArgsConstructor
@Slf4j
public class AliOssUtil {

    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String bucketName;

    /**
     * 文件上传
     *
     * @param bytes
     * @param objectName
     * @return
     */
    public String upload(byte[] bytes, String objectName) {

        // 创建OSSClient实例。
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

        try {
            // 创建PutObject请求。
            ossClient.putObject(bucketName, objectName, new ByteArrayInputStream(bytes));
        } catch (OSSException oe) {
            log.error("OSS上传失败: ErrorCode={}, ErrorMessage={}, RequestId={}",
                    oe.getErrorCode(), oe.getErrorMessage(), oe.getRequestId());
            throw new RuntimeException("文件上传到OSS失败: " + oe.getErrorMessage(), oe);
        } catch (ClientException ce) {
            log.error("OSS客户端异常: {}", ce.getMessage());
            throw new RuntimeException("文件上传客户端异常: " + ce.getMessage(), ce);
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }

        String filePath = "https://" + bucketName + "." + endpoint + "/" + objectName;
        log.info("文件上传到:{}", filePath);
        return filePath;
    }
}
