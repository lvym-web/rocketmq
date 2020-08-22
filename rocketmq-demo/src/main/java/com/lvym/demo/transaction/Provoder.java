package com.lvym.demo.transaction;

import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.producer.*;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;

import java.util.concurrent.TimeUnit;

public class Provoder {

    public static void main(String[] args) throws Exception {
        //1.创建消息生产者producer，并制定生产者组名
        TransactionMQProducer producer = new TransactionMQProducer("group1");
        //2.指定Nameserver地址
        producer.setNamesrvAddr("192.168.146.200:9876;192.168.146.201:9876");
        //创建事务监听器
        producer.setTransactionListener(new TransactionListener() {
            //执行本地事务  处理半消息
            @Override
            public LocalTransactionState executeLocalTransaction(Message msg, Object arg) {
                if (StringUtils.equals(msg.getTags(),"TagA")){
                    return LocalTransactionState.COMMIT_MESSAGE;
                }else if (StringUtils.equals(msg.getTags(),"TagB")){
                    return LocalTransactionState.ROLLBACK_MESSAGE;
                }else {
                    return LocalTransactionState.UNKNOW;
                }
            }
            //  UNKNOW回查
            @Override
            public LocalTransactionState checkLocalTransaction(MessageExt msg) {
                System.out.println("MQ检查消息Tag-UNKNOW【"+msg.getTags()+"】的本地事务执行结果");
                return LocalTransactionState.COMMIT_MESSAGE;
            }
        });
        //3.启动producer
        producer.start();
        //4.创建消息对象，指定主题Topic、Tag和消息体
        /**
         * 参数一：消息主题Topic
         * 参数二：消息Tag
         * 参数三：消息内容
         */
        String[] tags={"TagA", "TagB", "TagC"};
        for (int i=0;i<3;i++){
            Message msg = new Message("transaction", tags[i], ("Hello World"+i).getBytes());

            //5.发送消息
            TransactionSendResult transactionSendResult = producer.sendMessageInTransaction(msg, null);
            System.out.println(">>>>>"+transactionSendResult);
            //线程睡1秒
            TimeUnit.SECONDS.sleep(10);
        }

        //6.关闭生产者producer
      //  producer.shutdown();
        System.out.println("生");
    }
}
