-- KEYS[1]: stock_key (Tồn kho của sản phẩm)
-- KEYS[2]: user_bought_key (Đánh dấu user đã mua sản phẩm này chưa)
-- ARGV[1]: quantity (Số lượng muốn mua)

local stock_key = KEYS[1]
local user_bought_key = KEYS[2]
local quantity = tonumber(ARGV[1])

-- 1. Anti-Cheat: Kiểm tra xem user này đã mua/giữ chỗ sản phẩm này chưa?
if redis.call("EXISTS", user_bought_key) == 1 then
    return -1 -- Trả về -1: Lỗi đã mua rồi
end

-- 2. Lấy số lượng tồn kho hiện tại
local current_stock = tonumber(redis.call("GET", stock_key))

-- 3. Kiểm tra xem còn hàng không?
if current_stock and current_stock >= quantity then
    -- TRỪ KHO (Atomic)
    redis.call("DECRBY", stock_key, quantity)

    -- ĐÁNH DẤU USER ĐÃ MUA (Khóa trong 5 phút = 300 giây)
    -- Nếu sau 5 phút không thanh toán, hệ thống hoàn kho và xóa key này
    redis.call("SETEX", user_bought_key, 300, "1")

    return 1 -- Trả về 1: Thành công
else
    return 0 -- Trả về 0: Hết hàng
end