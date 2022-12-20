package com.hmdp.service.impl;

import com.hmdp.config.RabbitMQConfig;
import com.hmdp.dto.SeckillVoucherMsg;
import com.hmdp.service.IRabbitMQService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;

@Service
public class IRabbitMQServiceImpl implements IRabbitMQService {

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Override
    public boolean sendMsg(SeckillVoucherMsg msg) {
        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.RABBITMQ_DIRECT_EXCHANGE, RabbitMQConfig.RABBITMQ_DIRECT_ROUTING, msg);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}