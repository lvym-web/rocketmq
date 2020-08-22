package com.lvym.rocketmq.listenr;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;

@RocketMQMessageListener(topic = "springboot-mq",consumerGroup = "group")
@Component
@Slf4j
public class RocketmqListen implements RocketMQListener<MessageExt> {

    @Override
    public void onMessage(MessageExt message) {
        System.out.println(message.getReconsumeTimes());

        log.info("消费者>>>>>"+new String(message.getBody()));

    }
}
