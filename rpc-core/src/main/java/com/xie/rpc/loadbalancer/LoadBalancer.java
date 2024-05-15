package com.xie.rpc.loadbalancer;

import com.xie.rpc.model.ServiceMetaInfo;

import java.util.List;
import java.util.Map;

/**
 * 负载均衡器（消费端使用），使消费者选择不同的服务提供者
 * 消费者在注册中心差查找的服务地址，是一个列表，放着不同主机提供的服务，消费者需要从这些中选一个
 */
public interface LoadBalancer {

    /**
     * 选择服务调用
     *
     * @param requestParams       请求参数
     * @param serviceMetaInfoList 可用服务列表
     * @return
     */
    ServiceMetaInfo select(Map<String, Object> requestParams, List<ServiceMetaInfo> serviceMetaInfoList);
}