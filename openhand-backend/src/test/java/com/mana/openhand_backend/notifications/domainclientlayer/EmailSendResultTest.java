package com.mana.openhand_backend.notifications.domainclientlayer;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EmailSendResultTest {

    @Test
    void okFactoryBuildsSuccess() {
        EmailSendResult result = EmailSendResult.ok();
        assertThat(result.success()).isTrue();
        assertThat(result.errorMessage()).isNull();
    }

    @Test
    void failureFactoryBuildsFailure() {
        EmailSendResult result = EmailSendResult.failure("oops");
        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).isEqualTo("oops");
    }
}
