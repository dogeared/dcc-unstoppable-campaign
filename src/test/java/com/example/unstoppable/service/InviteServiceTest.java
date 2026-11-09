package com.example.unstoppable.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InviteServiceTest {

    @Test
    void hashIsStableAndDoesNotRevealTheToken() {
        String hash = InviteService.hash("a-very-long-invite-token");

        assertThat(hash).hasSize(64);
        assertThat(hash).isEqualTo(InviteService.hash("a-very-long-invite-token"));
        assertThat(hash).doesNotContain("invite");
    }

    @Test
    void differentTokensHashDifferently() {
        assertThat(InviteService.hash("one"))
                .isNotEqualTo(InviteService.hash("two"));
    }
}
