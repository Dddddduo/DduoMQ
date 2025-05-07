package com.github.xjtuwsn.cranemq.client.producer.result;

import com.github.xjtuwsn.cranemq.common.route.TopicRouteInfo;
import lombok.*;

/**
 * SendResult 类用于封装消息发送操作的结果信息。
 * 该类包含了消息发送的结果类型、关联 ID、主题路由信息以及主题名称等内容。
 *
 * @project:dduomq
 * @file:SendResult
 * @author:dduo
 * @create:2023/09/27-19:42
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class SendResult {

    /**
     * 消息发送的结果类型，表明消息发送操作的最终状态，
     * 例如成功、失败、超时等不同的结果状态。
     */
    private SendResultType resultType;

    /**
     * 消息的关联 ID，用于唯一标识本次消息发送操作，
     * 可以在后续的业务处理中通过该 ID 来追踪和关联相关的消息操作。
     */
    private String correlationID;

    /**
     * 主题的路由信息，包含了消息主题在消息队列系统中的路由规则、
     * 可用的队列等相关信息，有助于消息正确地被路由和处理。
     */
    private TopicRouteInfo topicRouteInfo;

    /**
     * 消息所发送到的主题名称，用于标识消息所属的主题分类。
     */
    private String topic;

    /**
     * 构造函数，用于初始化消息发送结果的基本信息。
     *
     * @param resultType 消息发送的结果类型
     * @param correlationID 消息的关联 ID
     */
    public SendResult(SendResultType resultType, String correlationID) {
        this.resultType = resultType;
        this.correlationID = correlationID;
    }

    /**
     * 重写 toString 方法，将对象的主要信息以字符串形式返回，
     * 方便在日志记录、调试等场景中查看消息发送结果的关键信息。
     *
     * @return 包含消息发送结果类型、关联 ID 和主题名称的字符串
     */
    @Override
    public String toString() {
        return "SendResult{" +
                "resultType=" + resultType +
                ", correlationID='" + correlationID + '\'' +
                ", topic='" + topic + '\'' +
                '}';
    }
}