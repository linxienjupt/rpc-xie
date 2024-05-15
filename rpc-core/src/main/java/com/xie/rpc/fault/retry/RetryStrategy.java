package com.xie.rpc.fault.retry;

import com.xie.rpc.model.RpcRequest;
import com.xie.rpc.model.RpcResponse;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

/**
 * 重试策略
 */
public interface RetryStrategy {
    /**
     * 重试
     * @param callable
     * @return
     * @throws Exception
     */
    RpcResponse doRetry(Callable<RpcResponse> callable) throws Exception;

}
