package com.veltro.inventory.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RequestContextHolderTest {

    @AfterEach
    void tearDown() {
        RequestContextHolder.clear();
    }

    @Test
    @DisplayName("set y get — devuelve el contexto establecido")
    void set_and_get_returnsContext() {
        RequestContextHolder.RequestContext ctx = new RequestContextHolder.RequestContext("192.168.1.1");
        RequestContextHolder.set(ctx);

        RequestContextHolder.RequestContext result = RequestContextHolder.get();
        assertThat(result).isNotNull();
        assertThat(result.clientIp()).isEqualTo("192.168.1.1");
    }

    @Test
    @DisplayName("get — antes de set devuelve null")
    void get_beforeSet_returnsNull() {
        assertThat(RequestContextHolder.get()).isNull();
    }

    @Test
    @DisplayName("clear — elimina el contexto")
    void clear_removesContext() {
        RequestContextHolder.set(new RequestContextHolder.RequestContext("10.0.0.1"));
        RequestContextHolder.clear();

        assertThat(RequestContextHolder.get()).isNull();
    }

    @Test
    @DisplayName("getClientIp — devuelve IP del contexto")
    void getClientIp_returnsIp() {
        RequestContextHolder.set(new RequestContextHolder.RequestContext("172.16.0.1"));
        assertThat(RequestContextHolder.getClientIp()).isEqualTo("172.16.0.1");
    }

    @Test
    @DisplayName("getClientIp — sin contexto devuelve null")
    void getClientIp_noContext_returnsNull() {
        assertThat(RequestContextHolder.getClientIp()).isNull();
    }

    @Test
    @DisplayName("set — sobrescribe contexto anterior")
    void set_overwritesPrevious() {
        RequestContextHolder.set(new RequestContextHolder.RequestContext("1.1.1.1"));
        RequestContextHolder.set(new RequestContextHolder.RequestContext("2.2.2.2"));

        assertThat(RequestContextHolder.getClientIp()).isEqualTo("2.2.2.2");
    }

    @Test
    @DisplayName("RequestContext record — igualdad por valor")
    void requestContext_recordEquality() {
        var ctx1 = new RequestContextHolder.RequestContext("1.2.3.4");
        var ctx2 = new RequestContextHolder.RequestContext("1.2.3.4");

        assertThat(ctx1).isEqualTo(ctx2);
    }
}
