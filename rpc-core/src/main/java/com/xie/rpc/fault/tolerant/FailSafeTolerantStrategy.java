package com.xie.rpc.fault.tolerant;

import com.xie.rpc.model.RpcResponse;
import lombok.extern.slf4j.Slf4j;
import java.util.Map;

/**
 * 容错策略 静默处理异常
 */
@Slf4j
public class FailSafeTolerantStrategy implements TolerantStrategy{
    @Override
    public RpcResponse doTolerant(Map<String, Object> context, Exception e) {
        log.info("静默处理异常", e);
        return new RpcResponse();
    }

    public static void main(String[] args) {
        System.out.println(FailSafeTolerantStrategy.class.getName());
    }
}
