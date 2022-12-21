package com.hmdp.service;

import com.hmdp.dto.SeckillVoucherMsg;

public interface IRabbitMQListenerService {
    void getMsg(SeckillVoucherMsg msg);
}
