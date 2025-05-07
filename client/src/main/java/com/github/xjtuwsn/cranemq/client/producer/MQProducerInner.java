package com.github.xjtuwsn.cranemq.client.producer;

import java.util.List;

/**
 * @project:dduomq
 * @file:MQProducerInner
 * @author:dduo
 * @create:2023/09/27-14:37
 */
public interface MQProducerInner {
    List<String> getBrokerAddress();

    String fetechOrCreateTopic(int queueNumber);
}
