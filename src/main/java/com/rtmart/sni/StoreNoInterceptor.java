package com.rtmart.sni;

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

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Arrays;
import java.util.Collection;
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
    DataSource dataSource;

    String storeNoFieldName;
    String excludeTables;

    public StoreNoInterceptor(DataSource dataSource, String storeNoFieldName, String excludeTables) {
        this.dataSource = dataSource;
        this.storeNoFieldName = storeNoFieldName;
        this.excludeTables = excludeTables;
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
                SQLStatement parse = ((ShardingDataSource) dataSource).getRuntimeContext().getSqlParserEngine().parse(sql, true);


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
                            boolean excluded = Arrays.stream(excludeTables.split(",")).anyMatch(item -> item.equalsIgnoreCase(tableName));
                            if (!excluded && !existsStoreNO(where)) {
                                log.error("请修改sql，不允许不带店号({})的DML语句执行！！！ --> {}", storeNoFieldName, sql);
                                return -1;
                            }
                        }
                    }
                }
            }

        }
        return invocation.proceed();
    }

    private boolean existsStoreNO(Optional<WhereSegment> whereSegment) {
        if (whereSegment.isPresent()) {
            WhereSegment whereSegment1 = whereSegment.get();
            for (AndPredicate andPredicate : whereSegment.get().getAndPredicates()) {
                for (PredicateSegment predicate : andPredicate.getPredicates()) {
                    if (storeNoFieldName.equalsIgnoreCase(predicate.getColumn().getIdentifier().getValue())) {
                        if (predicate.getRightValue() instanceof PredicateCompareRightValue) {
                            String operator = ((PredicateCompareRightValue) predicate.getRightValue()).getOperator();
                            if ("=".equals(operator)) {
                                return true;
                            }
                        }
                    }

                }
            }
        }
        return false;
    }
}
