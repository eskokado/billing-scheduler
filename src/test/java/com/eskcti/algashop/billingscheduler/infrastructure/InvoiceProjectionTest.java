package com.eskcti.algashop.billingscheduler.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class InvoiceProjectionTest {

    @Test
    void shouldExposeGettersAndSetters() {
        UUID id = UUID.randomUUID();
        InvoiceProjection projection = new InvoiceProjection();

        projection.setId(id);
        projection.setPaymentGatewayCode("gateway-123");

        assertThat(projection.getId()).isEqualTo(id);
        assertThat(projection.getPaymentGatewayCode()).isEqualTo("gateway-123");
        assertThat(new InvoiceProjection(id, "gateway-123"))
                .usingRecursiveComparison()
                .isEqualTo(projection);
    }

    @Test
    void shouldImplementEqualsHashCodeAndToString() {
        UUID id = UUID.randomUUID();
        InvoiceProjection projection = new InvoiceProjection(id, "gateway-123");
        InvoiceProjection same = new InvoiceProjection(id, "gateway-123");
        InvoiceProjection other = new InvoiceProjection(id, "gateway-456");

        assertThat(projection)
                .isEqualTo(same)
                .hasSameHashCodeAs(same)
                .isNotEqualTo(other)
                .isNotEqualTo(new Object())
                .isNotEqualTo(null);
        assertThat(projection.toString())
                .contains("gateway-123")
                .contains(id.toString());
    }
}
