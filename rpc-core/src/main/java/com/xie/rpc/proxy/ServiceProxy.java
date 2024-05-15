package com.xie.rpc.proxy;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.xie.rpc.RpcApplication;
import com.xie.rpc.config.RpcConfig;
import com.xie.rpc.constant.RpcConstant;
import com.xie.rpc.fault.retry.RetryStrategy;
import com.xie.rpc.fault.retry.RetryStrategyFactory;
import com.xie.rpc.fault.tolerant.TolerantStrategy;
import com.xie.rpc.fault.tolerant.TolerantStrategyFactory;
import com.xie.rpc.fault.tolerant.TolerantStrategyKeys;
import com.xie.rpc.loadbalancer.LoadBalancer;
import com.xie.rpc.loadbalancer.LoadBalancerFactory;
import com.xie.rpc.model.RpcRequest;
import com.xie.rpc.model.RpcResponse;
import com.xie.rpc.model.ServiceMetaInfo;
import com.xie.rpc.protocol.*;
import com.xie.rpc.registry.Registry;
import com.xie.rpc.registry.RegistryFactory;
import com.xie.rpc.serializer.Serializer;
import com.xie.rpc.serializer.SerializerFactory;
import com.xie.rpc.server.tcp.VertxTcpClient;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * 服务代理（JDK动态代理)，消费者使用。
 */
public class ServiceProxy implements InvocationHandler {
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        //指定序列化器
        final Serializer serializer = SerializerFactory.getInstance(RpcApplication.getRpcConfig().getSerializer());

        //构造请求
        RpcRequest rpcRequest = RpcRequest.builder()
                .serviceName(method.getDeclaringClass().getName())
                .methodName(method.getName())
                .parameterTypes(method.getParameterTypes())
                .args(args)
                .build();

        try {
            //序列化
            byte[] bodyBytes = serializer.serialize(rpcRequest);
            //发送请求
            //从注册中心获取服务的地址
            //从配置文件中获取配置信息
            RpcConfig rpcConfig = RpcApplication.getRpcConfig();
            //获取注册中心
            Registry registry = RegistryFactory.getInstance(rpcConfig.getRegistryConfig().getRegistry());
            //得到并设置服务名称与版本
            ServiceMetaInfo serviceMetaInfo = new ServiceMetaInfo();
            serviceMetaInfo.setServiceName(method.getDeclaringClass().getName());
            serviceMetaInfo.setServiceVersion(RpcConstant.DEFAULT_SERVICE_VERSION);
            //从注册中心获取提供服务的地址，可能提供相同服务的地址有多个，
            List<ServiceMetaInfo> serviceMetaInfos = registry.serviceDiscovery(serviceMetaInfo.getServiceKey());
            if (CollUtil.isEmpty((serviceMetaInfos))) {
                throw new RuntimeException("暂无服务地址");
            }

            //负载均衡获取提供该方法服务的地址
            LoadBalancer loadBalancer = LoadBalancerFactory.getInstance(rpcConfig.getLoadBalancer());
            //将调用方法名（请求路径）作为负载均衡参数
            Map<String, Object> requestParams = new HashMap<>();
            requestParams.put("methodName", rpcRequest.getMethodName());
            ServiceMetaInfo selectedServiceMetaInfo = loadBalancer.select(requestParams, serviceMetaInfos);

            //发送TCP请求
            //使用重试机制
            RpcResponse rpcResponse = new RpcResponse();
            RetryStrategy retryStrategy = RetryStrategyFactory.getInstance(rpcConfig.getRetryStrategy());
            try{
                rpcResponse = retryStrategy.doRetry(() ->
                        VertxTcpClient.doRequest(rpcRequest, selectedServiceMetaInfo));
            }catch (Exception e){
                TolerantStrategy tolerantStrategy = TolerantStrategyFactory.getInstance(rpcConfig.getTolerantStrategy());
                Map<String, Object> map = new HashMap<>();
                map.put("rpcRequest", rpcRequest);
                map.put("retryStrategy", retryStrategy);
                map.put("loadBalancer", loadBalancer);
                map.put("failedServiceMetaInfo", selectedServiceMetaInfo);
                map.put("serviceMetaInfos", serviceMetaInfos);
                rpcResponse = tolerantStrategy.doTolerant(map, e);
            }
            return rpcResponse.getData();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
