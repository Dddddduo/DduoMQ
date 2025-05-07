package com.github.xjtuwsn.cranemq.client.spring.template;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.github.xjtuwsn.cranemq.client.hook.SendCallback;
import com.github.xjtuwsn.cranemq.client.producer.DefaultMQProducer;
import com.github.xjtuwsn.cranemq.client.producer.MQSelector;
import com.github.xjtuwsn.cranemq.client.producer.result.SendResult;
import com.github.xjtuwsn.cranemq.common.entity.Message;
import com.github.xjtuwsn.cranemq.common.exception.CraneClientException;
import com.github.xjtuwsn.cranemq.common.remote.serialize.Serializer;
import com.github.xjtuwsn.cranemq.common.remote.serialize.impl.Hessian1Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @project:dduomq
 * @file:CraneTemplate
 * @author:dduo
 * @create:2023/10/14-15:03
 */
public class CraneMQTemplate {

    // 使用 SLF4J 提供的日志记录器
    private static final Logger log = LoggerFactory.getLogger(CraneMQTemplate.class);

    // 用于序列化数据
    private Serializer serializer;

    //实际的消息生产者实例，负责将消息发送到 MQ 服务端
    private DefaultMQProducer defaultMQProducer;

    /**
     * 构造函数
     * @param defaultMQProducer
     * @param serializer
     */
    public CraneMQTemplate(DefaultMQProducer defaultMQProducer, Serializer serializer) {
        this.defaultMQProducer = defaultMQProducer;
        this.serializer = serializer;
    }

    /**
     * 发送消息方法（同步）
     * 如果只有一个数据项，则直接构造单条消息并发送。
     * 如果有多个数据项，则构建多条消息，并以批量形式发送。
     * @param topic 消息主题
     * @param tag 消息标签
     * @param data 消息体数据，支持可变参数列表
     * @return
     * @param <T>
     */
    public<T> SendResult send(String topic, String tag, T... data) {
        check(topic, tag, data);
        if (data.length == 1) {
            byte[] body = getByteData(data[0]);
            Message message = new Message(topic, tag, body);
            return defaultMQProducer.send(message);
        } else {
            List<Message> messages = new ArrayList<>();
            for (Object e : data) {
                byte[] body = getByteData(data);
                Message message = new Message(topic, tag, body);
                messages.add(message);
            }
            return defaultMQProducer.send(messages);
        }
    }

    /**
     * 发送消息方法（异步回调）
     * 与同步发送类似，但采用异步方式，发送完成后触发回调。
     * @param topic
     * @param tag
     * @param callback 回调函数，用于处理发送完成后的逻辑。
     * @param data
     * @param <T>
     */
    public<T> void send(String topic, String tag, SendCallback callback, T... data) {
        check(topic, tag, data);
        if (data.length == 1) {
            byte[] body = getByteData(data[0]);
            Message message = new Message(topic, tag, body);
            defaultMQProducer.send(message, callback);
        } else {
            List<Message> messages = new ArrayList<>();
            for (Object e : data) {
                byte[] body = getByteData(data);
                Message message = new Message(topic, tag, body);
                messages.add(message);
            }
            defaultMQProducer.send(messages, callback);
        }
    }

    /**
     * 发送消息方法（单向）（不关心是否成功）
     * 适用于对可靠性要求较低的场景。
     * @param topic
     * @param tag
     * @param data
     * @param <T>
     */
    public<T> void sendOneWay(String topic, String tag, T... data) {
        check(topic, tag, data);
        if (data.length == 1) {
            byte[] body = getByteData(data[0]);
            Message message = new Message(topic, tag, body);
            defaultMQProducer.send(message, true);
        } else {
            List<Message> messages = new ArrayList<>();
            for (Object e : data) {
                byte[] body = getByteData(data);
                Message message = new Message(topic, tag, body);
                messages.add(message);
            }
            defaultMQProducer.send(messages, true);
        }
    }

    /**
     * 按选择器发送消息
     * 根据选择器（MQSelector）决定消息发送到哪个分区。
     * @param topic
     * @param tag
     * @param data
     * @param selector 消息选择器
     * @param arg 传递给选择器的参数
     * @return
     * @param <T>
     */
    public<T> SendResult send(String topic, String tag, T data, MQSelector selector, Object arg) {
        check(topic, tag, data);
        if (selector == null || arg == null) {
            throw new CraneClientException("Selector or arg can not be null");
        }
        byte[] body = getByteData(data);
        Message message = new Message(topic, tag, body);
        return defaultMQProducer.send(message, selector, arg);
    }

    /**
     * 延迟发送消息
     * @param topic
     * @param tag
     * @param data
     * @param delay 延迟
     * @param unit 时间单位
     * @return
     * @param <T>
     */
    public<T> SendResult send(String topic, String tag, T data, long delay, TimeUnit unit) {
        check(topic, tag, data);
        byte[] body = getByteData(data);
        Message message = new Message(topic, tag, body);
        return defaultMQProducer.send(message, delay, unit);
    }

    /**
     * 辅助方法：序列化数据
     * 功能：将任意类型的数据序列化为字节数组。
     * 异常处理：如果序列化失败，抛出运行时异常。
     * @param data
     * @return
     * @param <T>
     */
    private<T> byte[] getByteData(T data) {
        try {
            return serializer.serialize(data);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 辅助方法：校验输入
     * 校验内容 1: 数据不能为 null。
     * 校验内容 2: topic 和 tag 不能为空且长度不超过 255。
     * 校验内容 3: 所有数据必须实现 Serializable 接口。
     * @param topic
     * @param tag
     * @param datas
     * @param <T>
     */
    private<T> void check(String topic, String tag, T... datas) {
        if (datas == null || StrUtil.isEmpty(topic) || tag == null) {
            throw new CraneClientException("Send data can not be null");
        }
        if (topic.length() > 255 || tag.length() > 255) {
            throw new CraneClientException("Topic or tag is too long");
        }
        for (T data : datas) {
            if (!Serializable.class.isAssignableFrom(data.getClass())) {
                throw new CraneClientException("Send data must be Serializable");
            }
        }
    }
}
