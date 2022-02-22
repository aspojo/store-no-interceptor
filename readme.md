# store-no-interceptor

    在使用sharding-jdbc 4，mybatis 3的项目中，拦截所有不带店号的DML语句（目前包含：update,delete）。
    注意：务必配置排除表。

## 环境

1. mybatis 3
2. sharding-jdbc 4（注意，暂时不支持 shardingsphere 5）

## 使用

1. 引入依赖

```xml

<dependency>
    <groupId>top.logbug.sni</groupId>
    <artifactId>store-no-interceptor-starter</artifactId>
    <version>1.1.3</version>
</dependency>
```

3. 配置
    配置前缀：rt-mart.store-no-interceptor

| key           | 默认值      | 描述                                                                                                                            |
|---------------|----------|-------------------------------------------------------------------------------------------------------------------------------|
| enabled       | true     | 是否启用拦截器                                                                                                                       |
| fieldName     | store_no | 店号字段。支持逗号分隔，兼容不同店号字段名称                                                                                                        |
| excludeTables | t1,t2    | 被排除的表一定不拦截，以逗号分隔                                                                                                              |
| tables        | t1,t2    | 以逗号分隔 <br> 如果tables为默认值(空字符串)，代表对所有表适用拦截规则(排除没有分片的逻辑表,同时排除excludeTables中的表)<br> 如不tables不为空，则拦截规则只在该范围上有效(排除excludeTables中的表) |

