package com.eskcti.algashop.billingscheduler.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.eskcti.algashop.billingscheduler.application.CancelExpiredInvoicesApplicationService;
import com.eskcti.algashop.billingscheduler.utils.TestcontainerPostgreSQLConfig;

@SpringBootTest
@ActiveProfiles("it")
@Import(TestcontainerPostgreSQLConfig.class)
class CancelExpiredInvoicesApplicationServiceJDBCImplIT {

        private static final String UNPAID = "UNPAID";
        private static final String CANCELED = "CANCELED";
        private static final String CANCEL_REASON = "Invoice expired";

        @Autowired
        private CancelExpiredInvoicesApplicationService applicationService;

        @Autowired
        private JdbcTemplate jdbcTemplate;

        @MockitoBean
        private FastpayPaymentAPIClient fastpayPaymentAPIClient;

        @BeforeEach
        void cleanDatabase() {
                jdbcTemplate.update("delete from invoice");
                jdbcTemplate.update("delete from payment_settings");
        }

        @Test
        void cancelExpiredInvoices_cancelsOnlyExpiredUnpaidInvoices() {
                UUID paymentSettingsId = insertPaymentSettings("gateway-1");
                UUID expiredUnpaid = insertInvoice(UNPAID, OffsetDateTime.now().minusDays(2),
                                paymentSettingsId);
                UUID recentUnpaid = insertInvoice(UNPAID, OffsetDateTime.now().plusDays(1),
                                paymentSettingsId);
                UUID expiredPaid = insertInvoice("PAID", OffsetDateTime.now().minusDays(2),
                                paymentSettingsId);

                applicationService.cancelExpiredInvoices();

                assertThat(findInvoice(expiredUnpaid))
                                .containsEntry("status", CANCELED)
                                .containsEntry("cancel_reason", CANCEL_REASON);
                assertThat(findInvoice(expiredUnpaid).get("canceled_at")).isNotNull();
                assertThat(findInvoice(recentUnpaid)).containsEntry("status", UNPAID);
                assertThat(findInvoice(expiredPaid)).containsEntry("status", "PAID");
                verifyNoInteractions(fastpayPaymentAPIClient);
        }

        @Test
        void cancelExpiredInvoicesPayments_cancelsOnGatewayAndMarksCanceled() {
                UUID firstPaymentSettings = insertPaymentSettings("gateway-1");
                UUID secondPaymentSettings = insertPaymentSettings("gateway-2");
                UUID firstInvoice = insertInvoice(UNPAID, OffsetDateTime.now().minusDays(2),
                                firstPaymentSettings);
                UUID secondInvoice = insertInvoice(UNPAID, OffsetDateTime.now().minusDays(2),
                                secondPaymentSettings);

                applicationService.cancelExpiredInvoicesPayments();

                verify(fastpayPaymentAPIClient).cancel("gateway-1");
                verify(fastpayPaymentAPIClient).cancel("gateway-2");
                assertThat(findInvoice(firstInvoice))
                                .containsEntry("status", CANCELED)
                                .containsEntry("cancel_reason", CANCEL_REASON);
                assertThat(findInvoice(secondInvoice))
                                .containsEntry("status", CANCELED)
                                .containsEntry("cancel_reason", CANCEL_REASON);
        }

        @Test
        void cancelExpiredInvoicesPayments_whenGatewayFails_invoiceRemainsUnpaid() {
                UUID validPaymentSettings = insertPaymentSettings("gateway-1");
                UUID failingPaymentSettings = insertPaymentSettings("gateway-2");
                UUID validInvoice = insertInvoice(UNPAID, OffsetDateTime.now().minusDays(2),
                                validPaymentSettings);
                UUID failingInvoice = insertInvoice(UNPAID, OffsetDateTime.now().minusDays(2),
                                failingPaymentSettings);
                doThrow(new IllegalStateException("gateway unavailable"))
                                .when(fastpayPaymentAPIClient).cancel("gateway-2");

                applicationService.cancelExpiredInvoicesPayments();

                assertThat(findInvoice(validInvoice)).containsEntry("status", CANCELED);
                assertThat(findInvoice(failingInvoice)).containsEntry("status", UNPAID);
                assertThat(findInvoice(failingInvoice).get("canceled_at")).isNull();
        }

        @Test
        void cancelExpiredInvoicesPayments_whenNoExpiredInvoices_doesNothing() {
                applicationService.cancelExpiredInvoicesPayments();

                verifyNoInteractions(fastpayPaymentAPIClient);
        }

        private UUID insertPaymentSettings(String gatewayCode) {
                UUID id = UUID.randomUUID();
                jdbcTemplate.update("insert into payment_settings (id, gateway_code) values (?, ?)",
                                id, gatewayCode);
                return id;
        }

        private UUID insertInvoice(String status, OffsetDateTime expiresAt, UUID paymentSettingsId) {
                UUID id = UUID.randomUUID();
                jdbcTemplate.update(
                                "insert into invoice (id, status, expires_at, payment_settings_id) values (?, ?, ?, ?)",
                                id, status, expiresAt, paymentSettingsId);
                return id;
        }

        private Map<String, Object> findInvoice(UUID invoiceId) {
                return jdbcTemplate.queryForMap(
                                "select status, canceled_at, cancel_reason from invoice where id = ?",
                                invoiceId);
        }
}
