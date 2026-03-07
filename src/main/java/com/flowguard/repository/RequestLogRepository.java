package com.flowguard.repository;

import com.flowguard.model.RequestLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RequestLogRepository extends JpaRepository<RequestLog, Long> {

    List<RequestLog> findByIpAddressAndTimestampAfter(String ipAddress, LocalDateTime timestamp);

    long countByBlocked(boolean blocked);

    List<RequestLog> findTop50ByOrderByTimestampDesc();

    @Query("SELECT r.ipAddress, COUNT(r) as cnt FROM RequestLog r GROUP BY r.ipAddress ORDER BY cnt DESC")
    List<Object[]> findTopIpAddresses();

    @Query("SELECT r.method, COUNT(r) as cnt FROM RequestLog r GROUP BY r.method ORDER BY cnt DESC")
    List<Object[]> findRequestsByMethod();

    List<RequestLog> findByTimestampAfter(LocalDateTime timestamp);

    List<RequestLog> findByTimestampBetween(LocalDateTime from, LocalDateTime to);

    @Query("SELECT r.requestUri, COUNT(r) as cnt FROM RequestLog r GROUP BY r.requestUri ORDER BY cnt DESC")
    List<Object[]> findTopEndpoints();

    @Query("SELECT r.ipAddress, COUNT(r) as cnt FROM RequestLog r WHERE r.blocked = true GROUP BY r.ipAddress ORDER BY cnt DESC")
    List<Object[]> findTopBlockedIpAddresses();

    @Query("SELECT AVG(r.processingTimeMs) FROM RequestLog r WHERE r.timestamp > :since")
    Double findAvgProcessingTime(@Param("since") LocalDateTime since);

    @Query("SELECT r.blockReason, COUNT(r) as cnt FROM RequestLog r WHERE r.blocked = true GROUP BY r.blockReason ORDER BY cnt DESC")
    List<Object[]> findBlockReasonBreakdown();
}
