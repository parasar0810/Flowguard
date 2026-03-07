-- Token Bucket Algorithm
-- KEYS[1]: bucket key
-- ARGV[1]: max tokens (capacity)
-- ARGV[2]: refill rate (tokens per second)
-- ARGV[3]: time window (for logical refill calc, usually 1s or matches window, simplified here as direct rate)
-- ARGV[4]: current timestamp (seconds)

local key = KEYS[1]
local capacity = tonumber(ARGV[1])
local rate = tonumber(ARGV[2]) -- tokens to add per second
local now = tonumber(ARGV[4])
local requested = 1

-- keys for tracking
local tokens_key = key .. ":tokens"
local timestamp_key = key .. ":ts"

local last_refill = tonumber(redis.call("get", timestamp_key))
local tokens = tonumber(redis.call("get", tokens_key))

if last_refill == nil then
  tokens = capacity
  last_refill = now
end

-- Refill tokens based on time passed
local delta = math.max(0, now - last_refill)
local refill = delta * rate
tokens = math.min(capacity, tokens + refill)

-- Consume token
local allowed = 0
if tokens >= requested then
  tokens = tokens - requested
  allowed = 1
end

-- Save state
redis.call("set", tokens_key, tokens)
redis.call("set", timestamp_key, now)
-- Expire keys to avoid clutter (e.g. 1 hour idle)
redis.call("expire", tokens_key, 3600)
redis.call("expire", timestamp_key, 3600)

return allowed
