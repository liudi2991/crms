# 示例：客户 CRUD（后端）

## 输入

### 系统消息
[crms/prompts/system-backend.md] 完整加载。

### 用户消息

```markdown
## 上下文

- **任务**：I1-CU-001/002 客户 CRUD（基础）
- **关联 SRS**：UC-CU-01 ~ UC-CU-04
- **关联 DSS**：§3.2 客户管理
- **数据库表**：
  ```sql
  CREATE TABLE customer (
      id BIGINT NOT NULL,
      code VARCHAR(32) NOT NULL,
      name VARCHAR(100) NOT NULL,
      type VARCHAR(16) NOT NULL,
      uscc VARCHAR(32),
      level VARCHAR(4) NOT NULL DEFAULT 'C',
      owner_id BIGINT NOT NULL,
      dept_id BIGINT NOT NULL,
      status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
      ... (见 V1.0.0)
  );
  ```
- **依赖**：`Result/PageResult/BizException/ErrorCode/BaseEntity/CodeGenerator/UserContextHolder/DataScopeInterceptor` 已就绪。

## 要求

1. 实现 GET /api/v1/customers（分页 + 关键词 + 状态过滤，受数据范围限制）；
2. 实现 POST /api/v1/customers（自动生成 code，唯一性校验 uscc）；
3. 实现 PUT /api/v1/customers/{id}（owner_id 默认不变；超管可改）；
4. 实现 DELETE /api/v1/customers/{id}（软删除，需检查无激活合同）；
5. 单元测试覆盖率 ≥ 90%。
```

## 期望输出（节选）

### 设计要点

1. 列表查询走 `selectPageWithDataScope`，由 `DataScopeInterceptor` 自动追加范围；
2. 编号 `CU-yyyyMM-NNNN` 由 `CodeGenerator` 在 Service 层生成；
3. 唯一性校验 USCC：用 unique key + 抓 `DuplicateKeyException` 转 `ErrorCode.CU_USCC_DUPLICATE`，避免 `select then insert` 的竞态；
4. 软删除前 `count(contract where customer_id = ? and status not in ('TERMINATED','EXPIRED'))` ≥ 1 即拒绝；
5. 操作日志：create/update/delete 三处加 `@OperationLog`。

### 文件清单

| 文件 | 用途 |
| --- | --- |
| `customer/entity/Customer.java` | 实体（替换 Generator 产物） |
| `customer/dto/CustomerQuery.java` | 列表查询 DTO |
| `customer/dto/CreateCustomerDTO.java` | 新建 DTO |
| `customer/dto/UpdateCustomerDTO.java` | 修改 DTO |
| `customer/vo/CustomerVO.java` | 列表 VO |
| `customer/vo/CustomerDetailVO.java` | 详情 VO |
| `customer/service/CustomerService.java` | 接口 |
| `customer/service/impl/CustomerServiceImpl.java` | 实现 |
| `customer/mapper/CustomerMapper.java` | Mapper（追加 count 方法） |
| `customer/controller/CustomerController.java` | Controller |
| `test/.../CustomerServiceImplTest.java` | 单测 |

### 代码

（按文件清单输出 fenced code block，每个文件代码块在头部以注释写完整路径）
