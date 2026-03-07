package com.flowguard.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@Profile("dev")
public class InMemoryAbuseDetectionService implements AbuseDetectionService {

    private final Map<String, Integer> failedLogins = new ConcurrentHashMap<>();
    private final Map<String, Long> blockedIps = new ConcurrentHashMap<>();

    @Override
    public void detectAbuse() {
        log.info("[Dev] Mock abuse detection scan...");
    }

    @Override
    public void reportFailedLogin(String ip) {
        int attempts = failedLogins.merge(ip, 1, Integer::sum);
        if (attempts >= 5) {
            blockIp(ip);
            failedLogins.remove(ip);
        }
    }

    @Override
    public void blockIp(String ip) {
        log.warn("[Dev] Blocking IP: {}", ip);
        blockedIps.put(ip, System.currentTimeMillis() + 300000); // 5 min
    }

    @Override
    public boolean isIpBlocked(String ip) {
        Long expiry = blockedIps.get(ip);
        if (expiry != null) {
            if (System.currentTimeMillis() > expiry) {
                blockedIps.remove(ip);
                return false;
            }
            return true;
        }
        return false;
    }
}
