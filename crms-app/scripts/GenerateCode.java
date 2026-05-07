package com.company.crms.scripts;

import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MyBatis-Plus 代码生成器（一次性脚本）。
 *
 * <p>用法：
 * <pre>
 *   mvn -pl crms-app exec:java -Dexec.mainClass="com.company.crms.scripts.GenerateCode" \
 *       -Dexec.args="iam"
 * </pre>
 *
 * <p>支持 module 参数：iam / customer / contract / payment / notification / system / all
 *
 * <p>生成原则：
 * <ul>
 *   <li>Entity 继承 {@code BaseEntity}（id/createdAt/.../version 自动复用）</li>
 *   <li>软删除字段 {@code is_deleted} 由 {@code @TableLogic}</li>
 *   <li>统一返回 {@code Result<T>}</li>
 *   <li>Controller 路径 {@code /api/v1/{module}/{entity}}</li>
 * </ul>
 */
public class GenerateCode {

    /** 模块前缀 → 模块包名映射。 */
    private static final Map<String, ModuleSpec> MODULES = new HashMap<>();
    static {
        MODULES.put("iam", new ModuleSpec("iam", List.of(
            "iam_department", "iam_user", "iam_role", "iam_permission",
            "iam_user_role", "iam_role_permission")));
        MODULES.put("customer", new ModuleSpec("customer", List.of(
            "customer", "customer_contact")));
        MODULES.put("contract", new ModuleSpec("contract", List.of(
            "contract", "contract_attachment", "contract_note")));
        MODULES.put("payment", new ModuleSpec("payment", List.of(
            "payment_plan", "payment_record", "payment_settlement")));
        MODULES.put("notification", new ModuleSpec("notification", List.of(
            "notification", "notification_setting")));
        MODULES.put("system", new ModuleSpec("system", List.of(
            "change_log", "operation_log", "hard_delete_log", "system_param", "file_object")));
    }

    public static void main(String[] args) {
        String moduleKey = args.length > 0 ? args[0] : "all";
        String url = System.getenv().getOrDefault(
            "GEN_DB_URL",
            "jdbc:mysql://localhost:3306/crms?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true");
        String user = System.getenv().getOrDefault("GEN_DB_USER", "root");
        String pwd  = System.getenv().getOrDefault("GEN_DB_PASSWORD", "root");
        String outputBase = System.getProperty("user.dir") + "/src/main/java";
        String mapperXmlBase = System.getProperty("user.dir") + "/src/main/resources/mapper";

        if ("all".equals(moduleKey)) {
            MODULES.forEach((k, v) -> generate(v, url, user, pwd, outputBase, mapperXmlBase));
        } else if (MODULES.containsKey(moduleKey)) {
            generate(MODULES.get(moduleKey), url, user, pwd, outputBase, mapperXmlBase);
        } else {
            System.err.println("Unknown module: " + moduleKey
                    + ", expect one of: iam|customer|contract|payment|notification|system|all");
            System.exit(1);
        }
    }

    private static void generate(ModuleSpec spec, String url, String user, String pwd,
                                 String outputBase, String mapperXmlBase) {
        FastAutoGenerator.create(url, user, pwd)
                .globalConfig(b -> b
                        .author("crms-codegen")
                        .commentDate("yyyy-MM-dd")
                        .outputDir(outputBase)
                        .disableOpenDir())
                .packageConfig(b -> b
                        .parent("com.company.crms")
                        .moduleName(spec.module)
                        .entity("entity")
                        .mapper("mapper")
                        .service("service")
                        .serviceImpl("service.impl")
                        .controller("controller")
                        .pathInfo(Collections.singletonMap(OutputFile.xml, mapperXmlBase + "/" + spec.module)))
                .strategyConfig(b -> b
                        .addInclude(spec.tables)
                        .entityBuilder()
                            .enableLombok()
                            .enableTableFieldAnnotation()
                            .superClass("com.company.crms.common.base.BaseEntity")
                            .addSuperEntityColumns("id", "created_by", "created_at", "updated_by",
                                                   "updated_at", "is_deleted", "version")
                            .logicDeleteColumnName("is_deleted")
                        .mapperBuilder()
                            .enableMapperAnnotation()
                            .enableBaseResultMap()
                            .enableBaseColumnList()
                        .serviceBuilder()
                            .formatServiceFileName("%sService")
                            .formatServiceImplFileName("%sServiceImpl")
                        .controllerBuilder()
                            .enableRestStyle()
                            .formatFileName("%sController"))
                .templateEngine(new FreemarkerTemplateEngine())
                .execute();
        System.out.println("[GEN] module=" + spec.module + " tables=" + spec.tables);
    }

    private record ModuleSpec(String module, List<String> tables) {
    }
}
