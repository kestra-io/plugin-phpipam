package io.kestra.plugin.phpipam.ipam.address;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class NewAddressTriggerTest {

    /**
     * When 2 new addresses exist in a poll and the first is emitted, only its id must be added
     * to the seen-set. The second must remain absent so it fires on the next poll.
     */
    @Test
    void nextSeenIds_adds_only_emitted_id() {
        var previouslySeen = Set.of("10", "11");

        var result = NewAddressTrigger.nextSeenIds(previouslySeen, "42");

        assertThat(result, hasItems("10", "11", "42"));
        assertThat(result, not(hasItem("99"))); // second new address stays unseen
        assertThat(result, hasSize(3));
    }

    @Test
    void nextSeenIds_from_empty_produces_singleton() {
        var result = NewAddressTrigger.nextSeenIds(Set.of(), "7");

        assertThat(result, contains("7"));
    }

    @Test
    void nextSeenIds_is_idempotent_when_id_already_seen() {
        var previouslySeen = Set.of("5");

        var result = NewAddressTrigger.nextSeenIds(previouslySeen, "5");

        assertThat(result, hasSize(1));
        assertThat(result, contains("5"));
    }
}
