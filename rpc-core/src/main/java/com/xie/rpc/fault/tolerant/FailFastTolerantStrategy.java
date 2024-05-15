package com.xie.rpc.fault.tolerant;

import com.xie.rpc.model.RpcResponse;

import java.util.Map;

/**
 * 容错策略 快速处理异常
 */
public class FailFastTolerantStrategy implements TolerantStrategy{
    @Override
    public RpcResponse doTolerant(Map<String, Object> context, Exception e) {
        throw new RuntimeException("服务报错", e);
    }
}
