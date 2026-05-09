package com.company.crms.file.service.impl;

import com.company.crms.common.config.MinioProperties;
import com.company.crms.common.exception.BizException;
import com.company.crms.common.exception.ErrorCode;
import com.company.crms.file.entity.FileObject;
import com.company.crms.file.mapper.FileObjectMapper;
import com.company.crms.file.service.FileStorage;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.errors.MinioException;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * MinIO/S3 兼容存储实现（DSS §3.3.3）。
 *
 * <p>启用方式：{@code crms.storage.type=minio}，并在 {@code application.yml} 配置好
 * {@code crms.storage.minio.{endpoint,access-key,secret-key,bucket}}。
 *
 * <p>对象键规则：{@code <bizType>/<yyyy-MM-dd>/<uuid>.<ext>}，与 {@link LocalFileStorage} 保持一致。
 *
 * <p>{@link #previewUrl(Long, int)} 返回 MinIO 预签名 GET URL，浏览器可直接访问，
 * 无需经过应用服务器中转，便于多实例部署与 CDN。
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "crms.storage", name = "type", havingValue = "minio")
public class MinioFileStorage implements FileStorage {

    /** 服务端内部用：put/get/delete 走 docker 网络 host（如 http://minio:9000）。 */
    private final MinioClient client;
    /** 仅生成预签名 URL 用：host 必须是浏览器可达的对外地址。 */
    private final MinioClient presignClient;
    private final MinioProperties props;
    private final FileObjectMapper fileObjectMapper;

    public MinioFileStorage(MinioClient client,
                            @Qualifier("minioPresignClient") MinioClient presignClient,
                            MinioProperties props,
                            FileObjectMapper fileObjectMapper) {
        this.client = client;
        this.presignClient = presignClient;
        this.props = props;
        this.fileObjectMapper = fileObjectMapper;
    }

    @PostConstruct
    public void ensureBucket() {
        try {
            boolean exists = client.bucketExists(BucketExistsArgs.builder().bucket(props.getBucket()).build());
            if (!exists) {
                client.makeBucket(MakeBucketArgs.builder().bucket(props.getBucket()).build());
                log.info("MinioFileStorage: bucket {} created", props.getBucket());
            } else {
                log.info("MinioFileStorage: bucket {} ready", props.getBucket());
            }
        } catch (MinioException | IOException | GeneralSecurityException e) {
            // 启动期不抛出，避免拖垮整个应用；运行时再抛业务错误
            log.error("MinioFileStorage init failed: {}", e.getMessage(), e);
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
        try (InputStream in = file.getInputStream()) {
            client.putObject(PutObjectArgs.builder()
                    .bucket(props.getBucket())
                    .object(objectKey)
                    .stream(in, file.getSize(), -1)
                    .contentType(file.getContentType() == null
                            ? "application/octet-stream" : file.getContentType())
                    .build());
        } catch (MinioException | GeneralSecurityException e) {
            throw new IOException("MinIO putObject failed: " + e.getMessage(), e);
        }
        return objectKey;
    }

    @Override
    public InputStream load(String objectKey) throws IOException {
        try {
            return client.getObject(GetObjectArgs.builder()
                    .bucket(props.getBucket())
                    .object(objectKey)
                    .build());
        } catch (MinioException | GeneralSecurityException e) {
            throw new IOException("MinIO getObject failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
            client.removeObject(RemoveObjectArgs.builder()
                    .bucket(props.getBucket())
                    .object(objectKey)
                    .build());
        } catch (Exception e) {
            log.warn("MinIO removeObject failed: {} -> {}", objectKey, e.getMessage());
        }
    }

    /**
     * 生成预签名 GET URL（默认 10 分钟，可由调用方调整）。
     *
     * <p>调用方传入 {@code fileObjectId}，本实现到 file_object 表查 objectKey。
     */
    @Override
    public String previewUrl(Long fileObjectId, int expireSeconds) {
        FileObject fo = fileObjectMapper.selectById(fileObjectId);
        if (fo == null) {
            throw new BizException(ErrorCode.FILE_NOT_FOUND);
        }
        int expiry = expireSeconds <= 0 ? 600 : expireSeconds;
        try {
            // 必须用 presignClient（公网 endpoint），浏览器才能解析到 host
            return presignClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(props.getBucket())
                    .object(fo.getObjectKey())
                    .expiry(expiry, TimeUnit.SECONDS)
                    .build());
        } catch (Exception e) {
            log.warn("MinIO presignedUrl failed: {}", e.getMessage());
            // 回退到应用层下载路径，至少不影响业务
            return "/api/v1/files/" + fileObjectId + "/preview";
        }
    }

    @Override
    public String bucket() {
        return props.getBucket();
    }
}
