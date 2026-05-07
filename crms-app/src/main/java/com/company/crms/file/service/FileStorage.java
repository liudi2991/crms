package com.company.crms.file.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * 文件存储抽象。当前内置 LocalFileStorage 实现；生产可替换为 MinIO。
 */
public interface FileStorage {

    /** 上传文件。返回 objectKey。 */
    String save(String bizType, MultipartFile file) throws java.io.IOException;

    /** 读取文件流（调用方负责关闭）。 */
    InputStream load(String objectKey) throws java.io.IOException;

    /** 删除文件。 */
    void delete(String objectKey);

    /** 生成预览/下载 URL（local 实现返回 /api/v1/files/{id}/preview）。 */
    String previewUrl(Long fileObjectId, int expireSeconds);

    /** 桶 / 根目录名称，写入 file_object.bucket 字段。 */
    String bucket();
}
