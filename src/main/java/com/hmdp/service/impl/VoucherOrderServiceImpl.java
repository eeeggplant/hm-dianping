package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.hmdp.dto.Result;
import com.hmdp.dto.SeckillVoucherMsg;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IRabbitMQService;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {
    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Resource
    private RedisIdWorker redisIdWorker;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private IRabbitMQService rabbitMQService;

    IVoucherOrderService proxy;

    @Override
    public void handleVoucherOrder(VoucherOrder voucherOrder) {
        // 1.获取用户
        Long userId = voucherOrder.getUserId();
        // 2.创建锁对象
        RLock lock = redissonClient.getLock("lock:order:" + userId);
        // 3.获取锁
        boolean isLock = lock.tryLock();
        // 4.判断是否成功
        if (!isLock) {
            log.error("不允许重复下单");
            return;
        }
        try {
            // 获取代理对象（事务）
            proxy.createVoucherOrder(voucherOrder);
        } finally {
            // 释放锁
            lock.unlock();
        }
    }

    @Override
    public Result seckillVoucher(Long voucherId) {
        long orderId = redisIdWorker.nextId("order");
        Long userId = UserHolder.getUserDTO().getId();
        String stockKey = "seckill:stock:" + voucherId;
        String orderKey = "seckill:order:" + voucherId;
        // 获取代理对象
        proxy = (IVoucherOrderService) AopContext.currentProxy();
        RLock lock = redissonClient.getLock("lock:order:" + voucherId);
        boolean isLock = false;
        try {
            isLock = lock.tryLock(5, 5, TimeUnit.SECONDS);
            if (isLock) {
                int stock = Integer.parseInt(stringRedisTemplate.opsForValue().get(stockKey));
                // 购买资格判断
                if (stock < 0) {
                    return Result.fail("库存不足！");
                }
                if (stringRedisTemplate.opsForSet().isMember(orderKey, userId.toString())) {
                    return Result.fail("不能重复下单！");
                }
                // 有购买资格，把下单信息保存到阻塞队列
                boolean isSuccess = proxy.saveToQueue(voucherId, orderId, userId, stockKey, orderKey);
                if(isSuccess) {
                    return Result.ok(orderId);
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            if (lock.isLocked() && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
        return Result.fail("下单失败！");
    }

    @Override
    @Transactional
    public boolean saveToQueue(Long voucherId, long orderId, Long userId, String stockKey, String orderKey) {
        // 扣库存
        stringRedisTemplate.opsForValue().decrement(stockKey);
        // 保存用户
        stringRedisTemplate.opsForSet().add(orderKey, userId.toString());
        // 添加到队列
        SeckillVoucherMsg seckillVoucherMsg = new SeckillVoucherMsg(orderId, userId, voucherId);
        return rabbitMQService.sendMsg(seckillVoucherMsg);
    }

    @Transactional
    @Override
    public void createVoucherOrder(VoucherOrder voucherOrder) {
        // 5.一人一单
//        Long userId = UserHolder.getUserDTO().getId();
        Long userId = voucherOrder.getUserId();
//        Long userId = 1010L;
        synchronized (userId.toString()) {
            // 5.1.查询订单
            int count = query().eq("user_id", userId)
                    .eq("voucher_id", voucherOrder.getVoucherId()).count();
            // 5.2.判断是否存在
            if (count > 0) {
                // 用户已经购买过了
                log.error("用户已经购买过一次！");
                return;
            }
            // 6.扣减库存
            boolean success = seckillVoucherService.update()
                    .setSql("stock = stock - 1")
                    .eq("voucher_id", voucherOrder.getVoucherId())
                    .gt("stock", 0)
                    .update();
            if (!success) {
                // 扣减不足
                log.error("库存不足！");
                return;
            }
            // 7.创建订单
            save(voucherOrder);
        }
    }
}
