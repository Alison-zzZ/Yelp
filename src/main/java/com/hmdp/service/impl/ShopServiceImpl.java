package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.hmdp.dto.Result;
import com.hmdp.entity.RedisData;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.CacheClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private CacheClient cacheClient;

    @Override
    public Result queryById(Long id) {
        // 解决缓存穿透
        // Shop shop = cacheClient.queryWithPassThrough
        //         (CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);

        // 互斥锁解决缓存击穿
        // Shop shop = queryWithMutex(id);
        // Shop shop = cacheClient
        //          .queryWithMutex(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);

        // 逻辑过期解决缓存击穿
         Shop shop = cacheClient
                 .queryWithLogicalExpire(CACHE_SHOP_KEY, id, Shop.class, this::getById, 20L, TimeUnit.SECONDS);

        if (shop == null) {
            return Result.fail("店铺不存在！");
        }
        return Result.ok(shop);
    }

    /**
     * 缓存穿透解决方案（有工具类后被取代）
     * public Shop queryWithPassThrough(Long id)
     */
//    public Shop queryWithPassThrough(Long id){
//        String key = CACHE_SHOP_KEY + id;
//        // 1. 从redis中查询商铺缓存
//        String shopJson = stringRedisTemplate.opsForValue().get(key);
//        // 2. 判断是否存在
//        if(StrUtil.isNotBlank(shopJson)){ // isNotBlank() - null和空字符串都返回false
//            // 2.1 命中，直接返回
//            return JSONUtil.toBean(shopJson, Shop.class);
//        }
//
//        // 说明 shopJson 为 null 或者空字符串
//        if(shopJson != null){ // 如果是空字符串
//            // 返回错误信息
//            return null;
//        }
//
//        // 2.2 当前数据是null，根据id查询数据库
//        Shop shop = getById(id);
//        // 2.2.1 数据库也不存在，返回error，设置缓存空值
//        if(shop == null){
//            stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES); // 设置null
//            return null;
//        }
//        // 2.2.2 数据库中存在，写入redis
//        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);
//        return shop;
//    }

    /**
     * 解决缓存击穿
     * 方案1: 互斥锁（有工具类后被取代）
     * public Shop queryWithMutex(Long id)
     */
//    public Shop queryWithMutex(Long id){
//
//        String key = CACHE_SHOP_KEY + id;
//        // 1. 从redis中查询商铺缓存
//        String shopJson = stringRedisTemplate.opsForValue().get(key);
//        // 2. 判断缓存是否命中
//        // isNotBlank() - 非null 并且 非空字符串 才返回true
//        if(StrUtil.isNotBlank(shopJson)){
//            // 2.1 命中，直接返回
//            return JSONUtil.toBean(shopJson, Shop.class);
//        }
//
//        // 此时说明 shopJson 为 null 或者空字符串
//
//        // 3. 先判断是否为空字符串
//        if(shopJson != null){
//            // 3.1 shopJson 为空字符串，是之前为了防止缓存穿透写入的空字符串
//            // 说明数据库里面没有这家店，返回null即可
//            return null;
//        }
//
//        // 3.2 shopJson 是null，说明没有向数据库写入过这个key（从未查询过）
//        //     此时是真正的未命中，需要获取互斥锁之后去数据库查询
//
//        // 4 尝试获取互斥锁
//        String lockKey = "lock:shop:" + id;
//        Shop shop = null;
//        try {
//            boolean isLock = tryLock(lockKey);
//            // 5 判断是否获取成功
//            if(!isLock){
//                // 5.1 失败，休眠并且重试
//                Thread.sleep(50);
//                return queryWithMutex(id);  // 使用递归重试
//            }
//
//            // 5.2 成功，当前线程负责“重建缓存”
//            // 6 查询数据库
//            shop = getById(id);
//            Thread.sleep(200); // 模拟重建耗时
//
//            // 6.1 数据库中没有这条数据，说明真的不存在
//            if(shop == null){
//                // 为了防止缓存穿透写入空字符串（防止缓存穿透）
//                stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
//                return null;
//            }
//            // 6.2 数据库中存在，写入redis
//            stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        } finally {
//            // 7 释放互斥锁
//            unLock(lockKey);
//        }
//
//        // 8 返回数据
//        return shop;
//    }

    /**
     * 解决缓存击穿
     * 方案2: 逻辑过期（有工具类后被取代）
     *  public Shop queryWithLogicalExpire(Long id)
     *  private static final ExecutorService CACHE_REBUILD_EXECUTOR =  Executors.newFixedThreadPool(10)
     */
