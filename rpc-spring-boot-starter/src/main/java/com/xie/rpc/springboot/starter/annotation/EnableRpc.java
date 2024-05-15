package com.xie.rpc.springboot.starter.annotation;

import com.xie.rpc.springboot.starter.bootstrap.RpcConsumerBootstrap;
import com.xie.rpc.springboot.starter.bootstrap.RpcInitBootstrap;
import com.xie.rpc.springboot.starter.bootstrap.RpcProviderBootstrap;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 启用rpc注解
 */
@Target({ElementType.TYPE}) //此注解作用在类上
@Retention(RetentionPolicy.RUNTIME) //此注解表示该注解能够被反射获取到
@Import({RpcInitBootstrap.class, RpcProviderBootstrap.class, RpcConsumerBootstrap.class})
public @interface EnableRpc {

    /**
     * field, 是否需要启动server, 默认为true
     * 服务提供者需要设置为true，服务消费者需要设置为false
     * @return
     */
    boolean needServer() default true;
}
