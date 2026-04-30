package com.example.multi_stores.service;

import com.example.multi_stores.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class PendingVerificationCleanupService {

    private static final Logger log = LoggerFactory.getLogger(PendingVerificationCleanupService.class);
    private final UserRepository userRepository;

    public PendingVerificationCleanupService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    @Scheduled(fixedDelayString = "${app.verification.cleanup-interval-ms:900000}")
    public void deleteExpiredUnverifiedUsers() {
        long deleted = userRepository.deleteByEmailVerifiedFalseAndVerificationTokenExpiryBefore(Instant.now());
        if (deleted > 0) {
            log.info("Deleted {} expired unverified user(s).", deleted);
        }
    }
}