//    private static final ExecutorService CACHE_REBUILD_EXECUTOR =  Executors.newFixedThreadPool(10);
//    public Shop queryWithLogicalExpire(Long id){
//        String shopKey = CACHE_SHOP_KEY + id;
//        // 1. 从redis中查询
//        String shopJson = stringRedisTemplate.opsForValue().get(shopKey);
//        // 2. 判断是否命中
//        if (StringUtils.isBlank(shopJson)) {
//            // 2.1 未命中，返回null
//            return null;
//        }
//        // 2.2 命中，需要判断缓存是否过期
//
//        // 3. 把Json反序列化为对象
//        RedisData redisData = JSONUtil.toBean(shopJson, RedisData.class);
//        // 注意：JSON反序列化依赖于字节码，字节码是Object类型，不能指定是Shop，因为还可能缓存其他数据
//        JSONObject jsonObject = (JSONObject) redisData.getData();
//        // 使用这个工具类转换成Shop类型的变量
//        Shop shop = BeanUtil.toBean(jsonObject, Shop.class);
//
//        LocalDateTime expireTime = redisData.getExpireTime();
//
//        // 4. 再判断是否过期
//        if (expireTime.isAfter(LocalDateTime.now())) {
//            // 4.1 未过期 直接返回
//            return shop;
//        }
//        // 4.2 已过期，需要缓存重建
//        // 5. 获取互斥锁
//        String lockKey = LOCK_SHOP_KEY + id;
//        boolean isLock = tryLock(lockKey);
//        // 6 判断是否获取锁成功
//        if (isLock) {
//            // 6.1 成功，开启独立线程缓存重建
//            // 这里其实也要做一个DoubleCheck，缓存存在则无需重建缓存
//
//            // 使用线程池，不用反复创建和销毁线程
//            CACHE_REBUILD_EXECUTOR.submit(() -> {
//                try {
//                    this.saveShop2Redis(id, 20L);
//                } catch (Exception e) {
//                    throw new RuntimeException(e);
//                } finally {
//                    //释放锁
//                    unLock(lockKey);
//                }
//            });
//        }
//        // 6.2 获取失败，直接返回过期商铺信息
//        return shop;
//        }

    /**
     * 获取锁（有工具类后被取代）
     * private boolean tryLock(String key)
     */
//    private boolean tryLock(String key) {
//        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
//        // 拆箱要判空，防止空指针异常
//        return BooleanUtil.isTrue(flag);
//    }

    /**
     * 释放锁（有工具类后被取代）
     * private void unLock(String key)
     */
//    private void unLock(String key) {
//        stringRedisTemplate.delete(key);
//    }

    /**
     * 将热点数据保存到缓存中
     */
    public void saveShop2Redis(Long id, Long expireSeconds) throws InterruptedException {
        // 1. 从数据库中查询店铺数据
        Shop shop = this.getById(id);
        Thread.sleep(500L); // 缓存延时
        // 2. 封装逻辑过期数据
        RedisData redisData = new RedisData();
        redisData.setData(shop);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));
        // 3. 将逻辑过期数据存入Redis中
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(redisData));
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result update(Shop shop) {
        Long id = shop.getId();
        if(id == null){
            return Result.fail("店铺id不能为null");
        }
        // 先更新，再删除缓存
        updateById(shop);
        stringRedisTemplate.delete(CACHE_SHOP_KEY + shop.getId());
        return Result.ok();
    }
}
