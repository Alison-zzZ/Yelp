-- 1. 参数列表：
--    ① 优惠券id
local voucherId = ARGV[1]
--    ② 用户id
local userId = ARGV[2]

-- 2. 数据key：
--    ① 库存key
local stockKey = 'seckill:stock:' .. voucherId
--    ② 订单key
local orderKey = 'seckill:order:' .. voucherId

-- 3. 脚本业务逻辑
-- ① 判断库存是否充足 get stockKey
if(tonumber(redis.call('get', stockKey)) <= 0) then
    -- 库存不足，返回1
    return 1
end
-- ② 判断用户是否下单 SISMEMBER key member：判断member是否是当前set集合（key）中的成员
if(redis.call('sismember', orderKey, userId) == 1)then
    -- 存在，说明重复下单
    return 2
end
-- ③ 扣除库存 incrby stockKey -1
redis.call('incrby', stockKey, -1)
-- ④ 当前用户id保存到set集合 sadd orderKey userId
redis.call('sadd', orderKey, userId)
return 0