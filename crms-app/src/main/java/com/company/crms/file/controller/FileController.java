package com.company.crms.file.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.company.crms.common.exception.BizException;
import com.company.crms.common.exception.ErrorCode;
import com.company.crms.file.entity.FileObject;
import com.company.crms.file.mapper.FileObjectMapper;
import com.company.crms.file.service.FileStorage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Tag(name = "文件下载")
@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final FileObjectMapper fileObjectMapper;
    private final FileStorage storage;

    @Operation(summary = "下载/预览文件")
    @SaCheckLogin
    @GetMapping("/{id}/preview")
    public ResponseEntity<InputStreamResource> preview(@PathVariable Long id) {
        FileObject fo = fileObjectMapper.selectById(id);
        if (fo == null) {
            throw new BizException(ErrorCode.FILE_NOT_FOUND);
        }
        InputStream in;
        try {
            in = storage.load(fo.getObjectKey());
        } catch (IOException e) {
            log.warn("load file failed: {}", e.getMessage());
            throw new BizException(ErrorCode.FILE_NOT_FOUND);
        }
        String filename = fo.getFileName() == null ? "file" : fo.getFileName();
        String enc = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION,
                "inline; filename*=UTF-8''" + enc);
        MediaType type = fo.getContentType() == null
                ? MediaType.APPLICATION_OCTET_STREAM
                : MediaType.parseMediaType(fo.getContentType());
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(type)
                .contentLength(fo.getSize() == null ? -1 : fo.getSize())
                .body(new InputStreamResource(in));
    }
}
