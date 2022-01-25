package com.rtmart.sni;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 所有配置都不区分大小写
 * @author : lin.chen1
 * @version : 1.0.0.0
 */
@Data
@ConfigurationProperties(prefix = StoreNoInterceptorConstant.STORE_NO_INTERCEPTOR)
public class StoreNoInterceptorProperties {
    boolean enabled=true;
    /**
     * 店号字段。支持逗号分隔，兼容不同店号字段名称
     */
    String fieldName="store_no";
    /**
     * 被排除的表一定不拦截，以逗号分隔
     */
    String excludeTables="";
    /**
     * 以逗号分隔 <br>
     * 如果tables为默认值(空字符串)，代表对所有表适用拦截规则(排除没有分片的逻辑表,同时排除excludeTables中的表)<br>
     * 如不tables不为空，这拦截规则只在该范围上有效(排除excludeTables中的表)
     */
    String tables="";
}
