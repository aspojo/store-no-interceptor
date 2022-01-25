package com.rtmart.sni;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author : lin.chen1
 * @version : 1.0.0.0
 */
@Data
@ConfigurationProperties(prefix = StoreNoInterceptorConstant.STORE_NO_INTERCEPTOR)
public class StoreNoInterceptorProperties {
    boolean enabled=true;
    String fieldName="store_no";
    String excludeTables="";
}
