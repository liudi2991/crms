# MyBatis-Plus Generator 使用说明

## 适用范围

适用于本仓库 `db/migration/V1.0.0__init.sql` 中已经存在的 20 张表，一次性产出 Entity、Mapper、Service、ServiceImpl、Controller。

> 仅生成 **CRUD 闭环骨架**。复杂业务逻辑（如核销、合并、状态机）**不要**通过 Generator 产出，统一由 G4 阶段 AI 辅助实现。

## 前置条件

1. 数据库已通过 Flyway 迁移完成（启动一次 `crms-app` 即可）；
2. `pom.xml` 中已包含 `mybatis-plus-generator` 与 Freemarker 依赖；
3. 环境变量（可选）：

   | 变量 | 默认 |
   | --- | --- |
   | `GEN_DB_URL` | `jdbc:mysql://localhost:3306/crms?...` |
   | `GEN_DB_USER` | `root` |
   | `GEN_DB_PASSWORD` | `root` |

## 运行

```bash
cd crms-app

# 单模块
mvn -DskipTests test-compile exec:java \
  -Dexec.mainClass="com.company.crms.scripts.GenerateCode" \
  -Dexec.classpathScope=test \
  -Dexec.args="customer"

# 全部模块
mvn -DskipTests test-compile exec:java \
  -Dexec.mainClass="com.company.crms.scripts.GenerateCode" \
  -Dexec.classpathScope=test \
  -Dexec.args="all"
```

## 产出

```
src/main/java/com/company/crms/{module}/
├── entity/        # Lombok @Data + 继承 BaseEntity
├── mapper/        # @Mapper + BaseMapper<T>
├── service/       # 接口 + impl/
└── controller/    # @RestController + @RequestMapping("/api/v1/{module}/{path}")

src/main/resources/mapper/{module}/        # XML（即使空也保留）
```

## 生成后必做

1. 检查 Entity 是否正确继承 `BaseEntity`；
2. 给 **敏感字段**（phone/email/voucher_no）追加 `@SensitiveField` 注解；
3. 给 **审计场景** 的 Service 方法加 `@OperationLog`；
4. 给 **数据范围** 受限的查询 Mapper 方法名加后缀 `WithDataScope`，触发 `DataScopeInterceptor`；
5. 跑 `mvn -pl crms-app test` 确认编译通过。

## 后续维护

- 新增表：只在 Flyway 加新版本号脚本，**不要再次** 跑 Generator 覆盖已生成代码；
- 非破坏性修改：手动给 Entity 加字段；
- 字段重命名/删除：写 `db/migration/Vx.y.z__{change}.sql`，并手工同步 Entity。
