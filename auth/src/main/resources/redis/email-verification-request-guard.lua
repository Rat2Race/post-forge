-- KEYS:
--   KEYS[1]: email send lock key
--   KEYS[2]: email send rate count key
--   KEYS[3]: email send cooldown key
--
-- ARGV:
--   ARGV[1]: rate limit window seconds
--   ARGV[2]: request limit per window
--   ARGV[3]: lock seconds after rate limit
--   ARGV[4]: cooldown seconds between requests

if redis.call("EXISTS", KEYS[1]) == 1 then
  return "LOCKED"
end

local count = redis.call("INCR", KEYS[2])
if count == 1 then
  redis.call("EXPIRE", KEYS[2], ARGV[1])
end

if count > tonumber(ARGV[2]) then
  redis.call("SET", KEYS[1], "1", "EX", ARGV[3])
  redis.call("DEL", KEYS[2])
  return "RATE_LIMITED"
end

local cooldownSet = redis.call("SET", KEYS[3], "1", "EX", ARGV[4], "NX")
if not cooldownSet then
  return "COOLDOWN"
end

return "ALLOWED"
