package com.github.xjtuwsn.cranemq.extension;

import com.github.xjtuwsn.cranemq.client.spring.factory.CraneClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @project:dduomq
 * @file:ClientAutoConfiguration
 * @author:dduo
 * @create:2023/10/14-21:28
 */
@Configuration
public class ClientAutoConfiguration {

    @Bean
    public CraneClientFactory getFactory() {
        CraneClientFactory craneClientFactory = new CraneClientFactory();
        return craneClientFactory;
    }
}
