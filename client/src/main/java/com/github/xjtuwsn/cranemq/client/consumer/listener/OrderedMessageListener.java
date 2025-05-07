package com.github.xjtuwsn.cranemq.client.consumer.listener;

import com.github.xjtuwsn.cranemq.common.entity.ReadyMessage;

import java.util.List;

/**
 * @project:dduomq
 * @file:OrderedMessageListener
 * @author:dduo
 * @create:2023/10/07-10:36
 */
public interface OrderedMessageListener extends MessageListener {
    boolean consume(List<ReadyMessage> messages);
}
