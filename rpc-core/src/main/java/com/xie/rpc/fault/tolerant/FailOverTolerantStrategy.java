package com.xie.rpc.fault.tolerant;

import com.xie.rpc.fault.retry.RetryStrategy;
import com.xie.rpc.loadbalancer.LoadBalancer;
import com.xie.rpc.model.RpcRequest;
import com.xie.rpc.model.RpcResponse;
import com.xie.rpc.model.ServiceMetaInfo;
import com.xie.rpc.server.tcp.VertxTcpClient;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 转移到其他服务节点 - 容错策略
 */
@Slf4j
public class FailOverTolerantStrategy implements TolerantStrategy {

    @Override
    public RpcResponse doTolerant(Map<String, Object> context, Exception e) {
        LoadBalancer loadBalancer = (LoadBalancer) context.get("loadBalance");
        RpcRequest rpcRequest = (RpcRequest) context.get("rpcRequest");
        List<ServiceMetaInfo> serviceMetaInfos = (List<ServiceMetaInfo>) context.get("serviceMetaInfos");
        serviceMetaInfos.remove((ServiceMetaInfo) context.get("failedServiceMetaInfo"));
        if(serviceMetaInfos == null ||serviceMetaInfos.size() == 0){
            //如果没有其他节点提供该服务，则使用快速失败策略
            TolerantStrategy tolerantStrategy = TolerantStrategyFactory.getInstance(TolerantStrategyKeys.FAIL_FAST);
            tolerantStrategy.doTolerant(null, e);
        }
        Map<String, Object> requestParams = new HashMap<>();
        requestParams.put("methodName", rpcRequest.getMethodName());
        ServiceMetaInfo selectedServiceMetaInfo = loadBalancer.select(requestParams, serviceMetaInfos);

        //使用重试机制
        RpcResponse rpcResponse = new RpcResponse();
        RetryStrategy retryStrategy = (RetryStrategy) context.get("retryStrategy");
        try {
            rpcResponse = retryStrategy.doRetry(() ->
                    VertxTcpClient.doRequest(rpcRequest, selectedServiceMetaInfo));
        } catch (Exception ex) {
            TolerantStrategy tolerantStrategy = TolerantStrategyFactory.getInstance(TolerantStrategyKeys.FAIL_OVER);
            Map<String, Object> map = new HashMap<>();
            map.put("rpcRequest", rpcRequest);
            map.put("retryStrategy", retryStrategy);
            map.put("loadBalancer", loadBalancer);
            map.put("failedServiceMetaInfo", selectedServiceMetaInfo);
            map.put("serviceMetaInfos", serviceMetaInfos);
            rpcResponse = tolerantStrategy.doTolerant(map, ex);
        }
        return rpcResponse;
    }
}