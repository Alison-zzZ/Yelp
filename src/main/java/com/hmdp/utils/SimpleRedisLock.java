package com.hmdp.utils;

import cn.hutool.core.lang.UUID;
//import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.concurrent.TimeUnit;

/**
 * 简单的redis分布式锁
 *
 */
public class SimpleRedisLock implements ILock {
    private String name;    // 业务名称
    private StringRedisTemplate stringRedisTemplate;
    private static final String KET_PREFIX="lock:"; // 锁前缀
    private static final String ID_PREFIX= UUID.randomUUID().toString(true)+"-";
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;

    static {
        UNLOCK_SCRIPT=new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("unlock.lua"));
        UNLOCK_SCRIPT.setResultType(Long.class);
    }

    // 使用构造器传参
    public SimpleRedisLock(String name, StringRedisTemplate stringRedisTemplate) {
        this.name = name;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean tryLock(long timeoutSec) {
        // 获取线程标识
        String threadId = ID_PREFIX + Thread.currentThread().getId();

        //获取锁
        Boolean isSuccess = stringRedisTemplate
                .opsForValue()
                .setIfAbsent(KET_PREFIX + name // key: 前缀拼接业务名称
                        , threadId             // value: 要加上线程标识
                        , timeoutSec
                        , TimeUnit.SECONDS); // setIfAbsent - setnx
        // 要从Boolean自动拆箱为boolean，自动拆箱有安全风险
        // 避免自动拆箱引发空指针异常
        return Boolean.TRUE.equals(isSuccess);
    }

    @Override
    public void unlock() {
        //获取线程标识
        String threadId =ID_PREFIX+ Thread.currentThread().getId();
        //获取锁中标识
        String id = stringRedisTemplate.opsForValue().get(KET_PREFIX + name);
        //判断是否一致
        if (StringUtils.equals(id,threadId)){
            //一致 释放锁
            stringRedisTemplate.delete(KET_PREFIX + name);
        }
    }

//    @Override
//    public void unLock() {
//        //获取线程标识
//        String threadId =ID_PREFIX+ Thread.currentThread().getId();
//        //获取锁中标识
//        String id = stringRedisTemplate.opsForValue().get(KET_PREFIX + name);
//        //判断是否一致
//        if (StringUtils.equals(id,threadId)){
//            //一致 释放锁
//            stringRedisTemplate.delete(KET_PREFIX + name);
//        }
//        //使用lua脚本保证操作原子性
//        stringRedisTemplate.execute(UNLOCK_SCRIPT
//                , Collections.singletonList(KET_PREFIX+name)
//                ,ID_PREFIX+Thread.currentThread().getId());
//    }
}