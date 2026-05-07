# CRMS 文档索引

按用途快速跳转：

| 读者 | 推荐阅读 |
| --- | --- |
| 业务 / 最终用户 | [用户操作手册](./user-manual.md) · [常见问题](./faq.md) |
| 系统管理员 | [管理员手册](./admin-manual.md) · [用户操作手册](./user-manual.md) · [安全自查清单](./security-checklist.md) |
| 测试 / UAT | [验收指南](./acceptance.md) · [人工验收清单](./acceptance-checklist.md) · [验收报告模板](./acceptance-report-template.md) |
| 研发 | [SRS](./srs.md) · [DSS](./dss.md) · [OpenAPI](./openapi.yml) · [迭代计划](./iteration-plan.md) · [代码自动生成计划](./code-gen-plan.md) · [迭代偏差与修复](./issues/) |
| 运维 / 部署 | [部署快速指南](./deploy-quickstart.md) · [运维手册](./operations.md) · [上线安全自查](./security-checklist.md) · [deploy/SECURITY.md](../deploy/SECURITY.md) |
| 数据库 | [ER 图](../db/erd.md) · `db/schema/` |

自动化验收：`../scripts/acceptance.sh`（用法见 [acceptance.md](./acceptance.md)）。

## 文档全集

```
docs/
├── README.md                      ← 本文件（索引）
├── user-manual.md                 业务用户按钮级手册
├── admin-manual.md                系统管理员手册
├── deploy-quickstart.md           部署快速指南（单机 5 分钟版）
├── operations.md                  运维 / 部署 / 排障
├── faq.md                         常见问题
├── acceptance.md                  自动化验收用法
├── acceptance-checklist.md        人工验收清单
├── acceptance-report-template.md  验收报告模板
├── security-checklist.md          上线前安全自查
├── srs.md                         软件需求规格 V1.1
├── dss.md                         设计规格 V1.1
├── tasks.md                       开发任务拆分
├── iteration-plan.md              迭代计划
├── code-gen-plan.md               代码自动生成计划（三层生成模型 / G0–G6）
├── openapi.yml                    OpenAPI 契约
└── issues/
    ├── UC-03-04-fixes.md          UC-03-04 编辑合同 状态白名单偏差（已记录，待批准）
    ├── UC-03-05-fixes.md          UC-03-05 附件 偏差与修复记录（已修复 V1.0.5/V1.0.6）
    └── UC-04-08-fixes.md          UC-04-08 逾期预警 看板与列表口径不一致（VO 层已修齐）
```
