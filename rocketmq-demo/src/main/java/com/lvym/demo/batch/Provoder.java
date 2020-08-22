package com.lvym.demo.batch;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Provoder {

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
        Message msg = new Message("batch", "Tag", ("Hello World" + 1).getBytes());
        Message msg2 = new Message("batch", "Tag", ("Hello World" + 2).getBytes());
        Message msg3 = new Message("batch", "Tag", ("Hello World" + 3).getBytes());
        List<Message> messageList = new ArrayList<Message>();
        messageList.add(msg);
        messageList.add(msg2);
        messageList.add(msg3);
        //5.发送消息
        producer.send(messageList);
        //线程睡1秒
        TimeUnit.SECONDS.sleep(5);
        //6.关闭生产者producer
        producer.shutdown();
        System.out.println("生");
    }
}
