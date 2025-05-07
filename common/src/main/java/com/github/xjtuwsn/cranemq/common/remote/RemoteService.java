package com.github.xjtuwsn.cranemq.common.remote;

/**
 * @project:dduomq
 * @file:RemoteService
 * @author:dduo
 * @create:2023/09/27-14:58
 */
public interface RemoteService {

    void start();

    void shutdown();

    void registerHook(RemoteHook hook);
}
