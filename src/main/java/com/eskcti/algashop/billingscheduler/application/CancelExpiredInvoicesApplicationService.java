package com.eskcti.algashop.billingscheduler.application;

public interface CancelExpiredInvoicesApplicationService {
    void cancelExpiredInvoices();

    void cancelExpiredInvoicesPayments();
}
