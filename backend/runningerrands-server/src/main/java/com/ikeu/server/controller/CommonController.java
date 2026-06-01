package com.ikeu.server.controller;

import com.ikeu.common.constant.MessageConstant;
import com.ikeu.common.result.Result;
import com.ikeu.common.utils.AliOssUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

/**
 * 通用接口，提供文件上传和获取默认头像等功能。
 * @author ikeu
 * @since 2025/05/21
 */
@Slf4j
@RestController
@Tag(name = "通用接口", description = "文件上传、默认头像等通用接口")
@RequiredArgsConstructor
public class CommonController {
    private final AliOssUtil aliOssUtil;

    /**
     * 文件上传至阿里云OSS。
     *
     * <p>接收 MultipartFile，提取原始文件名后缀，使用 UUID 生成本地唯一文件名，
     * 调用 {@link AliOssUtil#upload} 将文件字节流上传至 OSS，返回文件的公网访问URL。
     * 文件为空或上传过程中发生 IO 异常时返回错误消息。
     *
     * @param file 上传的文件
     * @return 文件访问URL
     */
    @PostMapping("/common/upload")
    @Operation(summary = "文件上传接口")
    public Result<String> upload(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Result.error(MessageConstant.FILE_EMPTY);
        }
        log.info("文件上传：{}", file.getOriginalFilename());

        String originalFileName = file.getOriginalFilename();
        String suffix = null;
        if (originalFileName != null) {
            suffix = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        String fileName = UUID.randomUUID() + suffix;

        try {
            String filePath = aliOssUtil.upload(file.getBytes(), fileName);
            return Result.successData(filePath);
        } catch (IOException e) {
            log.error("文件上传失败：{}", e.getMessage());
        }

        return Result.error(MessageConstant.UPLOAD_FAILED);
    }

    /**
     * 获取默认头像图片资源。
     *
     * <p>从 classpath 下的 {@code static/imgs/default_avatar.jpg} 加载默认头像文件，
     * 以 JPEG 媒体类型返回 ResponseEntity。文件不存在时返回 404。
     *
     * @return 默认头像图片响应实体
     */
    @GetMapping("/imgs/default_avatar.jpg")
    @Operation(summary = "获取默认头像")
    public ResponseEntity<Resource> defaultAvatar() throws IOException {
        ClassPathResource resource = new ClassPathResource("static/imgs/default_avatar.jpg");
        if (!resource.exists()) {
            log.error("默认头像文件不存在: static/imgs/default_avatar.jpg");
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(resource);
    }
}
