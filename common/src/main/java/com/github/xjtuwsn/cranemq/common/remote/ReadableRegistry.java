package com.github.xjtuwsn.cranemq.common.remote;

import com.github.xjtuwsn.cranemq.common.route.TopicRouteInfo;

/**
 * @project:dduomq
 * @file:RemoteRegistry
 * @author:dduo
 * @create:2023/10/15-15:21
 */
public interface ReadableRegistry extends RemoteService {

    TopicRouteInfo fetchRouteInfo(String topic);

    void fetchRouteInfo(String topic, RegistryCallback callback);

}
