package com.github.xjtuwsn.cranemq.common.command.types;

import java.io.Serializable;

/**
 * @project:dduomq
 * @file:RpcType
 * @author:dduo
 * @create:2023/09/27-10:24
 */
public enum RpcType implements Serializable {

    ONE_WAY,
    ASYNC,
    SYNC
}
