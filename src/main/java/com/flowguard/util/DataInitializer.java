package com.flowguard.util;

import com.flowguard.model.Plan;
import com.flowguard.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final PlanRepository planRepository;

    @Override
    public void run(String... args) throws Exception {
        if (planRepository.count() == 0) {
            log.info("Seeding default plans...");
            Plan free = Plan.builder()
                    .name("FREE")
                    .requests(50)
                    .timeWindowSeconds(60)
                    .build();

            Plan pro = Plan.builder()
                    .name("PRO")
                    .requests(500)
                    .timeWindowSeconds(60)
                    .build();

            Plan enterprise = Plan.builder()
                    .name("ENTERPRISE")
                    .requests(2000) // Configurable in real scenario
                    .timeWindowSeconds(60)
                    .build();

            Plan demo = Plan.builder()
                    .name("DEMO")
                    .requests(300) // Enough for 200-request burst demo
                    .timeWindowSeconds(60)
                    .build();

            planRepository.saveAll(Arrays.asList(free, pro, enterprise, demo));
            log.info("Default plans seeded.");
        }
    }
}
