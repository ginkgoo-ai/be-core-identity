package com.benwk.ginkgoocoreidentity.service.verification;

import com.benwk.ginkgoocoreidentity.enums.VerificationStrategy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class EmailVerificationStrategyFactory {
    private final Map<VerificationStrategy, EmailVerificationStrategy> strategies;

    public EmailVerificationStrategyFactory(List<EmailVerificationStrategy> strategyList) {
        strategies = strategyList.stream()
            .collect(Collectors.toMap(
                EmailVerificationStrategy::getType,
                strategy -> strategy
            ));
    }

    /**
     * Get verification strategy by type
     *
     * @param type Verification strategy type
     * @return Corresponding strategy implementation
     */
    public EmailVerificationStrategy getStrategy(VerificationStrategy type) {
        EmailVerificationStrategy strategy = strategies.get(type);
        if (strategy == null) {
            throw new IllegalArgumentException("Unsupported verification strategy: " + type);
        }
        return strategy;
    }
}