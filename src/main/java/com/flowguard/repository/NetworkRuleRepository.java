package com.flowguard.repository;

import com.flowguard.model.NetworkRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NetworkRuleRepository extends JpaRepository<NetworkRule, Long> {
    List<NetworkRule> findByIsActiveTrueOrderByPriorityDesc();
}
