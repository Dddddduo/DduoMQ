package com.github.xjtuwsn.cranemq.common.command.payloads.req;

import com.github.xjtuwsn.cranemq.common.command.PayLoad;
import lombok.*;

import java.io.Serializable;

/**
 * @project:dduomq
 * @file:MQUpdateTopicRequest
 * @author:dduo
 * @create:2023/09/28-21:50
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class MQQueryTopicRequest implements PayLoad, Serializable {
    private String topic;
}
