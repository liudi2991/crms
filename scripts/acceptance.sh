#!/usr/bin/env bash
# CRMS 自动化验收脚本
# 用法：./scripts/acceptance.sh [host:port]
# 环境变量：
#   ACCEPT_USER / ACCEPT_PASS         超管账号（默认 admin / Admin@123456）
#   SKIP_PERM=1                       跳过 D 段（不建临时用户）
#   SKIP_ACTUATOR=1                   跳过 G 段对 /actuator/* 的检查
#
# 文档入口：仓库根目录 docs/acceptance.md
#
# 包含 7 类检查：
#  A) 烟测：核心接口 GET（含 IAM / 系统参数 / 回收站 / 操作日志 等）
#  B) E2E：客户 → 联系人 → 合同 → 备注 → 状态机 → 计划生成 → 登记核销 → 红冲 → 硬删
#  C) 回归：本轮联调修过的 9 个 bug 不能再复活
#  D) 权限/数据范围：临时建销售/财务账号验证 403、SELF、硬删特权
#  E) 算法对账：跨期 FIFO、账龄一致性、状态机非法跳转、本月回款一致性
#  F) 安全：未授权 401、二次密码、SQL 注入字符串不致 500、硬删特权
#  G) 健康检查/可观测性：actuator/health、OpenAPI、Swagger UI、操作日志落库
#
# 退出码：0 全过；1 至少有一项 FAIL

set -u
HOST="${1:-localhost:8080}"
BASE="http://${HOST}/api/v1"
USER="${ACCEPT_USER:-admin}"
PASS="${ACCEPT_PASS:-Admin@123456}"

declare -i PASS_CNT=0 FAIL_CNT=0
declare -a FAILS=()

ok()   { printf "  \033[32m[PASS]\033[0m %s\n" "$1"; PASS_CNT+=1; }
fail() { printf "  \033[31m[FAIL]\033[0m %s — %s\n" "$1" "$2"; FAIL_CNT+=1; FAILS+=("$1: $2"); }

