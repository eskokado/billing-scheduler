package com.eskcti.algashop.billingscheduler.infrastructure;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;

import com.eskcti.algashop.billingscheduler.application.CancelExpiredInvoicesApplicationService;

@ExtendWith(MockitoExtension.class)
class CancelExpiredInvoicesRunnerTest {

    @Mock
    private CancelExpiredInvoicesApplicationService applicationService;

    @InjectMocks
    private CancelExpiredInvoicesRunner runner;

    @Test
    void run_cancelsExpiredInvoicePayments() throws Exception {
        runner.run(mock(ApplicationArguments.class));

        verify(applicationService).cancelExpiredInvoicesPayments();
        verify(applicationService, never()).cancelExpiredInvoices();
    }
}
