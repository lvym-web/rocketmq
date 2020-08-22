package com.lvym;

import com.lvym.rocketmq.RocketmqApplication;
import com.lvym.rocketmq.entity.Order;
import com.lvym.rocketmq.entity.TxLog;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.Properties;
import java.util.UUID;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {RocketmqApplication.class})
@Slf4j
public class ProducerTest {

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @Test
    public void test() throws InterruptedException, RemotingException, MQClientException, MQBrokerException {

//        Order order = new Order();
//        order.setId(1);
//        order.setName("helloWorld");
//        GenericMessage<String> message = new GenericMessage(order);

        Properties properties = new Properties();
//配置对应 Group ID 的最大消息重试次数为 20 次
        Message message = new Message("springboot-mq","hello".getBytes());
                message.setKeys("uuid1");

        rocketMQTemplate.getProducer().send(message);


    }

}
