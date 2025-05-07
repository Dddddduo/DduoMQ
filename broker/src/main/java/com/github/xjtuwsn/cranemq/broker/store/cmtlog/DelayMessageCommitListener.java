package com.github.xjtuwsn.cranemq.broker.store.cmtlog;

/**
 * @project:dduomq
 * @file:DelayMessageCommitListener
 * @author:dduo
 * @create:2023/10/20-10:03
 */
public interface DelayMessageCommitListener {

    void onCommit(long commitLogOffset, long queueOffset, String topic, int queueId, long delay);
}
