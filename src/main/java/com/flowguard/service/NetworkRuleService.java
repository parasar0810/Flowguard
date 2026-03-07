package com.flowguard.service;

import com.flowguard.model.NetworkRule;
import com.flowguard.repository.NetworkRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NetworkRuleService {

    private final NetworkRuleRepository networkRuleRepository;

    /**
     * Checks if the given IP address is allowed by the configured network rules.
     *
     * Logic:
     * - If no rules exist → allow everything (safe default)
     * - Rules are sorted by priority (highest first)
     * - First matching rule wins (ALLOW or DENY)
     * - If no rule matches → default ALLOW
     */
    @Cacheable(value = "networkRulesResult", key = "#ipAddress")
    public boolean isAllowed(String ipAddress) {
        List<NetworkRule> rules = networkRuleRepository.findByIsActiveTrueOrderByPriorityDesc();
        if (rules.isEmpty())
            return true;

        for (NetworkRule rule : rules) {
            if (matchesCidr(ipAddress, rule.getCidrBlock())) {
                boolean allowed = rule.getRuleType() == NetworkRule.RuleType.ALLOW;
                log.debug("IP {} matched rule '{}' ({}): {}",
                        ipAddress, rule.getName(), rule.getRuleType(), allowed ? "ALLOWED" : "DENIED");
                return allowed;
            }
        }
        return true; // No matching rule → allow by default
    }

    public boolean matchesCidr(String ipAddress, String cidr) {
        try {
            String[] parts = cidr.split("/");
            InetAddress network = InetAddress.getByName(parts[0]);
            int prefixLen = parts.length > 1 ? Integer.parseInt(parts[1]) : 32;

            byte[] networkBytes = network.getAddress();
            byte[] ipBytes = InetAddress.getByName(ipAddress).getAddress();
            if (networkBytes.length != ipBytes.length)
                return false;

            int mask = prefixLen == 0 ? 0 : (0xFFFFFFFF << (32 - prefixLen));
            int networkInt = toInt(networkBytes);
            int ipInt = toInt(ipBytes);

            return (networkInt & mask) == (ipInt & mask);
        } catch (UnknownHostException | NumberFormatException e) {
            log.warn("Invalid CIDR rule: {}", cidr);
            return false;
        }
    }

    private int toInt(byte[] bytes) {
        int result = 0;
        for (byte b : bytes)
            result = (result << 8) | (b & 0xFF);
        return result;
    }

    public List<NetworkRule> getAllRules() {
        return networkRuleRepository.findAll();
    }

    @CacheEvict(value = "networkRulesResult", allEntries = true)
    public NetworkRule createRule(NetworkRule rule) {
        return networkRuleRepository.save(rule);
    }

    @CacheEvict(value = "networkRulesResult", allEntries = true)
    public Optional<NetworkRule> updateRule(Long id, NetworkRule updated) {
        return networkRuleRepository.findById(id).map(existing -> {
            existing.setName(updated.getName());
            existing.setCidrBlock(updated.getCidrBlock());
            existing.setRuleType(updated.getRuleType());
            existing.setDescription(updated.getDescription());
            existing.setPriority(updated.getPriority());
            existing.setActive(updated.isActive());
            return networkRuleRepository.save(existing);
        });
    }

    @CacheEvict(value = "networkRulesResult", allEntries = true)
    public void deleteRule(Long id) {
        networkRuleRepository.deleteById(id);
    }
}
