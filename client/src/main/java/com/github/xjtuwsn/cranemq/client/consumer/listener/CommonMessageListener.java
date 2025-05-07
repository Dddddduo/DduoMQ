package com.github.xjtuwsn.cranemq.client.consumer.listener;

import com.github.xjtuwsn.cranemq.common.entity.ReadyMessage;

import java.util.List;

/**
 * @project:dduomq
 * @file:CommonMessageListener
 * @author:dduo
 * @create:2023/10/07-10:35
 */
public interface CommonMessageListener extends MessageListener {
    boolean consume(List<ReadyMessage> messages);
}
