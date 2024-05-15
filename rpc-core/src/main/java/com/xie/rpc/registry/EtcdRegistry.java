package com.xie.rpc.registry;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ConcurrentHashSet;
import cn.hutool.cron.CronUtil;
import cn.hutool.cron.task.CronTask;
import cn.hutool.cron.task.Task;
import cn.hutool.json.JSONUtil;
import com.xie.rpc.config.RegistryConfig;
import com.xie.rpc.model.ServiceMetaInfo;
import io.etcd.jetcd.*;

import io.etcd.jetcd.options.GetOption;
import io.etcd.jetcd.options.PutOption;
import io.etcd.jetcd.watch.WatchEvent;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import java.util.stream.Collectors;


public class EtcdRegistry implements Registry {

    private Client client;

    private KV kvClient;

    /**
     * 本机注册的节点 key 集合（用于维护续期），服务提供者使用。
     */
    private final Set<String> localRegisterNodeKeySet = new HashSet<>();

    /**
     * 注册中心服务缓存，供服务消费者使用
     */
    private final RegistryServiceCache registryServiceCache = new RegistryServiceCache();

    /**
     * 正在监听的key 集合
     */
    private final Set<String> watchingKeySet = new ConcurrentHashSet<>();
    /**
     * 根节点
     */
    private static final String ETCD_ROOT_PATH = "/rpc/";

    @Override
    public void init(RegistryConfig registryConfig) {
        client = Client.builder()
                .endpoints(registryConfig.getAddress())
                .connectTimeout(Duration.ofMillis(registryConfig.getTimeout()))
                .build();
        kvClient = client.getKVClient();
        heartBeat();
    }

    @Override
    public void register(ServiceMetaInfo serviceMetaInfo, String impl) throws Exception {
        // 创建 Lease
        Lease leaseClient = client.getLeaseClient();
        // 创建一个 30 秒的租约
        long leaseId = leaseClient.grant(30).get().getID();

        // 设置要存储的键值对    // key = /rpc/serviceName:serviceVersion/host:port
        String registerKey = ETCD_ROOT_PATH + serviceMetaInfo.getServiceNodeKey();
        ByteSequence key = ByteSequence.from(registerKey, StandardCharsets.UTF_8);
        serviceMetaInfo.setServiceName(impl);//存放的 value 中的 serviceName应该为具体是服务的具体实现的类全称
        ByteSequence value = ByteSequence.from(JSONUtil.toJsonStr(serviceMetaInfo), StandardCharsets.UTF_8);

        // 将键值对与租约关联起来，并设置过期时间
        PutOption putOption = PutOption.builder().withLeaseId(leaseId).build();
        kvClient.put(key, value, putOption).get();
        // 添加节点信息到本地缓存
        localRegisterNodeKeySet.add(registerKey);
    }

    @Override
    public void unRegister(ServiceMetaInfo serviceMetaInfo) {
        String registerKey = ETCD_ROOT_PATH + serviceMetaInfo.getServiceNodeKey();
        kvClient.delete(ByteSequence.from(registerKey, StandardCharsets.UTF_8));
        // 也要从本地缓存移除
        localRegisterNodeKeySet.remove(registerKey);
    }

    @Override
    public List<ServiceMetaInfo> serviceDiscovery(String serviceKey) {
        //优先从缓存中获取服务
        List<ServiceMetaInfo> cachedServiceMetaInfo = registryServiceCache.readCache();
        if(cachedServiceMetaInfo != null){
            return cachedServiceMetaInfo;
        }

        // 前缀搜索，结尾一定要加 '/'
        String searchPrefix = ETCD_ROOT_PATH + serviceKey + "/"; // /rpc/serviceName:serviceVersion
        try {
            // 前缀查询, 查看那些主机提供serviceName服务
            GetOption getOption = GetOption.builder().isPrefix(true).build();
            List<KeyValue> keyValues = kvClient.get(
                            ByteSequence.from(searchPrefix, StandardCharsets.UTF_8),
                            getOption)
                    .get()
                    .getKvs();
            // 解析服务信息
            List<ServiceMetaInfo>  serviceMetaInfos =  keyValues.stream()
                    .map(keyValue -> {
                        String key = keyValue.getKey().toString(StandardCharsets.UTF_8);
                        //监听消费者所使用的服务
                        watch(key);
                        String value = keyValue.getValue().toString(StandardCharsets.UTF_8);
                        return JSONUtil.toBean(value, ServiceMetaInfo.class);
                    })
                    .collect(Collectors.toList()); // List<ServiceMetaInfo> serviceMetaInfoList
            //写入消费者缓存
            registryServiceCache.writeCache(serviceMetaInfos);
            return serviceMetaInfos;
        } catch (Exception e) {
            throw new RuntimeException("获取服务列表失败", e);
        }
    }

    @Override
    public void heartBeat() {
        CronUtil.schedule("*/10 * * * * *", new Task() {
            @Override
            public void execute() {
                // 遍历本节点所有的key
                for (String key : localRegisterNodeKeySet) {
                    try {
                        List<KeyValue> keyValues = kvClient.get(ByteSequence.from(key, StandardCharsets.UTF_8))
                                .get()
                                .getKvs();
                        // 该节点已过期（需要重启节点才能重新注册）
                        if (CollUtil.isEmpty(keyValues)) {
                            continue;
                        }
                        // 节点未过期，重新注册（相当于续签）
                        KeyValue keyValue = keyValues.get(0);
                        String value = keyValue.getValue().toString(StandardCharsets.UTF_8);
                        ServiceMetaInfo serviceMetaInfo = JSONUtil.toBean(value, ServiceMetaInfo.class);
                        String impl = serviceMetaInfo.getServiceName();
                        serviceMetaInfo.setServiceName(key.split("/")[2].split(":")[0]);
                        register(serviceMetaInfo, impl);
                    } catch (Exception e) {
                        throw new RuntimeException(key + "续签失败", e);
                    }
                }
            }
        });
        //支持秒级任务
        CronUtil.setMatchSecond(true);
        CronUtil.start();
    }


    @Override
    public void destroy() {
        System.out.println("当前节点下线");
        // 下线节点
        // 遍历本节点所有的 key
        for (String key : localRegisterNodeKeySet) {
            try {
                kvClient.delete(ByteSequence.from(key, StandardCharsets.UTF_8)).get();
            } catch (Exception e) {
                throw new RuntimeException(key + "节点下线失败");
            }
        }

        // 释放资源
        if (kvClient != null) {
            kvClient.close();
        }
        if (client != null) {
            client.close();
        }
    }

    @Override
    public void watch(String serviceNodeKey) {
        Watch watchclient = client.getWatchClient();
        //之前未被监听，开启监听
        boolean newWatch = watchingKeySet.add(serviceNodeKey);
        //如果成功加入,则监听该服务的在注册中心中的信息
        if(newWatch){
            watchclient.watch(ByteSequence.from(serviceNodeKey, StandardCharsets.UTF_8), response ->{
                for(WatchEvent event : response.getEvents()){
                    switch (event.getEventType()){
                        case DELETE: // 如果监听到该服务在注册中心的信息被删除，则进行清除消费端的缓存中的注册服务信息。
                            registryServiceCache.clearCache();
                            break;
                        case PUT:
                        default:
                            break;
                    }
                }
            });
        }
    }
}
