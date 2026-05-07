# CRMS 后端 AI 辅助代码生成约定（system-backend.md）

> 本文件是所有后端代码生成提示词的"系统提示"。在 Cursor / Claude / GPT 中作为对话首条系统消息加载。

## 1. 技术栈与版本

- Java 17（LTS）；
- Spring Boot 3.2.x；
- MyBatis-Plus 3.5.6 + MySQL 8.0 + Flyway；
- Sa-Token 1.38.0；
- Lombok + MapStruct；
- Hutool 工具集；
- 测试：JUnit 5 + Mockito + Testcontainers。

## 2. 包结构（必须遵守）

```
com.company.crms
├── common/                                # 横切关注点
│   ├── annotation/  @OperationLog @SensitiveField
│   ├── aop/         OperationLogAspect
│   ├── base/        BaseEntity
│   ├── config/      MybatisPlusConfig SaTokenConfig WebMvcConfig OpenApiConfig
│   ├── constant/    Constants
│   ├── enums/       DataScope
│   ├── exception/   BizException ErrorCode GlobalExceptionHandler
│   ├── interceptor/ DataScopeInterceptor TraceIdFilter UserContextInterceptor
│   ├── response/    Result PageResult
│   ├── security/    UserContext UserContextHolder
│   └── util/        AesCryptoUtil SnowflakeIdGenerator CodeGenerator MaskUtil
├── iam/             用户/角色/权限/部门
├── customer/        客户档案
├── contract/        合同
├── payment/         回款计划/实际/核销/红冲
├── report/          看板/账龄/TOP
├── notification/    通知
└── system/          字典/参数/操作日志/回收站
```

每个业务模块内部统一：

```
{module}/
├── controller/
├── service/   (interface)
├── service/impl/
├── mapper/    (与 resources/mapper/{module}/*.xml 对应)
├── entity/
├── dto/       (Request DTO，含 @Valid 校验)
└── vo/        (Response VO，按需脱敏)
```

## 3. 强制约束

### 3.1 响应封装

所有 Controller 方法都返回 `Result<T>` 或 `PageResult<T>`：

```java
@GetMapping("/customers")
public Result<PageResult<CustomerVO>> list(@Validated CustomerQuery query) {
    return Result.ok(customerService.page(query));
}
```

### 3.2 异常处理

业务校验失败抛 `BizException(ErrorCode.X)`，**绝不**返回 ResponseEntity 5xx 给业务层；系统级异常由 `GlobalExceptionHandler` 兜底。

### 3.3 实体

所有业务实体继承 `BaseEntity`：

```java
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("customer")
public class Customer extends BaseEntity {
    private String code;
    private String name;
    @SensitiveField(mask = SensitiveField.Mask.PHONE)
    private String contactPhone;
    ...
}
```

### 3.4 数据范围

只要 SQL 涉及"业务用户能不能看到这条记录"：

- Mapper 方法名 **必须** 以 `WithDataScope` 结尾，触发 `DataScopeInterceptor` 自动追加 `dept_id IN (...)` 或 `owner_id = ?`；
- 不需要范围限制的查询（系统参数、字典、操作日志列表）保持普通命名；
- 系统管理员若拥有 ALL 范围则跳过追加。

### 3.5 操作日志

任何 **改动业务数据** 的 Service 方法必须加 `@OperationLog`：

```java
@OperationLog(module = "客户", action = "新建客户", type = "CREATE")
public Long createCustomer(CreateCustomerDTO dto) { ... }
```

硬删除场景设置 `recordParams = false` 并写 `hard_delete_log` 的快照。

### 3.6 加密 / 脱敏

- 持久化敏感字段（电话/邮箱/凭证号）：使用 `AesCryptoUtil`（包装为 MyBatis TypeHandler）；
- 对外 JSON 序列化：通过 `@SensitiveField` + Jackson 序列化器自动脱敏；
- 凭证号一律加密存储，不回显完整值。

### 3.7 校验

请求 DTO 全部用 `jakarta.validation` 注解：

```java
@Data
public class CreateCustomerDTO {
    @NotBlank(message = "客户名称不能为空")
    @Size(max = 100)
    private String name;

    @Pattern(regexp = "^[A-Z0-9]{18}$", message = "统一社会信用代码格式不正确")
    private String uscc;

    @NotNull
    @Pattern(regexp = "ENTERPRISE|GOVERNMENT|INDIVIDUAL")
    private String type;
}
```

Controller 上加 `@Validated`。

### 3.8 OpenAPI 注解

每个 Controller 类与方法都加：

```java
@Tag(name = "客户管理")
@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {

    @Operation(summary = "客户列表查询")
    @GetMapping
    public Result<PageResult<CustomerVO>> list(...) { ... }
}
```

### 3.9 命名

- 接口路径：`/api/v1/{resource}` 复数；
- 子资源：`/api/v1/contracts/{id}/attachments`；
- DTO：`{Verb}{Entity}DTO`；VO：`{Entity}VO`、`{Entity}DetailVO`；
- Service 方法：`page / detail / create / update / softDelete / hardDelete / merge`；
- Mapper 方法：`selectXxxByXxxWithDataScope`、`countOverdueByOwner`。

## 4. 测试

每个 Service 方法至少：

- 1 条正常路径单测；
- ≥ 2 条异常路径单测（参数非法、状态非法、并发冲突）；
- 关键模块（核销 / 合并 / 硬删除）覆盖率必须 ≥ 95%；
- Controller 用 `@WebMvcTest` + `MockMvc`；
- Mapper 用 Testcontainers MySQL 集成测试。

## 5. 永远不要做的事

- ❌ 在 Controller 里直接调 Mapper；
- ❌ 拼字符串 SQL（除非用 jsqlparser）；
- ❌ 在循环里 `for-each` 调 DB；
- ❌ 把 `BizException` 转成 5xx；
- ❌ 在响应里返回未脱敏的电话/邮箱/凭证号；
- ❌ 用 `e.printStackTrace()` 代替 `log.error("...", e)`；
- ❌ 静默吞掉异常（`catch { }`）；
- ❌ 创建已存在的文件（先 grep / read 确认）。

## 6. 输出格式约定

收到任务卡后，先输出：

1. **设计要点 / 关键决策**（3–5 条 markdown）；
2. **新增/修改的文件清单**（路径 + 1 行用途）；
3. **代码块** 一个文件一个 fenced code block，开头注明完整路径。

> 别一次"给所有文件"。如果任务大，先给 `Service` 接口 + `DTO/VO` 让我评审，再继续。
