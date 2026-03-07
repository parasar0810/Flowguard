package com.flowguard.service;

public interface AbuseDetectionService {
    void detectAbuse(); // Scheduled
    void reportFailedLogin(String ip);
    void blockIp(String ip);
    boolean isIpBlocked(String ip);
}
