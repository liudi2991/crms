package com.company.crms.common.config;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 全局 Jackson 配置。
 *
 * <p>把所有 {@link Long} / {@code long} 字段序列化为 JSON 字符串。
 *
 * <p>原因：系统的主键使用 Snowflake 算法（19 位 64bit Long），
 * 而 JavaScript 的 Number 只能安全表示到 2^53 - 1（≈ 9×10^15）。
 * 直接序列化为 JSON number 会在前端被解析成最近的可表示双精度浮点数，
 * 出现末几位被改写的现象（例如 ID {@code 177617934154203136}
 * 被改写为 {@code 177617934154203140}），
 * 导致后续 PUT / DELETE 等操作命中不到正确记录而报"客户不存在"等错误。
 *
 * <p>本项目所有 Long 字段语义都是 ID（owner_id / dept_id / customer_id ...），
 * 金额使用 BigDecimal、计数使用 int，因此整体序列化为 String 是安全的。
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer longToStringCustomizer() {
        SimpleModule module = new SimpleModule("LongToString");
        module.addSerializer(Long.class, ToStringSerializer.instance);
        module.addSerializer(Long.TYPE, ToStringSerializer.instance);
        return builder -> builder.modulesToInstall(module);
    }
}
