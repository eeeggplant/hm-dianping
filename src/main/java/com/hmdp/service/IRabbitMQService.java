package com.hmdp.service;

import com.hmdp.dto.SeckillVoucherMsg;

public interface IRabbitMQService {
    boolean sendMsg(SeckillVoucherMsg msg);
}
