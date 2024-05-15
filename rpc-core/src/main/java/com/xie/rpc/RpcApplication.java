package com.xie.rpc;

import com.xie.rpc.config.RegistryConfig;
import com.xie.rpc.config.RpcConfig;
import com.xie.rpc.constant.RpcConstant;
import com.xie.rpc.registry.Registry;
import com.xie.rpc.registry.RegistryFactory;
import com.xie.rpc.utils.ConfigUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;


/**
 * RPC 框架应用，启动类，单例模式加载配置文件
 * 相当于 holder，存放了项目全局用到的变量。双检锁单例模式实现
 */
@Slf4j
public class RpcApplication {

    private static volatile RpcConfig rpcConfig;

    /**
     * 框架初始化，支持传入自定义配置。
     * 根据读取的 配置文件 中的信息具体初始化
     * @param newRpcConfig
     */
    public static void init(RpcConfig newRpcConfig) {
        rpcConfig = newRpcConfig;
        log.info("rpc init, config = {}", newRpcConfig.toString());
        //注册中心初始化
        RegistryConfig registryConfig = rpcConfig.getRegistryConfig();
        Registry registry = RegistryFactory.getInstance(registryConfig.getRegistry());
        registry.init(registryConfig);
        log.info("registry init, config = {}", registryConfig);

        //创建并注册ShutdownHook，在提供者退出时将注册信息从注册中心移除。
        Runtime.getRuntime().addShutdownHook((new Thread(registry::destroy)));
    }

    /**
     * 初始化，读取配置文件中的信息，
     */
    public static void init() {
        RpcConfig newRpcConfig; //接收 消费者的 配置信息 的对象，读取配置文件中 类似 rpc.xxx的配置信息
        try {
            newRpcConfig = ConfigUtils.loadConfig(RpcConfig.class, RpcConstant.DEFAULT_CONFIG_PREFIX);// RpcConstant.DEFAULT_CONFIG_PREFIX ="rpc"
        } catch (Exception e) {
            // 配置加载失败，使用默认值
            newRpcConfig = new RpcConfig();
        }
        init(newRpcConfig);
    }


    /**
     * 获取配置
     *
     * @return
     */
    public static RpcConfig getRpcConfig() {
        if (rpcConfig == null) {
            synchronized (RpcApplication.class) {
                if (rpcConfig == null) {
                    init();
                }
            }
        }
        return rpcConfig;
    }

    public static void main(String[] args) {
        RpcApplication.init();
    }
}