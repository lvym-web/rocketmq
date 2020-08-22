package com.lvym.rocketmq.entity;

import lombok.Data;

import java.util.Date;

@Data
public class TxLog {
    //@Id
    private String txId;
    private Date date;
}