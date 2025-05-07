package com.github.xjtuwsn.cranemq.broker.store.flush;

import com.github.xjtuwsn.cranemq.broker.store.MappedFile;

/**
 * @project:dduomq
 * @file:FlushDiskService
 * @author:dduo
 * @create:2023/10/06-10:54
 */
public interface FlushDiskService {

    void flush();

    void flush(MappedFile mappedFile);

}
