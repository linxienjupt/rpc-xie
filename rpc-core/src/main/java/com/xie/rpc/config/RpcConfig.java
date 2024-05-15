package com.xie.rpc.config;
import com.xie.rpc.fault.retry.RetryStrategyKeys;
import com.xie.rpc.fault.tolerant.TolerantStrategy;
import com.xie.rpc.fault.tolerant.TolerantStrategyKeys;
import com.xie.rpc.loadbalancer.LoadBalancerKeys;
import com.xie.rpc.serializer.SerializerKeys;
import lombok.Data;

/**
 * RPC 框架全局配置
 */
@Data
public class RpcConfig {

    /**
     * 名称
     */
    private String name = "xie-rpc";

    /**
     * 版本号
     */
    private String version = "1.0";

    /**
     * 服务器主机名
     */
    private String serverHost;

    /**
     * 服务器端口号
     */
    private Integer serverPort;

    /**
     * 序列化器
     */
    private String serializer = SerializerKeys.JDK;

    /**
     * 负载均衡器
     */
    private String loadBalancer = LoadBalancerKeys.ROUND_ROBIN;

    /**
     * 模拟调用
     */
    private boolean mock = false;

    /**
     * 重试策略
     */
    private String retryStrategy = RetryStrategyKeys.NO;

    /**
     * 容错策略
     */
    private String tolerantStrategy = TolerantStrategyKeys.FAIL_OVER;

    /**
     * 注册中心配置
     */
    private RegistryConfig registryConfig = new RegistryConfig();
}
