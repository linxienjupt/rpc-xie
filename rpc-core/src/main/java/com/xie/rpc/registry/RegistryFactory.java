package com.xie.rpc.registry;

import com.xie.rpc.serializer.JdkSerializer;
import com.xie.rpc.serializer.Serializer;
import com.xie.rpc.spi.SpiLoader;


/**
 * 注册工厂（工厂模式+单例模式，用于获取注册中心对象）
 *
 */
public class RegistryFactory {
    /**
     * 静态代码块，先加载虽有可能的注册中心
     */
    static {
        SpiLoader.load(Registry.class);
    }

    /**
     * 默认序列化器
     */
    private static final Registry DEFAULT_SERIALIZER = new EtcdRegistry();

    public static Registry getInstance(String key){
        return SpiLoader.getInstance(Registry.class, key);
    }
}
