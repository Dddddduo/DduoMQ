package com.github.xjtuwsn.cranemq.common.remote;

/**
 * @project:dduomq
 * @file:ProducerHook
 * @author:dduo
 * @create:2023/09/27-14:45
 */
public interface RemoteHook {

    void beforeMessage();

    void afterMessage();
}
