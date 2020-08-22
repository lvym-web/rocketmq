package com.lvym.demo.onway;

import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.common.message.Message;

import java.util.concurrent.TimeUnit;

/**
 * 这种方式主要用在不特别关心发送结果的场景，例如日志发送。
 */
public class OnwayDemo {
    public static void main(String[] args) throws Exception {
        //1.创建消息生产者producer，并制定生产者组名
        DefaultMQProducer producer = new DefaultMQProducer("group1");
        //2.指定Nameserver地址

        producer.setNamesrvAddr("192.168.146.200:9876;192.168.146.201:9876");
        //3.启动producer
        producer.start();

            //4.创建消息对象，指定主题Topic、Tag和消息体
            /**
             * 参数一：消息主题Topic
             * 参数二：消息Tag
             * 参数三：消息内容
             */
            Message msg = new Message("onway", "Tag1", ("Hello World" ).getBytes());
            //5.发送消息
            producer.sendOneway(msg);
            //线程睡1秒
            TimeUnit.SECONDS.sleep(5);


        //6.关闭生产者producer
        producer.shutdown();
    }
}
