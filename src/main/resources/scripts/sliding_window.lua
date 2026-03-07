-- Sliding Window Log Algorithm (collision-proof)
-- KEYS[1]: limit key
-- ARGV[1]: window size in seconds
-- ARGV[2]: max requests (limit)
-- ARGV[3]: unique member ID (UUID from Java — guarantees uniqueness)
-- ARGV[4]: current timestamp (seconds, for window calculation)

local key = KEYS[1]
local window = tonumber(ARGV[1])
local limit = tonumber(ARGV[2])
local uniqueId = ARGV[3]
local now_sec = tonumber(ARGV[4])

-- Remove elements outside the window
local clearBefore = now_sec - window
redis.call('ZREMRANGEBYSCORE', key, 0, clearBefore)

-- Count current elements
local count = redis.call('ZCARD', key)

local allowed = 0
if count < limit then
    -- Use UUID as member → zero collisions even at same nanosecond
    redis.call('ZADD', key, now_sec, uniqueId)
    allowed = 1
end

-- Set expiry to window size + 1 to auto-cleanup
redis.call('EXPIRE', key, window + 1)

return allowed

