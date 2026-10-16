package com.eskcti.algashop.billingscheduler.infrastructure;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.ParameterizedPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
class CancelExpiredInvoicesApplicationServiceJDBCImplTest {

  private static final String UNPAID = "UNPAID";
  private static final int BATCH_LIMIT = 50;

  @Mock
  private JdbcOperations jdbcOperations;

  @Mock
  private TransactionTemplate transactionTemplate;

  @Mock
  private FastpayPaymentAPIClient fastpayPaymentAPIClient;

  @InjectMocks
  private CancelExpiredInvoicesApplicationServiceJDBCImpl service;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    when(transactionTemplate.execute(any())).thenAnswer(invocation -> invocation
        .getArgument(0, TransactionCallback.class)
        .doInTransaction(null));
  }

  @Test
  void cancelExpiredInvoices_whenNoneFound_doesNotUpdate() {
    stubQuery(List.of());

    service.cancelExpiredInvoices();

    verify(jdbcOperations, never())
        .batchUpdate(anyString(), ArgumentMatchers.<java.util.Collection<UUID>>any(), anyInt(),
            any(ParameterizedPreparedStatementSetter.class));
  }

  @Test
  void cancelExpiredInvoices_cancelsInBatch() {
    UUID first = UUID.randomUUID();
    UUID second = UUID.randomUUID();
    stubQuery(List.of(first, second));

    service.cancelExpiredInvoices();

    verify(jdbcOperations).batchUpdate(anyString(), eq(List.of(first, second)), eq(2),
        any(ParameterizedPreparedStatementSetter.class));
  }

  @Test
  void cancelExpiredInvoices_whenBatchFails_doesNotPropagate() {
    UUID invoiceId = UUID.randomUUID();
    stubQuery(List.of(invoiceId));
    stubBatchUpdateFailure();

    service.cancelExpiredInvoices();

    verify(jdbcOperations).batchUpdate(anyString(), eq(List.of(invoiceId)), eq(1),
        any(ParameterizedPreparedStatementSetter.class));
  }

  @Test
  void cancelExpiredInvoices_setsStatusAndLimitOnStatement() throws Exception {
    stubQuery(List.of());
    ArgumentCaptor<PreparedStatementSetter> setterCaptor = ArgumentCaptor
        .forClass(PreparedStatementSetter.class);
    ArgumentCaptor<RowMapper> mapperCaptor = ArgumentCaptor.forClass(RowMapper.class);

    service.cancelExpiredInvoices();

    verify(jdbcOperations).query(anyString(), setterCaptor.capture(), mapperCaptor.capture());

    PreparedStatement statement = mock(PreparedStatement.class);
    setterCaptor.getValue().setValues(statement);
    verify(statement).setString(1, UNPAID);
    verify(statement).setInt(2, BATCH_LIMIT);
    org.assertj.core.api.Assertions.assertThat(mapperCaptor.getValue()).isNotNull();
  }

  @Test
  void cancelExpiredInvoicesPayments_whenNoneFound_doesNotCallGatewayNorUpdate() {
    stubQuery(List.of());

    service.cancelExpiredInvoicesPayments();

    verifyNoInteractions(fastpayPaymentAPIClient);
    verify(jdbcOperations, never())
        .batchUpdate(anyString(), ArgumentMatchers.<java.util.Collection<InvoiceProjection>>any(), anyInt(),
            any(ParameterizedPreparedStatementSetter.class));
  }

  @Test
  void cancelExpiredInvoicesPayments_cancelsOnGatewayAndUpdatesInvoices() {
    InvoiceProjection first = new InvoiceProjection(UUID.randomUUID(), "gateway-1");
    InvoiceProjection second = new InvoiceProjection(UUID.randomUUID(), "gateway-2");
    stubQuery(List.of(first, second));

    service.cancelExpiredInvoicesPayments();

    verify(fastpayPaymentAPIClient).cancel("gateway-1");
    verify(fastpayPaymentAPIClient).cancel("gateway-2");
    verify(jdbcOperations).batchUpdate(anyString(), eq(List.of(first, second)), eq(2),
        any(ParameterizedPreparedStatementSetter.class));
  }

  @Test
  void cancelExpiredInvoicesPayments_whenGatewayFails_skipsInvoiceOnUpdate() {
    InvoiceProjection valid = new InvoiceProjection(UUID.randomUUID(), "gateway-1");
    InvoiceProjection failing = new InvoiceProjection(UUID.randomUUID(), "gateway-2");
    stubQuery(List.of(valid, failing));
    Mockito.lenient().doThrow(new IllegalStateException("gateway unavailable"))
        .when(fastpayPaymentAPIClient).cancel("gateway-2");

    service.cancelExpiredInvoicesPayments();

    verify(jdbcOperations).batchUpdate(anyString(), eq(List.of(valid)), eq(1),
        any(ParameterizedPreparedStatementSetter.class));
  }

  @Test
  void cancelExpiredInvoicesPayments_whenAllGatewayCallsFail_doesNotUpdate() {
    InvoiceProjection failing = new InvoiceProjection(UUID.randomUUID(), "gateway-2");
    stubQuery(List.of(failing));
    doThrow(new IllegalStateException("gateway unavailable"))
        .when(fastpayPaymentAPIClient).cancel("gateway-2");

    service.cancelExpiredInvoicesPayments();

    verify(jdbcOperations).batchUpdate(anyString(), eq(List.of()), eq(0),
        any(ParameterizedPreparedStatementSetter.class));
  }

  @Test
  void cancelExpiredInvoicesPayments_whenUpdateFails_doesNotPropagate() {
    InvoiceProjection invoice = new InvoiceProjection(UUID.randomUUID(), "gateway-1");
    stubQuery(List.of(invoice));
    stubBatchUpdateFailure();

    service.cancelExpiredInvoicesPayments();

    verify(fastpayPaymentAPIClient).cancel("gateway-1");
  }

  private void stubBatchUpdateFailure() {
    when(jdbcOperations.batchUpdate(anyString(), ArgumentMatchers.any(), anyInt(),
        any(ParameterizedPreparedStatementSetter.class)))
        .thenThrow(new DataAccessResourceFailureException("database unavailable"));
  }

  @SuppressWarnings("unchecked")
  private void stubQuery(List<?> rows) {
    when(jdbcOperations.query(anyString(), any(PreparedStatementSetter.class), any(RowMapper.class)))
        .thenReturn((List<UUID>) rows);
  }
}
