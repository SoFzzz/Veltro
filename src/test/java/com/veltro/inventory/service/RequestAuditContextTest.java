package com.veltro.inventory.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RequestAuditContextTest {

    @Test
    @DisplayName("empty — devuelve contexto con IP null")
    void empty_returnsNullIp() {
        RequestAuditContext ctx = RequestAuditContext.empty();
        assertThat(ctx.ipAddress()).isNull();
    }

    @Test
    @DisplayName("withIp — devuelve contexto con IP correcta")
    void withIp_returnsCorrectIp() {
        RequestAuditContext ctx = RequestAuditContext.withIp("192.168.1.1");
        assertThat(ctx.ipAddress()).isEqualTo("192.168.1.1");
    }

    @Test
    @DisplayName("igualdad de records — misma IP son iguales")
    void recordEquality() {
        RequestAuditContext ctx1 = RequestAuditContext.withIp("10.0.0.1");
        RequestAuditContext ctx2 = RequestAuditContext.withIp("10.0.0.1");

        assertThat(ctx1).isEqualTo(ctx2);
        assertThat(ctx1.hashCode()).isEqualTo(ctx2.hashCode());
    }

    @Test
    @DisplayName("igualdad de records — diferente IP no son iguales")
    void recordInequality() {
        RequestAuditContext ctx1 = RequestAuditContext.withIp("10.0.0.1");
        RequestAuditContext ctx2 = RequestAuditContext.withIp("10.0.0.2");

        assertThat(ctx1).isNotEqualTo(ctx2);
    }

    @Test
    @DisplayName("constructor directo — funciona igual que factory method")
    void directConstructor() {
        RequestAuditContext ctx = new RequestAuditContext("127.0.0.1");
        assertThat(ctx.ipAddress()).isEqualTo("127.0.0.1");
    }

    @Test
    @DisplayName("empty — dos empty son iguales")
    void twoEmpty_areEqual() {
        assertThat(RequestAuditContext.empty()).isEqualTo(RequestAuditContext.empty());
    }
}
