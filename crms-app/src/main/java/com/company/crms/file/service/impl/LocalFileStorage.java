package com.company.crms.file.service.impl;

import com.company.crms.file.service.FileStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.UUID;

/**
 * 简易本地文件存储。开发与单实例部署默认启用；生产多实例请切换 MinIO。
 *
 * <p>目录结构：{@code <root>/<bizType>/<yyyy-MM-dd>/<uuid>.<ext>}。
 *
 * <p>启用方式：{@code crms.storage.type=local}（默认；缺省时也生效）。
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "crms.storage", name = "type", havingValue = "local", matchIfMissing = true)
public class LocalFileStorage implements FileStorage {

    private final Path rootPath;
    private final String bucketName;

    public LocalFileStorage(@Value("${crms.storage.local.root:./uploads}") String root,
                            @Value("${crms.storage.local.bucket:crms-local}") String bucket) {
        this.rootPath = Paths.get(root).toAbsolutePath().normalize();
        this.bucketName = bucket;
        try {
            Files.createDirectories(rootPath);
            log.info("LocalFileStorage initialized at {}", rootPath);
        } catch (IOException e) {
            throw new IllegalStateException("无法创建文件存储目录: " + rootPath, e);
        }
    }

    @Override
    public String save(String bizType, MultipartFile file) throws IOException {
        String original = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String ext = "";
        int dot = original.lastIndexOf('.');
        if (dot >= 0) ext = original.substring(dot);
        String day = LocalDate.now().toString();
        String objectKey = String.format("%s/%s/%s%s",
                bizType == null ? "misc" : bizType, day, UUID.randomUUID(), ext);
        Path target = rootPath.resolve(objectKey);
        Files.createDirectories(target.getParent());
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target);
        }
        return objectKey;
    }

    @Override
    public InputStream load(String objectKey) throws IOException {
        Path p = rootPath.resolve(objectKey).normalize();
        if (!p.startsWith(rootPath) || !Files.exists(p)) {
            throw new IOException("文件不存在或非法路径: " + objectKey);
        }
        return Files.newInputStream(p);
    }

    @Override
    public void delete(String objectKey) {
        try {
            Path p = rootPath.resolve(objectKey).normalize();
            if (p.startsWith(rootPath)) Files.deleteIfExists(p);
        } catch (IOException e) {
            log.warn("delete file failed: {} -> {}", objectKey, e.getMessage());
        }
    }

    @Override
    public String previewUrl(Long fileObjectId, int expireSeconds) {
        return "/api/v1/files/" + fileObjectId + "/preview";
    }

    @Override
    public String bucket() {
        return bucketName;
    }
}
