package com.eskcti.algashop.billingscheduler.infrastructure;

import com.eskcti.algashop.billingscheduler.application.CancelExpiredInvoicesApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CancelExpiredInvoicesRunner implements ApplicationRunner {

    private final CancelExpiredInvoicesApplicationService applicationService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("Task started - Cancelling expired invoices.");
        // applicationService.cancelExpiredInvoices();
        applicationService.cancelExpiredInvoicesPayments();
        log.info("Task ended - Expired invoices payments canceled.");
    }
}
