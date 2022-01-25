package com.rtmart.sni;

import org.apache.ibatis.plugin.Interceptor;
import org.apache.shardingsphere.shardingjdbc.jdbc.core.datasource.ShardingDataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * @author : lin.chen1
 * @version : 1.0.0.0
 */
@Configuration
@ConditionalOnClass({Interceptor.class, ShardingDataSource.class})
@EnableConfigurationProperties(StoreNoInterceptorProperties.class)
@ConditionalOnProperty(prefix = StoreNoInterceptorConstant.STORE_NO_INTERCEPTOR, name = "enabled",havingValue = "true" ,matchIfMissing = true)
public class StoreNoInterceptorConfiguration {
    StoreNoInterceptorProperties properties;
    DataSource dataSource;


    public StoreNoInterceptorConfiguration(StoreNoInterceptorProperties properties, DataSource dataSource) {
        this.properties = properties;
        this.dataSource = dataSource;
    }

    @Bean
    @ConditionalOnMissingBean(StoreNoInterceptor.class)
    public StoreNoInterceptor storeNOInterceptor() {
        return new StoreNoInterceptor(dataSource,properties.getFieldName(),properties.getExcludeTables(),properties.getTables());
    }
}
