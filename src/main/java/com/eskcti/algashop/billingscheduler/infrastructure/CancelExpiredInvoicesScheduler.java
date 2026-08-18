package com.eskcti.algashop.billingscheduler.infrastructure;

import com.eskcti.algashop.billingscheduler.application.CancelExpiredInvoicesApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// @Component
@RequiredArgsConstructor
@Slf4j
public class CancelExpiredInvoicesScheduler {

    private final CancelExpiredInvoicesApplicationService applicationService;

    // @Scheduled(fixedRate = 60000) // a cada 1 minuto
    public void runTask() {
        log.info("Task started - Cancelling expired invoices.");
        applicationService.cancelExpiredInvoices();
        log.info("Task ended - Expired invoices.");
    }

}
