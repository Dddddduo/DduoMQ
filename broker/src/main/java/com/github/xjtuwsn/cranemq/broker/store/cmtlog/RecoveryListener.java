package com.github.xjtuwsn.cranemq.broker.store.cmtlog;

/**
 * @project:dduomq
 * @file:RecoveryListener
 * @author:dduo
 * @create:2023/10/05-19:19
 */
public interface RecoveryListener {

    void onUpdateOffset(long offset, int size);
}
