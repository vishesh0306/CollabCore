package com.collabflow.audit;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/** Beans only the audit tests need. Classes nested in a test aren't scanned, so they live here. */
@TestConfiguration
class AuditTestBeans {

    @Bean
    FailingWork failingWork(AuditService auditService) {
        return new FailingWork(auditService);
    }
}
