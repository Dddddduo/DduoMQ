package com.github.xjtuwsn.cranemq.client.producer;

import com.github.xjtuwsn.cranemq.client.hook.SendCallback;
import com.github.xjtuwsn.cranemq.client.producer.result.SendResult;
import com.github.xjtuwsn.cranemq.common.entity.Message;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * MQProducer 接口定义了消息队列生产者的基本操作，
 * 生产者可以使用这些方法来启动、关闭以及向消息队列发送消息，
 * 支持不同的发送模式和回调机制。
 *
 * @project:dduomq
 * @file:MQProducer
 * @author:dduo
 * @create:2023/09/27-11:10
 */
public interface MQProducer {

    /**
     * 启动消息队列生产者。
     * 此方法用于初始化生产者的资源，建立与消息队列服务的连接等操作，
     * 为后续的消息发送操作做准备。
     */
    void start();

    /**
     * 关闭消息队列生产者。
     * 此方法用于释放生产者占用的资源，断开与消息队列服务的连接，
     * 确保在不需要生产者时能正确清理资源。
     */
    void shutdown();

    /**
     * 同步发送单条消息到消息队列。
     *
     * @param message 要发送的消息对象，包含消息的内容和相关属性
     * @return 返回消息发送的结果，包含发送状态、消息ID等信息
     */
    SendResult send(Message message);

    /**
     * 发送单条消息到消息队列，可以选择单向发送模式。
     * 在单向发送模式下，生产者发送消息后不等待服务器的响应，
     * 适用于对发送结果不关心或对性能要求较高的场景。
     *
     * @param message 要发送的消息对象
     * @param oneWay  是否采用单向发送模式，true 表示单向发送，false 表示正常发送
     */
    void send(Message message, boolean oneWay);

    /**
     * 异步发送单条消息到消息队列，并在发送完成后执行回调。
     * 当消息发送成功或失败时，会调用传入的回调对象的相应方法。
     *
     * @param message 要发送的消息对象
     * @param callback 消息发送完成后的回调对象，包含成功和失败的处理逻辑
     */
    void send(Message message, SendCallback callback);

    /**
     * 同步发送单条消息到消息队列，并设置延迟发送的时间。
     * 消息会在指定的延迟时间后被发送到消息队列。
     *
     * @param message 要发送的消息对象
     * @param delay 延迟的时间量
     * @param unit 延迟时间的单位，如 TimeUnit.SECONDS 等
     * @return 返回消息发送的结果
     */
    SendResult send(Message message, long delay, TimeUnit unit);

    /**
     * 同步发送多条消息到消息队列。
     *
     * @param messages 要发送的消息列表
     * @return 返回消息发送的结果
     */
    SendResult send(List<Message> messages);

    /**
     * 发送多条消息到消息队列，可以选择单向发送模式。
     *
     * @param messages 要发送的消息列表
     * @param oneWay 是否采用单向发送模式
     */
    void send(List<Message> messages, boolean oneWay);

    /**
     * 异步发送多条消息到消息队列，并在发送完成后执行回调。
     *
     * @param messages 要发送的消息列表
     * @param callback 消息发送完成后的回调对象
     */
    void send(List<Message> messages, SendCallback callback);

    /**
     * 根据选择器选择目标队列并发送单条消息。
     * 选择器可以根据传入的参数决定消息要发送到哪个队列。
     *
     * @param message 要发送的消息对象
     * @param selector 消息队列选择器，用于选择目标队列
     * @param arg 选择器使用的参数
     * @return 返回消息发送的结果
     */
    SendResult send(Message message, MQSelector selector, Object arg);
}