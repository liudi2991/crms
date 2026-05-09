package com.company.crms.common.config;

import io.minio.MinioClient;
import lombok.Data;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * MinIO 客户端配置（DSS §3.3.3 附件存储）。
 *
 * <p>容器化部署里 {@code endpoint} 是容器内 DNS（如 {@code http://minio:9000}），
 * 浏览器解析不到。所以又引入 {@code publicEndpoint} 专门用于生成预签名下载 URL，
 * 它必须是浏览器可达的公网/内网地址（如 {@code http://124.221.19.234:9000}）。
 *
 * <p>预签名 URL 的 host 是签名计算的一部分，**不能事后字符串替换**，
 * 必须用一个独立的 MinioClient 实例（{@link #minioPresignClient(MinioProperties)}）
 * 在生成时就用 publicEndpoint。
 */
@Configuration
@ConfigurationProperties(prefix = "crms.storage.minio")
@Data
public class MinioProperties {
    /** 容器/服务端互通的内部 endpoint，用于上传/下载流式读取。 */
    private String endpoint;
    /** 浏览器可达的对外 endpoint，用于生成预签名 URL。空时回退到 endpoint。 */
    private String publicEndpoint;
    private String accessKey;
    private String secretKey;
    private String bucket;

    /** 默认客户端：服务端内部 put/get 用，走容器网络。 */
    @Bean
    @Primary
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }

    /** 仅生成预签名 URL 用，host 必须是浏览器能解析的对外地址。 */
    @Bean(name = "minioPresignClient")
    public MinioClient minioPresignClient(MinioProperties self) {
        String publicEp = (publicEndpoint == null || publicEndpoint.isBlank())
                ? endpoint : publicEndpoint;
        return MinioClient.builder()
                .endpoint(publicEp)
                .credentials(accessKey, secretKey)
                .build();
    }
}
