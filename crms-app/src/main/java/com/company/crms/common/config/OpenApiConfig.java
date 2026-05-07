package com.company.crms.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Springdoc-OpenAPI 配置：暴露 OpenAPI 3 文档与 Swagger UI。
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI crmsOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("CRMS API")
                        .description("合同回款管理系统 OpenAPI 文档")
                        .version("v1.0.0")
                        .contact(new Contact().name("CRMS Dev Team").email("dev@company.com")))
                .components(new Components().addSecuritySchemes(SECURITY_SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("opaque")
                                .description("Sa-Token Authorization Header")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME));
    }
}
