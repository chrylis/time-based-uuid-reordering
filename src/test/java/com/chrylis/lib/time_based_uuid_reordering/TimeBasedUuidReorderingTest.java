package com.chrylis.lib.time_based_uuid_reordering;

import static java.util.UUID.fromString;
import static org.junit.Assert.assertEquals;

import java.util.UUID;

import org.junit.Test;

public class TimeBasedUuidReorderingTest {

    @Test
    public void testRoundTrip() {
        for (String[] pair : TEST_PAIRS) {
            UUID rfc = fromString(pair[0]);
            UUID expected = fromString(pair[1]);
            UUID reordered = TimeBasedUuidReordering.rfcToBigEndian(rfc);
            assertEquals(expected, reordered);
            assertEquals(rfc, TimeBasedUuidReordering.bigEndianToRfc(reordered));
        }
    }

    private static final String TEST_PAIRS[][] = {
        // @formatter:off
        { "0d0b40f8-e965-11e8-88b6-ac7ba1c62b04",
             "1e8"    // time_hi
          + "e965"    // time_mid
          + "0-d0b4-" // time_low(0:3-4:19)
          + "1"       // version
          + "0f8"     // time_low(20:31)
          + "-88b6-ac7ba1c62b04" },

        { "b1fbc6e2-e966-11e8-9f32-f2801f1b9fd1",
             "1e8"    // time_hi
          + "e966"    // time_mid
          + "b-1fbc-" // time_low(0:3-4:19)
          + "1"       // version
          + "6e2"     // time_low(20:31)
          + "-9f32-f2801f1b9fd1" },
        // @formatter:on
    };
}
