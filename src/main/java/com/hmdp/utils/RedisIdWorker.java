package com.hmdp.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * redis ID生成器
 */
@Component
public class RedisIdWorker {

    // 初始时间戳
    private static final Long BEGIN_TIMESTAMP = 1640995200L; // 开始的时间戳，2022-01-01 00:00 对应的秒数

    // 序列号位数
    private static final Integer COUNT_BITS = 32;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 获取id
     *
     * @param keyPrefix 前缀区分不同业务
     * @return {@link Long}
     */
    public Long nextId(String keyPrefix) {
        // 1. 生成时间戳
        LocalDateTime now = LocalDateTime.now();
        long nowSecond = now.toEpochSecond(ZoneOffset.UTC);
        long timestamp = nowSecond - BEGIN_TIMESTAMP;   // 现在时间和初始时间戳的时间差

        // 2. 生成序列号
        // 生成当前日期 精确到天
        String today = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        // 自增长，设置自增的key（业务名称+当天日期）
        Long count = stringRedisTemplate.opsForValue().increment("icr:" + keyPrefix + ":" + today);

        // 拼接时间戳与序列号并返回（返回结果是long，不能使用字符串拼接，要挪动）
        return timestamp << COUNT_BITS|count ;
    }

}