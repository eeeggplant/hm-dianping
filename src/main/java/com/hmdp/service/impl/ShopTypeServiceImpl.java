package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.util.ArrayList;
import java.util.List;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_TYPE_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryTypeList() {
        List<String> listJson = stringRedisTemplate.opsForList().range(CACHE_SHOP_TYPE_KEY, 0, -1);
        List<ShopType> list = new ArrayList<>();
        if (!listJson.isEmpty()) {
            jsonToList(listJson, list);
            return Result.ok(list);
        }

        list.addAll(list());
        if (list.isEmpty()) {
            return Result.fail("店铺种类不存在！");
        }
        listToJson(listJson, list);
        stringRedisTemplate.opsForList().rightPushAll(CACHE_SHOP_TYPE_KEY, listJson);
        return Result.ok(list);
    }

    private void listToJson(List<String> listJson, List<ShopType> list) {
        for (ShopType shopType : list) {
            String shopTypeJson = JSONUtil.toJsonStr(shopType);
            listJson.add(shopTypeJson);
        }
    }

    private void jsonToList(List<String> listJson, List<ShopType> list) {
        for (String shopTypeJson : listJson) {
            ShopType shopType = JSONUtil.toBean(shopTypeJson, ShopType.class);
            list.add(shopType);
        }
    }

}
