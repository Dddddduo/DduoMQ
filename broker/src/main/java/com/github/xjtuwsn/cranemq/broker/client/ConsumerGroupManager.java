package com.github.xjtuwsn.cranemq.broker.client;

import com.github.xjtuwsn.cranemq.common.consumer.ConsumerInfo;

import java.util.List;
import java.util.Set;

/**
 * @project:dduomq
 * @file:ClientManager
 * @author:dduo
 * @create:2023/10/09-21:46
 */
public interface ConsumerGroupManager {

    Set<String> getGroupClients(String group);
}
