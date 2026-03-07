package com.flowguard.service;

import com.flowguard.model.NetworkRule;
import com.flowguard.repository.NetworkRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NetworkRuleServiceTest {

    @Mock
    private NetworkRuleRepository networkRuleRepository;

    private NetworkRuleService service;

    @BeforeEach
    void setUp() {
        service = new NetworkRuleService(networkRuleRepository);
    }

    @Test
    @DisplayName("Should allow any IP when no rules exist")
    void allowsAllIpsWhenNoRules() {
        when(networkRuleRepository.findByIsActiveTrueOrderByPriorityDesc())
                .thenReturn(Collections.emptyList());
        assertTrue(service.isAllowed("192.168.1.1"));
    }

    @Test
    @DisplayName("Should deny IP matching a DENY rule")
    void deniesMatchingDenyRule() {
        NetworkRule rule = NetworkRule.builder()
                .name("Block internal")
                .cidrBlock("192.168.1.0/24")
                .ruleType(NetworkRule.RuleType.DENY)
                .priority(100)
                .isActive(true)
                .build();

        when(networkRuleRepository.findByIsActiveTrueOrderByPriorityDesc())
                .thenReturn(List.of(rule));

        assertFalse(service.isAllowed("192.168.1.50"));
    }

    @Test
    @DisplayName("Should allow IP matching an ALLOW rule")
    void allowsMatchingAllowRule() {
        NetworkRule rule = NetworkRule.builder()
                .name("Allow office")
                .cidrBlock("10.0.0.0/8")
                .ruleType(NetworkRule.RuleType.ALLOW)
                .priority(100)
                .isActive(true)
                .build();

        when(networkRuleRepository.findByIsActiveTrueOrderByPriorityDesc())
                .thenReturn(List.of(rule));

        assertTrue(service.isAllowed("10.0.0.1"));
    }

    @Test
    @DisplayName("Should allow IP that doesn't match any rule")
    void allowsNonMatchingIp() {
        NetworkRule rule = NetworkRule.builder()
                .name("Block internal")
                .cidrBlock("192.168.1.0/24")
                .ruleType(NetworkRule.RuleType.DENY)
                .priority(100)
                .isActive(true)
                .build();

        when(networkRuleRepository.findByIsActiveTrueOrderByPriorityDesc())
                .thenReturn(List.of(rule));

        assertTrue(service.isAllowed("172.16.0.1"));
    }

    @Test
    @DisplayName("Should handle exact IP (single host CIDR /32)")
    void handlesSingleHostCidr() {
        NetworkRule rule = NetworkRule.builder()
                .name("Block specific")
                .cidrBlock("1.2.3.4/32")
                .ruleType(NetworkRule.RuleType.DENY)
                .priority(100)
                .isActive(true)
                .build();

        when(networkRuleRepository.findByIsActiveTrueOrderByPriorityDesc())
                .thenReturn(List.of(rule));

        assertFalse(service.isAllowed("1.2.3.4"));
        assertTrue(service.isAllowed("1.2.3.5"));
    }
}
