package com.company.crms.common.interceptor;

import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import com.company.crms.common.enums.DataScope;
import com.company.crms.common.security.UserContext;
import com.company.crms.common.security.UserContextHolder;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.sql.SQLException;
import java.util.Set;

/**
 * 数据范围拦截器：根据当前用户的数据范围（SELF/DEPT/ALL）自动追加 SQL 条件。
 *
 * <p>规则：
 * <ul>
 *   <li>{@code ALL}（含超级管理员）：不追加；</li>
 *   <li>{@code DEPT}：追加 {@code dept_id IN (?, ?, ...)}；</li>
 *   <li>{@code SELF}：追加 {@code owner_id = ?}。</li>
 * </ul>
 *
 * <p>仅对带有数据范围标记的 SQL 生效，通过 {@code MappedStatement.id} 后缀
 * {@code WithDataScope} 触发，避免影响系统级查询。
 *
 * <p>实现使用 jsqlparser 4.9（MyBatis-Plus 3.5.6 内置）。
 */
@Slf4j
public class DataScopeInterceptor implements InnerInterceptor {

    private static final String SCOPE_SUFFIX = "WithDataScope";

    @Override
    public void beforeQuery(Executor executor, MappedStatement ms, Object parameter,
                            RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) throws SQLException {
        if (ms.getSqlCommandType() != SqlCommandType.SELECT) {
            return;
        }
        if (!ms.getId().endsWith(SCOPE_SUFFIX)) {
            return;
        }
        UserContext ctx = UserContextHolder.get();
        if (ctx == null || ctx.getDataScope() == DataScope.ALL || ctx.isSuperAdmin()) {
            return;
        }
        String original = boundSql.getSql();
        String rewritten = appendCondition(original, ctx);
        if (!original.equals(rewritten)) {
            try {
                java.lang.reflect.Field field = boundSql.getClass().getDeclaredField("sql");
                field.setAccessible(true);
                field.set(boundSql, rewritten);
            } catch (ReflectiveOperationException e) {
                log.warn("rewrite SQL with data scope failed", e);
            }
        }
    }

    private String appendCondition(String sql, UserContext ctx) {
        try {
            Statement stmt = CCJSqlParserUtil.parse(sql);
            if (!(stmt instanceof PlainSelect plain)) {
                return sql;
            }
            Expression scopeExpr = buildScopeExpression(ctx);
            if (scopeExpr == null) {
                return sql;
            }
            Expression where = plain.getWhere();
            plain.setWhere(where == null ? scopeExpr : new AndExpression(where, scopeExpr));
            return plain.toString();
        } catch (JSQLParserException e) {
            log.warn("parse sql failed, skip data scope: {}", e.getMessage());
            return sql;
        }
    }

    private Expression buildScopeExpression(UserContext ctx) {
        return switch (ctx.getDataScope()) {
            case SELF -> equalsExpression("owner_id", ctx.getUserId());
            case DEPT -> {
                Set<Long> depts = ctx.getDeptIds();
                if (depts == null || depts.isEmpty()) {
                    yield equalsExpression("owner_id", ctx.getUserId());
                }
                ExpressionList<Expression> list = new ExpressionList<>();
                depts.forEach(d -> list.add(new LongValue(d)));
                InExpression in = new InExpression();
                in.setLeftExpression(new Column("dept_id"));
                in.setRightExpression(list);
                yield in;
            }
            default -> null;
        };
    }

    private EqualsTo equalsExpression(String column, Long value) {
        EqualsTo eq = new EqualsTo();
        eq.setLeftExpression(new Column(column));
        eq.setRightExpression(new LongValue(value));
        return eq;
    }
}
