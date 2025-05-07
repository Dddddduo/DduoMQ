package com.github.xjtuwsn.cranemq.client.producer.balance;

import com.github.xjtuwsn.cranemq.common.entity.MessageQueue;
import com.github.xjtuwsn.cranemq.common.exception.CraneClientException;
import com.github.xjtuwsn.cranemq.common.route.BrokerData;
import com.github.xjtuwsn.cranemq.common.route.TopicRouteInfo;

import java.util.Random;

/**
 * @project:dduomq
 * @file:RandomLoadBalance
 * @author:dduo
 * @create:2023/09/29-22:00
 *
 * 该类实现了 LoadBalanceStrategy 接口，使用随机策略来选择消息队列。
 * 随机策略意味着随机选择可用的消息队列，以达到负载均衡的目的。
 */
public class RandomStrategy implements LoadBalanceStrategy {
    // 用于生成随机数的 Random 对象
    private Random random = new Random();

    /**
     * 该方法用于根据随机策略选择一个消息队列。
     *
     * @param topic 消息的主题
     * @param info  主题的路由信息，包含了可用的 broker 信息
     * @return 选择的消息队列
     * @throws CraneClientException 如果在选择过程中出现异常
     */
    @Override
    public MessageQueue getNextQueue(String topic, TopicRouteInfo info) throws CraneClientException {
        // 获取可用的 broker 数量
        int brokerNumber = info.brokerNumber();
        // 随机选择一个 broker 的索引
        int pickedBroker = random.nextInt(brokerNumber);
        // 根据选择的索引获取对应的 broker 数据
        BrokerData brokerData = info.getBroker(pickedBroker);
        // 获取该 broker 的主队列的可写队列数量
        int queueSize = brokerData.getMasterQueueData().getWriteQueueNums();
        // 随机选择一个队列的索引
        int pickedQueue = random.nextInt(queueSize);
        // 创建并返回一个新的消息队列对象
        return new MessageQueue(topic, brokerData.getBrokerName(), pickedQueue);
    }
}