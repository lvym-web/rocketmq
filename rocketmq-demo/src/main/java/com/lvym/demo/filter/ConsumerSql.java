package com.lvym.demo.filter;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.MessageSelector;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.MessageExt;

public class ConsumerSql {
    public static void main(String[] args) throws MQClientException {
       // 实例化消息生产者,指定组名
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("group1");
        // 指定Namesrv地址信息.
        consumer.setNamesrvAddr("192.168.146.200:9876;192.168.146.201:9876");
        // 订阅Topic                           tag || tag2 或  * 
        consumer.subscribe("filtersql", MessageSelector.bySql("k>=6"));
        //消费模式   默认负载均衡
         //consumer.setMessageModel(MessageModel.BROADCASTING);//广播模式
        // 注册回调函数，处理消息
        consumer.registerMessageListener((MessageListenerConcurrently)(msgs,context)->{
            for (MessageExt msg : msgs) {
                System.out.println("过滤Sql消息>>>>>:"+new String(msg.getBody()));
            }
            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        });

        //启动消息者
        consumer.start();
        System.out.println("消");
    }
}
