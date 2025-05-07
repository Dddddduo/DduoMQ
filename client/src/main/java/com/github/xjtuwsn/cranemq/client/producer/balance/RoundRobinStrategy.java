package com.github.xjtuwsn.cranemq.client.producer.balance;

import com.github.xjtuwsn.cranemq.common.entity.MessageQueue;
import com.github.xjtuwsn.cranemq.common.exception.CraneClientException;
import com.github.xjtuwsn.cranemq.common.route.BrokerData;
import com.github.xjtuwsn.cranemq.common.route.TopicRouteInfo;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @project:dduomq
 * @file:RoundRobinStrategy
 * @author:dduo
 * @create:2023/09/30-16:36
 *
 * 该类实现了 LoadBalanceStrategy 接口，使用轮询策略来选择消息队列。
 * 轮询策略意味着按照顺序依次选择可用的消息队列，以达到负载均衡的目的。
 */
public class RoundRobinStrategy implements LoadBalanceStrategy {

    // 全局索引映射，键为主题名，值为 AtomicInteger 类型的计数器
    // 用于记录每个主题当前轮询到的索引位置，使用 ConcurrentHashMap 保证线程安全
    private ConcurrentHashMap<String, AtomicInteger> globalIndex = new ConcurrentHashMap<>();

    /**
     * 该方法用于根据轮询策略选择一个消息队列。
     *
     * @param topic 消息的主题
     * @param info  主题的路由信息，包含了可用的消息队列信息
     * @return 选择的消息队列
     * @throws CraneClientException 如果在选择过程中出现异常
     */
    @Override
    public MessageQueue getNextQueue(String topic, TopicRouteInfo info) throws CraneClientException {
        // 获取该主题下所有可写消息队列的总数
        int total = info.getTotalWriteQueueNumber();
        // 从全局索引映射中获取该主题对应的计数器
        AtomicInteger atomicInteger = this.globalIndex.get(topic);
        // 如果该主题还没有对应的计数器
        if (atomicInteger == null) {
            // 创建一个新的计数器，初始值为 0
            atomicInteger = new AtomicInteger(0);
            // 将该计数器放入全局索引映射中
            this.globalIndex.put(topic, atomicInteger);
        }
        // 获取当前计数器的值，并将计数器的值加 1
        int cur = atomicInteger.getAndIncrement();
        // 通过取模运算得到下一个要选择的消息队列的索引
        int next = cur % total;
        // 根据计算得到的索引，从主题路由信息中获取对应的消息队列
        MessageQueue queue = info.getNumberKQueue(next);
        // 设置该消息队列的主题
        queue.setTopic(topic);
        // 返回选择的消息队列
        return queue;
    }
}