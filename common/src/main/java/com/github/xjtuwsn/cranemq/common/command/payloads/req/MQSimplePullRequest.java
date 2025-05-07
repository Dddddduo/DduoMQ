package com.github.xjtuwsn.cranemq.common.command.payloads.req;

import com.github.xjtuwsn.cranemq.common.command.PayLoad;
import com.github.xjtuwsn.cranemq.common.entity.MessageQueue;
import lombok.*;

import java.io.Serializable;

/**
 * @project:dduomq
 * @file:MQSimplePullRequest
 * @author:dduo
 * @create:2023/10/07-16:58
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class MQSimplePullRequest implements PayLoad, Serializable {
    private MessageQueue messageQueue;
    private long offset;
    private int length;

}
