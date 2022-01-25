package com.rtmart.sni;

import cn.hutool.core.util.StrUtil;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.shardingsphere.shardingjdbc.jdbc.core.datasource.ShardingDataSource;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.predicate.AndPredicate;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.predicate.PredicateSegment;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.predicate.WhereSegment;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.predicate.value.PredicateCompareRightValue;
import org.apache.shardingsphere.sql.parser.sql.segment.generic.table.SimpleTableSegment;
import org.apache.shardingsphere.sql.parser.sql.statement.SQLStatement;
import org.apache.shardingsphere.sql.parser.sql.statement.dml.DMLStatement;
import org.apache.shardingsphere.sql.parser.sql.statement.dml.DeleteStatement;
import org.apache.shardingsphere.sql.parser.sql.statement.dml.UpdateStatement;
import org.apache.shardingsphere.underlying.common.rule.DataNode;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 店号拦截器
 *
 * @author : lin.chen1
 * @version : 1.0.0.0
 */
@Slf4j
@Intercepts(
        {
                @Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class}),
                @Signature(type = StatementHandler.class, method = "getBoundSql", args = {}),
                @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}),
                @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
                @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class}),
        }
)
@NoArgsConstructor
public class StoreNoInterceptor implements Interceptor {
    ShardingDataSource dataSource;
    String storeNoFieldName;
    String excludeTables;
    String tableNames;

    public StoreNoInterceptor(DataSource dataSource, String storeNoFieldName, String excludeTables, String tableNames) {
        this.dataSource = (ShardingDataSource) dataSource;
        this.storeNoFieldName = storeNoFieldName;
        this.excludeTables = excludeTables;
        this.tableNames = tableNames;
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object target = invocation.getTarget();
        Object[] args = invocation.getArgs();
        if (target instanceof Executor) {
            Object parameter = args[1];
            MappedStatement ms = (MappedStatement) args[0];
            boolean isUpdate = args.length == 2;
            if (isUpdate) {
                String sql = ms.getBoundSql(parameter).getSql();
                SQLStatement parse = dataSource.getRuntimeContext().getSqlParserEngine().parse(sql, true);
                if (parse instanceof DMLStatement) {
                    DMLStatement dmlStatement = (DMLStatement) parse;
                    Collection<SimpleTableSegment> tables = null;
                    Optional<WhereSegment> where = Optional.empty();
                    if (dmlStatement instanceof UpdateStatement) {
                        UpdateStatement statement = (UpdateStatement) dmlStatement;
                        tables = statement.getTables();
                        where = statement.getWhere();
                    } else if (dmlStatement instanceof DeleteStatement) {
                        DeleteStatement statement = (DeleteStatement) dmlStatement;
                        tables = statement.getTables();
                        where = statement.getWhere();
                    }
                    if (tables != null) {
                        for (SimpleTableSegment table : tables) {
                            String tableName = table.getTableName().getIdentifier().getValue();
                            if (shouldDoInterceptor(tableName)) {
                                if (!where.isPresent() || !existsStoreNO(where.get())) {
                                    log.error("请修改sql，不允许不带店号({})的DML语句执行！！！ --> {}", storeNoFieldName, sql);
                                    return -1;
                                }
                            }
                        }
                    }
                }
            }

        }
        return invocation.proceed();
    }

    private boolean shouldDoInterceptor(String tableName) {
        List<DataNode> actualDataNodes = dataSource.getRuntimeContext().getRule().getTableRule(tableName).getActualDataNodes();
        boolean includeTable = (StrUtil.isEmpty(tableNames) && actualDataNodes.size() > 1) || containsStr(tableNames, tableName);
        boolean excludeTable = containsStr(excludeTables, tableName);
        return includeTable && !excludeTable;
    }

    private boolean containsStr(String str, String subStr) {
        return Arrays.stream(str.split(",")).anyMatch(item -> item.equalsIgnoreCase(subStr));
    }

    private boolean existsStoreNO(WhereSegment whereSegment) {
        for (AndPredicate andPredicate : whereSegment.getAndPredicates()) {
            for (PredicateSegment predicate : andPredicate.getPredicates()) {
                if (containsStr(storeNoFieldName, predicate.getColumn().getIdentifier().getValue())) {
                    if (predicate.getRightValue() instanceof PredicateCompareRightValue) {
                        String operator = ((PredicateCompareRightValue) predicate.getRightValue()).getOperator();
                        if ("=".equals(operator)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}
