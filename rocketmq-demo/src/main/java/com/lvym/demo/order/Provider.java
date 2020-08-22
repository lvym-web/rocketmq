package com.lvym.demo.order;

import com.oracle.jrockit.jfr.Producer;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.MessageQueueSelector;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.remoting.exception.RemotingException;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class Provider {
    public static void main(String[] args) throws MQClientException, RemotingException, InterruptedException, MQBrokerException {
        //1.创建消息生产者producer，并制定生产者组名
        DefaultMQProducer producer = new DefaultMQProducer("group1");
        //2.指定Nameserver地址

        producer.setNamesrvAddr("192.168.146.200:9876;192.168.146.201:9876");
        //3.启动producer
        producer.start();

        // 订单列表
        List<OrderStep> orderList = OrderStep.buildOrders();

        for (int i=1;i<orderList.size();i++){
            String body = orderList.get(i) + "";
            Message message = new Message("orderT", "tag", "i" + i, body.getBytes());
            /**
             * 消息对象
             * 消息队列选择器
             * 选择队列的业务标识  orderId
             */
            SendResult sendResult = producer.send(message, new MessageQueueSelector() {
                /**
                 * @param mqs 队列集合
                 * @param msg 消息对象
                 * @param arg 业务标识参数
                 * @return
                 */
                public MessageQueue select(List<MessageQueue> mqs, Message msg, Object arg) {
                    Long orderId = (Long) arg;
                    long index = orderId % mqs.size();
                    return mqs.get((int) index);
                }
            }, orderList.get(i).getOrderId());

            System.out.println("result>>>>"+sendResult);
        }
       producer.shutdown();
    }
}
