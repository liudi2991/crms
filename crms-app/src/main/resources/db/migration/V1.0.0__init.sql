-- =====================================================================
-- CRMS 初始化 DDL（基于 DSS V1.1 §4.3）
-- 字符集：utf8mb4 / utf8mb4_0900_ai_ci
-- 引擎：InnoDB
-- 公共字段：id / created_by / created_at / updated_by / updated_at / is_deleted / version
-- =====================================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- 1. 部门
-- ---------------------------------------------------------------------
CREATE TABLE iam_department (
    id          BIGINT      NOT NULL,
    parent_id   BIGINT      NOT NULL DEFAULT 0,
    name        VARCHAR(64) NOT NULL,
    full_path   VARCHAR(255) NOT NULL,
    sort        INT         NOT NULL DEFAULT 0,
    created_by  BIGINT,
    created_at  DATETIME    NOT NULL,
    updated_by  BIGINT,
    updated_at  DATETIME    NOT NULL,
    is_deleted  TINYINT     NOT NULL DEFAULT 0,
    version     INT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_department_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门';

-- ---------------------------------------------------------------------
-- 2. 用户
-- ---------------------------------------------------------------------
CREATE TABLE iam_user (
    id              BIGINT       NOT NULL,
    username        VARCHAR(64)  NOT NULL,
    password_hash   VARCHAR(128) NOT NULL,
    real_name       VARCHAR(64)  NOT NULL,
    phone           VARCHAR(255),
    email           VARCHAR(128),
    dept_id         BIGINT       NOT NULL,
    data_scope      VARCHAR(16)  NOT NULL DEFAULT 'SELF',
    status          VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    last_login_at   DATETIME,
    failed_count    INT          NOT NULL DEFAULT 0,
    locked_until    DATETIME,
    must_change_pwd TINYINT      NOT NULL DEFAULT 0,
    super_admin     TINYINT      NOT NULL DEFAULT 0,
    created_by      BIGINT,
    created_at      DATETIME     NOT NULL,
    updated_by      BIGINT,
    updated_at      DATETIME     NOT NULL,
    is_deleted      TINYINT      NOT NULL DEFAULT 0,
    version         INT          NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_username (username),
    KEY idx_user_dept (dept_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户';

-- ---------------------------------------------------------------------
-- 3. 角色与权限
-- ---------------------------------------------------------------------
CREATE TABLE iam_role (
    id          BIGINT      NOT NULL,
    code        VARCHAR(32) NOT NULL,
    name        VARCHAR(64) NOT NULL,
    data_scope  VARCHAR(16) NOT NULL DEFAULT 'SELF',
    description VARCHAR(255),
    builtin     TINYINT     NOT NULL DEFAULT 0,
    sort        INT         NOT NULL DEFAULT 0,
    created_by  BIGINT,
    created_at  DATETIME    NOT NULL,
    updated_by  BIGINT,
    updated_at  DATETIME    NOT NULL,
    is_deleted  TINYINT     NOT NULL DEFAULT 0,
    version     INT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色';

CREATE TABLE iam_permission (
    id          BIGINT       NOT NULL,
    code        VARCHAR(64)  NOT NULL,
    name        VARCHAR(64)  NOT NULL,
    type        VARCHAR(16)  NOT NULL,        -- MENU / BUTTON / SPECIAL（如 hard_delete）
    parent_code VARCHAR(64),
    sort        INT          NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_permission_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限点';

CREATE TABLE iam_user_role (
    id      BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_role (user_id, role_id),
    KEY idx_ur_role (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户-角色';

CREATE TABLE iam_role_permission (
    id              BIGINT NOT NULL,
    role_id         BIGINT NOT NULL,
    permission_code VARCHAR(64) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_rp (role_id, permission_code),
    KEY idx_rp_perm (permission_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色-权限';

-- ---------------------------------------------------------------------
-- 4. 客户与联系人
-- ---------------------------------------------------------------------
CREATE TABLE customer (
    id          BIGINT       NOT NULL,
    code        VARCHAR(32)  NOT NULL,
    name        VARCHAR(100) NOT NULL,
    short_name  VARCHAR(50),
    type        VARCHAR(16)  NOT NULL,
    uscc        VARCHAR(32),
    region_code VARCHAR(12),
    address     VARCHAR(255),
    industry    VARCHAR(32),
    level       VARCHAR(4)   NOT NULL DEFAULT 'C',
    owner_id    BIGINT       NOT NULL,
    dept_id     BIGINT       NOT NULL,
    status      VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    merged_to   BIGINT,
    remark      TEXT,
    created_by  BIGINT,
    created_at  DATETIME     NOT NULL,
    updated_by  BIGINT,
    updated_at  DATETIME     NOT NULL,
    is_deleted  TINYINT      NOT NULL DEFAULT 0,
    version     INT          NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_customer_code (code),
    UNIQUE KEY uk_customer_uscc (uscc),
    KEY idx_customer_name (name),
    KEY idx_customer_owner (owner_id),
    KEY idx_customer_dept (dept_id),
    KEY idx_customer_status (status, is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户';

CREATE TABLE customer_contact (
    id          BIGINT       NOT NULL,
    customer_id BIGINT       NOT NULL,
    name        VARCHAR(64)  NOT NULL,
    title       VARCHAR(64),
    phone       VARCHAR(255),
    email       VARCHAR(255),
    wechat      VARCHAR(64),
    is_primary  TINYINT      NOT NULL DEFAULT 0,
    remark      VARCHAR(255),
    created_by  BIGINT,
    created_at  DATETIME     NOT NULL,
    updated_by  BIGINT,
    updated_at  DATETIME     NOT NULL,
    is_deleted  TINYINT      NOT NULL DEFAULT 0,
    version     INT          NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_contact_customer (customer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户联系人';

-- ---------------------------------------------------------------------
-- 5. 合同
-- ---------------------------------------------------------------------
CREATE TABLE contract (
    id                BIGINT         NOT NULL,
    code              VARCHAR(32)    NOT NULL,
    name              VARCHAR(100)   NOT NULL,
    type              VARCHAR(16)    NOT NULL,
    customer_id       BIGINT         NOT NULL,
    amount            DECIMAL(18, 2) NOT NULL,
    signed_at         DATE           NOT NULL,
    perform_start_at  DATE           NOT NULL,
    perform_end_at    DATE           NOT NULL,
    remind_days       INT,
    owner_id          BIGINT         NOT NULL,
    dept_id           BIGINT         NOT NULL,
    status            VARCHAR(16)    NOT NULL DEFAULT 'DRAFT',
    remark            TEXT,
    created_by        BIGINT,
    created_at        DATETIME       NOT NULL,
    updated_by        BIGINT,
    updated_at        DATETIME       NOT NULL,
    is_deleted        TINYINT        NOT NULL DEFAULT 0,
    version           INT            NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_contract_code (code),
    KEY idx_contract_customer (customer_id),
    KEY idx_contract_owner (owner_id),
    KEY idx_contract_dept (dept_id),
    KEY idx_contract_status (status, is_deleted),
    KEY idx_contract_perform_end (perform_end_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合同';

CREATE TABLE contract_attachment (
    id              BIGINT       NOT NULL,
    contract_id     BIGINT       NOT NULL,
    file_object_id  BIGINT       NOT NULL,
    file_name       VARCHAR(255) NOT NULL,
    file_size       BIGINT       NOT NULL,
    uploaded_by     BIGINT       NOT NULL,
    uploaded_at     DATETIME     NOT NULL,
    is_deleted      TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_attachment_contract (contract_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合同附件';

CREATE TABLE contract_note (
    id          BIGINT       NOT NULL,
    contract_id BIGINT       NOT NULL,
    author_id   BIGINT       NOT NULL,
    content     VARCHAR(1000) NOT NULL,
    created_at  DATETIME     NOT NULL,
    PRIMARY KEY (id),
    KEY idx_note_contract (contract_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合同备注';

-- ---------------------------------------------------------------------
-- 6. 回款
-- ---------------------------------------------------------------------
CREATE TABLE payment_plan (
    id               BIGINT         NOT NULL,
    contract_id      BIGINT         NOT NULL,
    period_no        INT            NOT NULL,
    plan_date        DATE           NOT NULL,
    plan_amount      DECIMAL(18, 2) NOT NULL,
    settled_amount   DECIMAL(18, 2) NOT NULL DEFAULT 0,
    unsettled_amount DECIMAL(18, 2) NOT NULL,
    status           VARCHAR(16)    NOT NULL DEFAULT 'PENDING',
    is_overdue       TINYINT        NOT NULL DEFAULT 0,
    overdue_days     INT            NOT NULL DEFAULT 0,
    remind_days      INT,
    created_by       BIGINT,
    created_at       DATETIME       NOT NULL,
    updated_by       BIGINT,
    updated_at       DATETIME       NOT NULL,
    is_deleted       TINYINT        NOT NULL DEFAULT 0,
    version          INT            NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_plan_contract_period (contract_id, period_no),
    KEY idx_plan_status (status, is_overdue),
    KEY idx_plan_date (plan_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='回款计划';

CREATE TABLE payment_record (
    id                 BIGINT         NOT NULL,
    contract_id        BIGINT         NOT NULL,
    arrival_date       DATE           NOT NULL,
    amount             DECIMAL(18, 2) NOT NULL,
    payer              VARCHAR(100),
    voucher_no         VARCHAR(255),
    status             VARCHAR(16)    NOT NULL DEFAULT 'NORMAL',
    red_ref_id         BIGINT,
    unallocated_amount DECIMAL(18, 2) NOT NULL DEFAULT 0,
    remark             VARCHAR(500),
    voucher_file_id    BIGINT,
    created_by         BIGINT,
    created_at         DATETIME       NOT NULL,
    updated_by         BIGINT,
    updated_at         DATETIME       NOT NULL,
    is_deleted         TINYINT        NOT NULL DEFAULT 0,
    version            INT            NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_record_contract (contract_id),
    KEY idx_record_arrival (arrival_date),
    KEY idx_record_red (red_ref_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实际回款';

CREATE TABLE payment_settlement (
    id                BIGINT         NOT NULL,
    payment_plan_id   BIGINT         NOT NULL,
    payment_record_id BIGINT         NOT NULL,
    settle_amount     DECIMAL(18, 2) NOT NULL,
    settle_at         DATETIME       NOT NULL,
    settle_type       VARCHAR(16)    NOT NULL DEFAULT 'AUTO',
    PRIMARY KEY (id),
    KEY idx_settlement_plan (payment_plan_id),
    KEY idx_settlement_record (payment_record_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='核销关系';

-- ---------------------------------------------------------------------
-- 7. 通知
-- ---------------------------------------------------------------------
CREATE TABLE notification (
    id          BIGINT       NOT NULL,
    receiver_id BIGINT       NOT NULL,
    scene       VARCHAR(32)  NOT NULL,
    biz_type    VARCHAR(32)  NOT NULL,
    biz_id      BIGINT,
    title       VARCHAR(128) NOT NULL,
    content     VARCHAR(500) NOT NULL,
    link_url    VARCHAR(255),
    is_read     TINYINT      NOT NULL DEFAULT 0,
    read_at     DATETIME,
    created_at  DATETIME     NOT NULL,
    archived    TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_notification_receiver (receiver_id, is_read, created_at),
    KEY idx_notification_dedupe (receiver_id, scene, biz_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知';

CREATE TABLE notification_setting (
    id           BIGINT      NOT NULL,
    user_id      BIGINT      NOT NULL,
    scene        VARCHAR(32) NOT NULL,
    enabled      TINYINT     NOT NULL DEFAULT 1,
    advance_days INT,
    PRIMARY KEY (id),
    UNIQUE KEY uk_setting_user_scene (user_id, scene)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知偏好';

-- ---------------------------------------------------------------------
-- 8. 系统：变更日志、操作日志、硬删除日志、参数、文件
-- ---------------------------------------------------------------------
CREATE TABLE change_log (
    id            BIGINT       NOT NULL,
    biz_type      VARCHAR(32)  NOT NULL,
    biz_id        BIGINT       NOT NULL,
    field         VARCHAR(64)  NOT NULL,
    old_value     VARCHAR(1000),
    new_value     VARCHAR(1000),
    reason        VARCHAR(255),
    operator_id   BIGINT       NOT NULL,
    operated_at   DATETIME     NOT NULL,
    PRIMARY KEY (id),
    KEY idx_change_biz (biz_type, biz_id, operated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务变更日志';

CREATE TABLE operation_log (
    id            BIGINT       NOT NULL,
    operator_id   BIGINT,
    operator_name VARCHAR(64),
    operator_ip   VARCHAR(64),
    module        VARCHAR(32)  NOT NULL,
    action        VARCHAR(64)  NOT NULL,
    op_type       VARCHAR(16)  NOT NULL,
    biz_type      VARCHAR(32),
    biz_id        BIGINT,
    uri           VARCHAR(255),
    method        VARCHAR(8),
    params_json   TEXT,
    result        VARCHAR(16),
    error_message VARCHAR(500),
    duration_ms   INT,
    created_at    DATETIME     NOT NULL,
    PRIMARY KEY (id),
    KEY idx_oplog_operator (operator_id, created_at),
    KEY idx_oplog_module (module, op_type, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作审计日志';

CREATE TABLE hard_delete_log (
    id            BIGINT NOT NULL,
    operator_id   BIGINT NOT NULL,
    biz_type      VARCHAR(32) NOT NULL,
    biz_id        BIGINT NOT NULL,
    snapshot_json JSON   NOT NULL,
    reason        VARCHAR(500),
    created_at    DATETIME NOT NULL,
    PRIMARY KEY (id),
    KEY idx_hd_biz (biz_type, biz_id),
    KEY idx_hd_operator (operator_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='硬删除日志';

CREATE TABLE system_param (
    id          BIGINT       NOT NULL,
    param_key   VARCHAR(64)  NOT NULL,
    param_value VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    updated_by  BIGINT,
    updated_at  DATETIME     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_param_key (param_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统参数';

CREATE TABLE file_object (
    id            BIGINT       NOT NULL,
    object_key    VARCHAR(255) NOT NULL,
    bucket        VARCHAR(64)  NOT NULL,
    file_name     VARCHAR(255) NOT NULL,
    content_type  VARCHAR(64),
    size          BIGINT       NOT NULL,
    biz_type      VARCHAR(32),
    biz_id        BIGINT,
    uploaded_by   BIGINT       NOT NULL,
    uploaded_at   DATETIME     NOT NULL,
    is_deleted    TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_file_biz (biz_type, biz_id),
    KEY idx_file_uploader (uploaded_by, uploaded_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件元数据';