assert_success_json() {
  local label="$1" body="$2"
  local s
  s=$(printf '%s' "$body" | python3 -c 'import json,sys
try: print(json.load(sys.stdin).get("success"))
except: print("PARSE_FAIL")' 2>/dev/null)
  if [ "$s" = "True" ]; then ok "$label"; else fail "$label" "${body:0:160}"; fi
}

# 期望 success=false 且 code 命中给定枚举（HTTP 200 业务失败场景）
assert_biz_fail() {
  local label="$1" body="$2" expect="$3"
  local s c
  s=$(printf '%s' "$body" | python3 -c 'import json,sys
try: print(json.load(sys.stdin).get("success"))
except: print("PARSE_FAIL")' 2>/dev/null)
  c=$(printf '%s' "$body" | python3 -c 'import json,sys
try: print(json.load(sys.stdin).get("code") or "")
except: print("")' 2>/dev/null)
  if [ "$s" = "False" ] && [[ ",$expect," == *",$c,"* ]]; then
    ok "$label (code=$c)"
  else
    fail "$label" "want success=false in [$expect], got success=$s code=$c body=${body:0:140}"
  fi
}

# 期望 HTTP 状态码命中给定枚举
assert_http_status() {
  local label="$1" url="$2" expect="$3"
  shift 3
  local code
  code=$(curl -s -o /dev/null -w '%{http_code}' "$url" "$@")
  if [[ ",$expect," == *",$code,"* ]]; then
    ok "$label (HTTP $code)"
  else
    fail "$label" "want HTTP in [$expect], got $code"
  fi
}

# 从 Result 包装里提取 .data，jq 替代品（纯 python3）
jget() {  # $1=body $2=python expr 操作 d
  printf '%s' "$1" | python3 -c "import json,sys
try:
  d=json.load(sys.stdin)
  print($2)
except: print('')" 2>/dev/null
}

# -------- 登录 --------
echo "==> 登录 $USER"
LOGIN=$(curl -s -X POST "$BASE/auth/login" -H "Content-Type: application/json" \
  -d "{\"username\":\"$USER\",\"password\":\"$PASS\"}")
TOKEN=$(printf '%s' "$LOGIN" | python3 -c 'import json,sys
try:
  d=json.load(sys.stdin)
  print(d["data"]["token"] if d.get("success") else "")
except: print("")')

if [ -z "$TOKEN" ]; then
  echo "登录失败：$LOGIN"
  exit 1
fi
H=("-H" "Authorization: Bearer $TOKEN")

# ============ A) 烟测：核心接口 ============
echo
echo "==> A) 烟测（核心接口 GET）"
for p in \
  "/customers?page=1&size=10" \
  "/contracts?page=1&size=10" \
  "/payment-records?page=1&size=10" \
  "/payment-plans?page=1&size=10" \
  "/notifications?page=1&size=10" \
  "/notifications/unread-count" \
  "/notifications/top?limit=5" \
  "/notifications/settings" \
  "/reports/dashboard" \
  "/reports/trend?months=12" \
  "/reports/top-customers?n=5&metric=PAID" \
  "/reports/top-customers?n=5&metric=UNPAID" \
  "/reports/top-customers?n=5&metric=CONTRACT" \
  "/reports/my-todos" \
  "/aging" \
  "/aging/drill?bucket=UNDUE&page=1&size=5" \
  "/auth/me" \
  "/users?page=1&size=10" \
  "/roles" \
  "/departments/tree" \
  "/permissions/tree" \
  "/system-params" \
  "/operation-logs?page=1&size=5" \
  "/recycle-bin?bizType=CUSTOMER&page=1&size=5"
do
  body=$(curl -s "${BASE}$p" "${H[@]}")
  assert_success_json "GET $p" "$body"
done

# ============ B) E2E：业务主流程 ============
echo
echo "==> B) E2E 主流程"

CUST_NAME="ACCEPT_TEST_$(date +%s)"
CUST_BODY=$(curl -s -X POST "$BASE/customers" "${H[@]}" -H "Content-Type: application/json" \
  -d "{\"name\":\"$CUST_NAME\",\"type\":\"ENTERPRISE\",\"level\":\"B\"}")
CUST_ID=$(printf '%s' "$CUST_BODY" | python3 -c 'import json,sys
try: print(json.load(sys.stdin)["data"])
except: print("")')
[ -n "$CUST_ID" ] && ok "create customer ($CUST_ID)" || fail "create customer" "$CUST_BODY"

TODAY=$(date +%F)
END=$(date -v+2y +%F 2>/dev/null || date -d '+2 years' +%F)
# 联系人 CRUD
CONT_BODY=$(curl -s -X POST "$BASE/contacts" "${H[@]}" -H "Content-Type: application/json" \
  -d "{\"customerId\":$CUST_ID,\"name\":\"acc_contact\",\"phone\":\"13800000000\",\"email\":\"a@b.com\",\"isPrimary\":true}")
CONT_ID=$(jget "$CONT_BODY" 'd.get("data","")')
[ -n "$CONT_ID" ] && ok "create contact ($CONT_ID)" || fail "create contact" "$CONT_BODY"

CT_BODY=$(curl -s -X POST "$BASE/contracts" "${H[@]}" -H "Content-Type: application/json" \
  -d "{\"name\":\"ACCEPT_HT_$(date +%s)\",\"type\":\"SALES\",\"customerId\":$CUST_ID,\"amount\":120000,\"signedAt\":\"$TODAY\",\"performStartAt\":\"$TODAY\",\"performEndAt\":\"$END\",\"remindDays\":30}")
CT_ID=$(printf '%s' "$CT_BODY" | python3 -c 'import json,sys
try: print(json.load(sys.stdin)["data"])
except: print("")')
[ -n "$CT_ID" ] && ok "create contract ($CT_ID)" || fail "create contract" "$CT_BODY"

# 合同备注
NOTE_BODY=$(curl -s -X POST "$BASE/contracts/$CT_ID/notes" "${H[@]}" -H "Content-Type: application/json" \
  -d '{"content":"acceptance note"}')
assert_success_json "add contract note" "$NOTE_BODY"

TR_BODY=$(curl -s -X POST "$BASE/contracts/$CT_ID/transition" "${H[@]}" -H "Content-Type: application/json" \
  -d '{"to":"EFFECTIVE","reason":"acceptance"}')
assert_success_json "contract DRAFT→EFFECTIVE" "$TR_BODY"

GEN_BODY=$(curl -s -X POST "$BASE/payment-plans/generate" "${H[@]}" -H "Content-Type: application/json" \
  -d "{\"contractId\":$CT_ID,\"firstPlanDate\":\"$TODAY\",\"periods\":12,\"frequency\":\"MONTHLY\"}")
PLAN_IDS=$(printf '%s' "$GEN_BODY" | python3 -c 'import json,sys
try:
  d=json.load(sys.stdin)["data"]; print(",".join(str(x) for x in d) if d else "")
except: print("")')
PLAN_CNT=$(printf '%s' "$PLAN_IDS" | tr ',' '\n' | grep -c .)
[ "$PLAN_CNT" = "12" ] && ok "generate 12 plans" || fail "generate 12 plans" "got $PLAN_CNT"
FIRST_PLAN=$(printf '%s' "$PLAN_IDS" | cut -d, -f1)

REC_BODY=$(curl -s -X POST "$BASE/payment-records" "${H[@]}" -H "Content-Type: application/json" \
  -d "{\"contractId\":$CT_ID,\"arrivalDate\":\"$TODAY\",\"amount\":10000,\"payer\":\"acceptance\",\"voucherNo\":\"acc-$(date +%s)\",\"targetPlanIds\":[$FIRST_PLAN]}")
REC_ID=$(printf '%s' "$REC_BODY" | python3 -c 'import json,sys
try: print(json.load(sys.stdin)["data"])
except: print("")')
[ -n "$REC_ID" ] && ok "register payment + auto-settle" || fail "register payment" "$REC_BODY"

PLAN1_STATUS=$(curl -s "$BASE/payment-plans/by-contract/$CT_ID" "${H[@]}" | python3 -c '
import json,sys
try:
  p=json.load(sys.stdin)["data"][0]; print(p["status"], p["settledAmount"], p["unsettledAmount"])
except: print("")')
[[ "$PLAN1_STATUS" == "SETTLED 10000.0 0.0" ]] && ok "plan 1 SETTLED after auto-settle" || fail "plan 1 status" "$PLAN1_STATUS"

RED_BODY=$(curl -s -X POST "$BASE/payment-records/$REC_ID/red-reverse" "${H[@]}" -H "Content-Type: application/json" \
  -d '{"redAmount":10000,"reason":"acceptance"}')
assert_success_json "red-reverse" "$RED_BODY"

PLAN1_STATUS=$(curl -s "$BASE/payment-plans/by-contract/$CT_ID" "${H[@]}" | python3 -c '
import json,sys
try:
  p=json.load(sys.stdin)["data"][0]; print(p["status"], p["settledAmount"], p["unsettledAmount"])
except: print("")')
[[ "$PLAN1_STATUS" == "PENDING 0.0 10000.0" ]] && ok "plan 1 rolled back after red-reverse" || fail "plan 1 rollback" "$PLAN1_STATUS"

REC_LIST=$(curl -s "$BASE/payment-records?page=1&size=10&contractId=$CT_ID" "${H[@]}" | python3 -c '
import json,sys
try:
  items=json.load(sys.stdin)["data"]["items"]
  st=sorted([r["status"] for r in items])
  print(",".join(st))
except: print("")')
[[ "$REC_LIST" == *"RED"* && "$REC_LIST" == *"REVERSED"* ]] && ok "RED + REVERSED rows present" || fail "red rows" "$REC_LIST"

# 清理：硬删该合同（带级联）
HARD_BODY=$(curl -s -X DELETE "$BASE/contracts/$CT_ID/hard?reason=acceptance" "${H[@]}")
assert_success_json "hard delete test contract" "$HARD_BODY"
HARD_C_BODY=$(curl -s -X DELETE "$BASE/customers/$CUST_ID/hard?reason=acceptance" "${H[@]}")
assert_success_json "hard delete test customer" "$HARD_C_BODY"

# ============ C) 回归 (本轮 9 bug) ============
echo
echo "==> C) 回归（已知问题不复现）"

# R1 customer 列表：以前空 wrapper 会 SYS-500
B=$(curl -s "$BASE/customers" "${H[@]}")
assert_success_json "R1 customers 列表（无筛选）不报 SQL grammar" "$B"

# R2 contracts 列表
B=$(curl -s "$BASE/contracts" "${H[@]}")
assert_success_json "R2 contracts 列表（无筛选）" "$B"

# R3 payment-records 列表
B=$(curl -s "$BASE/payment-records" "${H[@]}")
assert_success_json "R3 payment-records 列表" "$B"

# R4 reports/trend 不再 ParseException Parameter 'd' not found
B=$(curl -s "$BASE/reports/trend?months=12" "${H[@]}")
assert_success_json "R4 reports/trend（<foreach> 包了 <script>）" "$B"

# R5 admin 访问 aging 不再「无此权限」
B=$(curl -s "$BASE/aging" "${H[@]}")
assert_success_json "R5 admin 通配权限可访问 aging" "$B"

# R6 创建空 USCC 客户不报「USCC 已存在」
NAME="REG_NULL_USCC_$(date +%s)"
B=$(curl -s -X POST "$BASE/customers" "${H[@]}" -H "Content-Type: application/json" \
  -d "{\"name\":\"$NAME\",\"type\":\"ENTERPRISE\",\"level\":\"C\",\"uscc\":\"\"}")
RID=$(printf '%s' "$B" | python3 -c 'import json,sys
try: d=json.load(sys.stdin); print(d.get("data") if d.get("success") else "")
except: print("")')
[ -n "$RID" ] && ok "R6 空 USCC 客户可创建" || fail "R6 空 USCC" "$B"
[ -n "$RID" ] && curl -s -X DELETE "$BASE/customers/$RID/hard?reason=cleanup" "${H[@]}" >/dev/null

# R7 paidThisMonth 不再为负（红冲不会让本月回款变负数）
DASH=$(curl -s -X POST "$BASE/reports/cache/evict" "${H[@]}" >/dev/null && curl -s "$BASE/reports/dashboard" "${H[@]}")
PTM=$(printf '%s' "$DASH" | python3 -c 'import json,sys
try: print(json.load(sys.stdin)["data"]["paidThisMonth"])
except: print("")')
case "$PTM" in
  -*) fail "R7 paidThisMonth 不为负" "got $PTM" ;;
  ""|*err*|*ERR*) fail "R7 paidThisMonth 解析" "$DASH" ;;
  *) ok "R7 paidThisMonth=$PTM (>=0)" ;;
esac

# R8 payment-records VO 包含 contractCode
B=$(curl -s "$BASE/payment-records?page=1&size=5" "${H[@]}")
HAS_CC=$(printf '%s' "$B" | python3 -c '
import json,sys
try:
  items=json.load(sys.stdin)["data"]["items"]
  print("yes" if items and items[0].get("contractCode") else "no")
except: print("err")')
if [ "$HAS_CC" = "yes" ] || [ -z "$(curl -s "$BASE/payment-records?page=1&size=1" "${H[@]}" | python3 -c 'import json,sys;d=json.load(sys.stdin)["data"]["items"];print("x" if d else "")')" ]; then
  ok "R8 payment-records VO 含 contractCode"
else
  fail "R8 contractCode" "no contractCode in record"
fi

# R9 ReportCacheScheduler 缓存名 report-top-customers 不再缺
B=$(curl -s -X POST "$BASE/reports/cache/evict" "${H[@]}")
assert_success_json "R9 evict 全部缓存（含 report-top-customers）" "$B"

# ============ D) 权限 / 数据范围 ============
# SRS §9.3 安全验收硬要求：低权限用户被 RBAC + 数据范围拦截
echo
echo "==> D) 权限 / 数据范围"
TMP_USER_ID=""
TMP_CUST_ID=""
if [ "${SKIP_PERM:-0}" = "1" ]; then
  echo "  [SKIP] D 段（SKIP_PERM=1）"
else
  # 1. 临时建一个销售账号（R01_SALES，data_scope=SELF）
  SUF=$(date +%s)
  TMP_USERNAME="acc_sales_$SUF"
  TMP_PASS_INIT="${CRMS_DEFAULT_PASSWORD:-Crms@123456}"
  CU_BODY=$(curl -s -X POST "$BASE/users" "${H[@]}" -H "Content-Type: application/json" \
    -d "{\"username\":\"$TMP_USERNAME\",\"realName\":\"acc sales\",\"deptId\":1,\"dataScope\":\"SELF\",\"roleIds\":[1]}")
  TMP_USER_ID=$(jget "$CU_BODY" 'd.get("data","")')
  if [ -z "$TMP_USER_ID" ]; then
    fail "D-prep 创建销售用户" "$CU_BODY"
  else
    ok "D-prep 创建销售用户 $TMP_USERNAME ($TMP_USER_ID)"

    # 2. 用销售账号登录（首登有 must_change_pwd 拦截，先改密）
    LO=$(curl -s -X POST "$BASE/auth/login" -H "Content-Type: application/json" \
      -d "{\"username\":\"$TMP_USERNAME\",\"password\":\"$TMP_PASS_INIT\"}")
    SALES_TOKEN=$(jget "$LO" 'd.get("data",{}).get("token","") if d.get("success") else ""')
    if [ -z "$SALES_TOKEN" ]; then
      fail "D-prep 销售用户登录" "$LO"
    else
      ok "D-prep 销售用户登录"
      SH=("-H" "Authorization: Bearer $SALES_TOKEN")
      TMP_PASS_NEW="Sales@$SUF"
      CP=$(curl -s -X POST "$BASE/auth/change-password" "${SH[@]}" -H "Content-Type: application/json" \
        -d "{\"oldPassword\":\"$TMP_PASS_INIT\",\"newPassword\":\"$TMP_PASS_NEW\"}")
      assert_success_json "D-prep 销售首登强制改密" "$CP"

      # 3. 越权：销售调系统管理类接口 → 必须 403 / 业务失败
      assert_http_status "D1 销售调用 /users（无 system:user 权限）" \
        "$BASE/users?page=1&size=1" "403,401" "${SH[@]}"
      assert_http_status "D2 销售调用 /system-params" \
        "$BASE/system-params" "403,401" "${SH[@]}"
      assert_http_status "D3 销售调用 /reports/dashboard（无 report:dashboard）" \
        "$BASE/reports/dashboard" "403,401" "${SH[@]}"
      assert_http_status "D4 销售硬删客户（无 customer:hard_delete）" \
        "$BASE/customers/1/hard" "403,401,400" "${SH[@]}" -X DELETE

      # 4. 数据范围 SELF：销售自建客户能看到，但看不到 admin 建的
      ADMIN_CUST_NAME="ADMIN_OWNED_$SUF"
      ADM_CB=$(curl -s -X POST "$BASE/customers" "${H[@]}" -H "Content-Type: application/json" \
        -d "{\"name\":\"$ADMIN_CUST_NAME\",\"type\":\"ENTERPRISE\",\"level\":\"C\"}")
      ADMIN_CUST_ID=$(jget "$ADM_CB" 'd.get("data","")')

      SALES_CUST_NAME="SALES_OWNED_$SUF"
      SLS_CB=$(curl -s -X POST "$BASE/customers" "${SH[@]}" -H "Content-Type: application/json" \
        -d "{\"name\":\"$SALES_CUST_NAME\",\"type\":\"ENTERPRISE\",\"level\":\"C\"}")
      SALES_CUST_ID=$(jget "$SLS_CB" 'd.get("data","")')

      if [ -n "$SALES_CUST_ID" ]; then
        # 销售看列表：应该看到自己的，看不到 admin 的
        SLIST=$(curl -s "$BASE/customers?page=1&size=50&keyword=OWNED_$SUF" "${SH[@]}")
        SEEN=$(SALES_NAME="$SALES_CUST_NAME" ADMIN_NAME="$ADMIN_CUST_NAME" \
          printf '%s' "$SLIST" | SALES_NAME="$SALES_CUST_NAME" ADMIN_NAME="$ADMIN_CUST_NAME" python3 -c '
import json,sys,os
try:
  items=json.load(sys.stdin)["data"]["items"]
  names=[i.get("name","") for i in items]
  has_self=os.environ["SALES_NAME"] in names
  has_admin=os.environ["ADMIN_NAME"] in names
  print("OK" if has_self and not has_admin else f"BAD self={has_self} admin={has_admin}")
except Exception as e: print(f"ERR:{e}")')
        if [ "$SEEN" = "OK" ]; then
          ok "D5 数据范围 SELF：销售看自己 ✓ / 看不到 admin ✓"
        else
          fail "D5 数据范围 SELF" "$SEEN"
        fi
      else
        fail "D-prep 销售创建客户" "$SLS_CB"
      fi

      # 清理 D 段造的客户
      [ -n "$ADMIN_CUST_ID" ] && curl -s -X DELETE "$BASE/customers/$ADMIN_CUST_ID/hard?reason=cleanup" "${H[@]}" >/dev/null
      [ -n "$SALES_CUST_ID" ] && curl -s -X DELETE "$BASE/customers/$SALES_CUST_ID/hard?reason=cleanup" "${H[@]}" >/dev/null
    fi
  fi
fi

# ============ E) 算法对账 ============
echo
echo "==> E) 算法对账"
# E.0 起一个独立合同跑跨期 FIFO（独立于 B 段，避免污染）
EC_NAME="ACCEPT_E_$(date +%s)"
EC_BODY=$(curl -s -X POST "$BASE/customers" "${H[@]}" -H "Content-Type: application/json" \
  -d "{\"name\":\"$EC_NAME\",\"type\":\"ENTERPRISE\",\"level\":\"C\"}")
E_CUST_ID=$(jget "$EC_BODY" 'd.get("data","")')

if [ -z "$E_CUST_ID" ]; then
  fail "E-prep 创建客户" "$EC_BODY"
else
  E_END=$(date -v+1y +%F 2>/dev/null || date -d '+1 year' +%F)
  EC_HT=$(curl -s -X POST "$BASE/contracts" "${H[@]}" -H "Content-Type: application/json" \
    -d "{\"name\":\"E_HT_$(date +%s)\",\"type\":\"SALES\",\"customerId\":$E_CUST_ID,\"amount\":30000,\"signedAt\":\"$TODAY\",\"performStartAt\":\"$TODAY\",\"performEndAt\":\"$E_END\",\"remindDays\":30}")
  E_CT_ID=$(jget "$EC_HT" 'd.get("data","")')

  # E1 状态机非法跳转：DRAFT → COMPLETED 应被拒（DRAFT 仅允许 → EFFECTIVE / TERMINATED，见 ContractStatus）
  IL_BODY=$(curl -s -X POST "$BASE/contracts/$E_CT_ID/transition" "${H[@]}" -H "Content-Type: application/json" \
    -d '{"to":"COMPLETED","reason":"illegal jump"}')
  IL_OK=$(jget "$IL_BODY" 'str(d.get("success"))')
  if [ "$IL_OK" = "False" ]; then
    ok "E1 状态机非法跳转 DRAFT→COMPLETED 被拒"
  else
    fail "E1 状态机非法跳转应被拒" "$IL_BODY"
  fi

  # 推到 EFFECTIVE 才能排计划
  curl -s -X POST "$BASE/contracts/$E_CT_ID/transition" "${H[@]}" -H "Content-Type: application/json" \
    -d '{"to":"EFFECTIVE","reason":"acceptance"}' >/dev/null

  # 排 3 期 ×10000，按月
  E_GEN=$(curl -s -X POST "$BASE/payment-plans/generate" "${H[@]}" -H "Content-Type: application/json" \
    -d "{\"contractId\":$E_CT_ID,\"firstPlanDate\":\"$TODAY\",\"periods\":3,\"frequency\":\"MONTHLY\"}")
  E_PLAN_CNT=$(jget "$E_GEN" 'len(d.get("data",[]) or [])')
  [ "$E_PLAN_CNT" = "3" ] && ok "E-prep 生成 3 期" || fail "E-prep 生成 3 期" "got $E_PLAN_CNT"

  # E2 跨期 FIFO：登记 25000 不指定 targetPlanIds，应自动按计划日期升序填满
  E_REC=$(curl -s -X POST "$BASE/payment-records" "${H[@]}" -H "Content-Type: application/json" \
    -d "{\"contractId\":$E_CT_ID,\"arrivalDate\":\"$TODAY\",\"amount\":25000,\"payer\":\"E\",\"voucherNo\":\"E-$(date +%s)\"}")
  E_REC_ID=$(jget "$E_REC" 'd.get("data","")')

  E_PLANS=$(curl -s "$BASE/payment-plans/by-contract/$E_CT_ID" "${H[@]}")
  FIFO=$(printf '%s' "$E_PLANS" | python3 -c '
import json,sys
def near(a,b): return abs(float(a)-float(b))<0.01
try:
  ps=json.load(sys.stdin)["data"]
  ps=sorted(ps, key=lambda x: x.get("planDate",""))
  ok = (
    len(ps)==3
    and ps[0]["status"]=="SETTLED" and near(ps[0]["settledAmount"],10000) and near(ps[0]["unsettledAmount"],0)
    and ps[1]["status"]=="SETTLED" and near(ps[1]["settledAmount"],10000) and near(ps[1]["unsettledAmount"],0)
    and ps[2]["status"]=="PARTIAL" and near(ps[2]["settledAmount"],5000)  and near(ps[2]["unsettledAmount"],5000)
  )
  bad = [(p["status"], p["settledAmount"], p["unsettledAmount"]) for p in ps]
  print("OK" if ok else "BAD: " + str(bad))
except Exception as e: print(f"ERR:{e}")')
  if [ "$FIFO" = "OK" ]; then
    ok "E2 跨期 FIFO：25000 自动核销 P1=全, P2=全, P3=半"
  else
    fail "E2 跨期 FIFO 结果不符" "$FIFO"
  fi

  # E3 账龄一致性：四桶之和 ≈ dashboard.unpaidAmount（DashboardVO 字段名）
  curl -s -X POST "$BASE/reports/cache/evict" "${H[@]}" >/dev/null
  AG=$(curl -s "$BASE/aging" "${H[@]}")
  DASH=$(curl -s "$BASE/reports/dashboard" "${H[@]}")
  CONSIST=$(AG_JSON="$AG" DASH_JSON="$DASH" python3 -c '
import json, os
ag = json.loads(os.environ["AG_JSON"])["data"]
dash = json.loads(os.environ["DASH_JSON"])["data"]
sum_ag = round(sum(float(b["amount"]) for b in ag), 2)
total = round(float(dash.get("unpaidAmount") or 0), 2)
print(f"sum_ag={sum_ag} total={total} eq={abs(sum_ag-total)<0.01}")
')
  if [[ "$CONSIST" == *"eq=True"* ]]; then
    ok "E3 账龄四桶之和 ≈ dashboard.unpaidAmount ($CONSIST)"
  else
    fail "E3 账龄一致性" "$CONSIST"
  fi

  # E4 红冲对账：红冲 25000 → 三期回到 PENDING，settled=0
  curl -s -X POST "$BASE/payment-records/$E_REC_ID/red-reverse" "${H[@]}" -H "Content-Type: application/json" \
    -d '{"redAmount":25000,"reason":"E4"}' >/dev/null
  E_PLANS2=$(curl -s "$BASE/payment-plans/by-contract/$E_CT_ID" "${H[@]}")
  ROLLBACK=$(printf '%s' "$E_PLANS2" | python3 -c '
import json,sys
try:
  ps=json.load(sys.stdin)["data"]
  total_settled=sum(float(p["settledAmount"]) for p in ps)
  all_pending=all(p["status"] in ("PENDING","OVERDUE") for p in ps)
  print("OK" if abs(total_settled)<0.01 and all_pending else f"BAD settled={total_settled} pending={all_pending}")
except Exception as e: print(f"ERR:{e}")')
  if [ "$ROLLBACK" = "OK" ]; then
    ok "E4 红冲后所有计划 settled 归零"
  else
    fail "E4 红冲回滚" "$ROLLBACK"
  fi

  # 清理 E 段
  curl -s -X DELETE "$BASE/contracts/$E_CT_ID/hard?reason=cleanup" "${H[@]}" >/dev/null
  curl -s -X DELETE "$BASE/customers/$E_CUST_ID/hard?reason=cleanup" "${H[@]}" >/dev/null
fi

# ============ F) 安全 ============
echo
echo "==> F) 安全"
# F1 未带 token：拒绝（401 或业务失败）
NA=$(curl -s -o /dev/null -w '%{http_code}' "$BASE/customers")
NA_BODY=$(curl -s "$BASE/customers")
if [ "$NA" = "401" ] || [ "$NA" = "403" ]; then
  ok "F1 未带 token → HTTP $NA"
else
  # 也接受 200 + success=false（取决于 GlobalExceptionHandler 配置）
  S=$(jget "$NA_BODY" 'str(d.get("success"))')
  [ "$S" = "False" ] && ok "F1 未带 token → 业务失败" || fail "F1 未带 token" "HTTP=$NA body=${NA_BODY:0:120}"
fi

# F2 错误 token
BAD=$(curl -s -o /dev/null -w '%{http_code}' "$BASE/customers" -H "Authorization: Bearer not-a-real-token")
BAD_BODY=$(curl -s "$BASE/customers" -H "Authorization: Bearer not-a-real-token")
if [ "$BAD" = "401" ] || [ "$BAD" = "403" ]; then
  ok "F2 伪造 token → HTTP $BAD"
else
  S=$(jget "$BAD_BODY" 'str(d.get("success"))')
  [ "$S" = "False" ] && ok "F2 伪造 token → 业务失败" || fail "F2 伪造 token" "HTTP=$BAD body=${BAD_BODY:0:120}"
fi

# F3 二次密码：错密码 → ok=false；对密码 → ok=true（Result.data 为 Map，键名 ok）
WRONG=$(curl -s -X POST "$BASE/auth/verify-password" "${H[@]}" -H "Content-Type: application/json" \
  -d '{"password":"definitely-wrong-pwd-zzz"}')
W_OK=$(jget "$WRONG" 'str(d.get("data",{}).get("ok"))')
[ "$W_OK" = "False" ] && ok "F3a 二次密码：错密码 ok=false" || fail "F3a 二次密码错密码" "$WRONG"

RIGHT=$(curl -s -X POST "$BASE/auth/verify-password" "${H[@]}" -H "Content-Type: application/json" \
  -d "{\"password\":\"$PASS\"}")
R_OK=$(jget "$RIGHT" 'str(d.get("data",{}).get("ok"))')
[ "$R_OK" = "True" ] && ok "F3b 二次密码：对密码 ok=true" || fail "F3b 二次密码对密码" "$RIGHT"

# F4 SQL 注入字符串不致 500（参数化查询应原样作为字面量）
INJ_KW="x%27%20OR%20%271%27%3D%271"   # x' OR '1'='1
INJ_BODY=$(curl -s "$BASE/customers?page=1&size=5&keyword=$INJ_KW" "${H[@]}")
assert_success_json "F4 SQL 注入字符串作为关键字（不致 500）" "$INJ_BODY"

# F5 XSS 字符串原样存（不做 escape，前端渲染层负责）→ 这里只验证服务端不 500
XSS_NAME='<script>alert(1)</script>_'$(date +%s)
XSS_BODY=$(curl -s -X POST "$BASE/customers" "${H[@]}" -H "Content-Type: application/json" \
  -d "{\"name\":\"$XSS_NAME\",\"type\":\"ENTERPRISE\",\"level\":\"C\"}")
XSS_ID=$(jget "$XSS_BODY" 'd.get("data","")')
[ -n "$XSS_ID" ] && ok "F5 XSS 字符串可入库（由前端渲染防御）" || fail "F5 XSS 字符串入库" "$XSS_BODY"
[ -n "$XSS_ID" ] && curl -s -X DELETE "$BASE/customers/$XSS_ID/hard?reason=cleanup" "${H[@]}" >/dev/null

# F6 软删 → 回收站可见 → 用 /recycle-bin/CUSTOMER/{id}/hard 真正硬删（清理）
# 说明：/customers/{id}/hard 走 MP 软删拦截器，selectById 拿不到已软删记录，对回收站清理无效
SD_NAME="SD_TEST_$(date +%s)"
SD_BODY=$(curl -s -X POST "$BASE/customers" "${H[@]}" -H "Content-Type: application/json" \
  -d "{\"name\":\"$SD_NAME\",\"type\":\"ENTERPRISE\",\"level\":\"C\"}")
SD_ID=$(jget "$SD_BODY" 'd.get("data","")')
if [ -n "$SD_ID" ]; then
  curl -s -X DELETE "$BASE/customers/$SD_ID" "${H[@]}" >/dev/null   # 软删
  RB=$(curl -s "$BASE/recycle-bin?page=1&size=50&bizType=CUSTOMER" "${H[@]}")
  IN_RB=$(printf '%s' "$RB" | SD_ID="$SD_ID" python3 -c '
import json,sys,os
try:
  target=os.environ["SD_ID"]
  items=json.load(sys.stdin)["data"]["items"]
  found=any(str(i.get("id"))==target or str(i.get("bizId"))==target for i in items)
  print("yes" if found else "no")
except Exception as e: print(f"err:{e}")')
  [ "$IN_RB" = "yes" ] && ok "F6 软删后客户出现在回收站" || fail "F6 软删进回收站" "$RB"
  # 通过回收站接口硬删（MP 拦截器对该 mapper 路径失效，能真正清理）
  curl -s -X DELETE "$BASE/recycle-bin/CUSTOMER/$SD_ID/hard" "${H[@]}" \
    -H "Content-Type: application/json" -d '{"reason":"acceptance cleanup"}' >/dev/null
fi

# ============ G) 健康检查 / 可观测性 ============
echo
echo "==> G) 健康检查 / 可观测性"
if [ "${SKIP_ACTUATOR:-0}" = "1" ]; then
  echo "  [SKIP] /actuator/* （SKIP_ACTUATOR=1）"
else
  H_BODY=$(curl -s "http://${HOST}/actuator/health")
  H_STATUS=$(jget "$H_BODY" 'd.get("status","")')
  [ "$H_STATUS" = "UP" ] && ok "G1 /actuator/health = UP" || fail "G1 /actuator/health" "$H_BODY"
fi

# G2 OpenAPI JSON 可达
OAPI=$(curl -s -o /dev/null -w '%{http_code}' "http://${HOST}/v3/api-docs")
[ "$OAPI" = "200" ] && ok "G2 /v3/api-docs HTTP 200" || fail "G2 /v3/api-docs" "HTTP=$OAPI"

# G3 Swagger UI 可达
SW=$(curl -s -o /dev/null -w '%{http_code}' "http://${HOST}/swagger-ui/index.html")
[ "$SW" = "200" ] && ok "G3 /swagger-ui/index.html HTTP 200" || fail "G3 swagger-ui" "HTTP=$SW"

# G4 操作日志：至少能查到 1 条（前面 B/E 段做过大量写操作，必然有日志）
OL=$(curl -s "$BASE/operation-logs?page=1&size=5" "${H[@]}")
OL_CNT=$(jget "$OL" 'len(d.get("data",{}).get("items",[]) or [])')
if [ -n "$OL_CNT" ] && [ "$OL_CNT" -gt 0 ] 2>/dev/null; then
  ok "G4 operation-logs 已记录 (>=1 条)"
else
  fail "G4 operation-logs 未记录" "$OL"
fi

# ============ 临时数据清理 ============
# D 段创建的临时销售用户
[ -n "$TMP_USER_ID" ] && curl -s -X DELETE "$BASE/users/$TMP_USER_ID" "${H[@]}" >/dev/null

# ============ 总结 ============
echo
echo "================================"
printf "PASS=%d  FAIL=%d\n" "$PASS_CNT" "$FAIL_CNT"
if [ ${#FAILS[@]} -gt 0 ]; then
  echo "--- 失败明细 ---"
  for f in "${FAILS[@]}"; do echo "  $f"; done
fi
echo "================================"

[ "$FAIL_CNT" -eq 0 ] && exit 0 || exit 1
