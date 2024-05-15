package com.xie.rpc.serializer;

import com.xie.rpc.spi.SpiLoader;


/**
 * 序列化器工厂（工厂模式+单例模式，用于获取序列化器对象）
 *
 */
public class SerializerFactory {
    /**
     * 静态代码块，先加载虽有可能的序列化器
     */
    static {
        SpiLoader.load(Serializer.class);
    }

    /**
     * 默认序列化器
     */
    private static final Serializer DEFAULT_SERIALIZER = new JdkSerializer();

    public static Serializer getInstance(String key){
        return SpiLoader.getInstance(Serializer.class, key);
    }
}
