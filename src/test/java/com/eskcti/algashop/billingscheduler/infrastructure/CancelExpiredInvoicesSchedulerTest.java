package com.eskcti.algashop.billingscheduler.infrastructure;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.eskcti.algashop.billingscheduler.application.CancelExpiredInvoicesApplicationService;

@ExtendWith(MockitoExtension.class)
class CancelExpiredInvoicesSchedulerTest {

    @Mock
    private CancelExpiredInvoicesApplicationService applicationService;

    @InjectMocks
    private CancelExpiredInvoicesScheduler scheduler;

    @Test
    void runTask_cancelsExpiredInvoices() {
        scheduler.runTask();

        verify(applicationService).cancelExpiredInvoices();
    }
}
