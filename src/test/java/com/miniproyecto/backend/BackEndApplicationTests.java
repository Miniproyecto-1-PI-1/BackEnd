package com.miniproyecto.backend;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BackEndApplicationTests {

    @Test
    void applicationClassIsPresent() {
        assertThat(BackEndApplication.class).isNotNull();
    }
}
