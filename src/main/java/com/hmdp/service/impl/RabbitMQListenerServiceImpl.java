package com.hmdp.service.impl;

import com.hmdp.config.RabbitMQConfig;
import com.hmdp.dto.SeckillVoucherMsg;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.service.IRabbitMQListenerService;
import com.hmdp.service.IVoucherOrderService;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@RabbitListener(queues = RabbitMQConfig.RABBITMQ_QUEUE_NAME)
public class RabbitMQListenerServiceImpl implements IRabbitMQListenerService {
    @Resource
    private IVoucherOrderService voucherOrderService;

    @Override
    @RabbitHandler
    public void getMsg(SeckillVoucherMsg msg) {

        VoucherOrder voucherOrder = new VoucherOrder();
        voucherOrder.setUserId(msg.getUserId());
        voucherOrder.setVoucherId(msg.getVoucherId());
        voucherOrder.setId(msg.getId());

        voucherOrderService.handleVoucherOrder(voucherOrder);
    }
}
