package com.xie.rpc.springboot.starter.annotation;

import com.xie.rpc.constant.RpcConstant;
import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 服务提供者使用的注解（用于注册服务）
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface RpcService {
    /**
     * 服务接口类（)
     */
    Class<?> interfaceClass() default void.class;

    /**
     * 服务的主机地址
     */
    String serverHost();

    /**
     * 服务的端口号
     */
    int serverPort();

    /**
     * 版本
     * @return
     */
    String serviceVersion() default RpcConstant.DEFAULT_SERVICE_VERSION;
}
