package com.github.xjtuwsn.cranemq.common.remote;

import com.github.xjtuwsn.cranemq.common.route.TopicRouteInfo;

/**
 * @project:dduomq
 * @file:RegistryCallback
 * @author:dduo
 * @create:2023/10/15-15:43
 */
public interface RegistryCallback {

    void onRouteInfo(TopicRouteInfo info);
}
