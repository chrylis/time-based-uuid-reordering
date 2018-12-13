package com.chrylis.lib.time_based_uuid_reordering;

import static com.chrylis.lib.time_based_uuid_reordering.TimeBasedUuidReordering.UUID_TIMESTAMP_ROLLOVER;
import static com.chrylis.lib.time_based_uuid_reordering.TimeBasedUuidReordering.bigEndianToInstant;
import static com.chrylis.lib.time_based_uuid_reordering.TimeBasedUuidReordering.bigEndianToRfc;
import static com.chrylis.lib.time_based_uuid_reordering.TimeBasedUuidReordering.lowestBound;
import static com.chrylis.lib.time_based_uuid_reordering.TimeBasedUuidReordering.rfcToBigEndian;
import static com.chrylis.lib.time_based_uuid_reordering.TimeBasedUuidReordering.rfcUuidToInstant;
import static java.time.temporal.ChronoUnit.NANOS;
import static java.util.UUID.fromString;
import static org.junit.Assert.assertEquals;

import java.time.Instant;
import java.util.UUID;

import org.junit.Test;

public class TimeBasedUuidReorderingTest {

    @Test
    public void testRoundTrip() {
        for (String[] pair : TEST_PAIRS) {
            UUID rfc = fromString(pair[0]);
            UUID expected = fromString(pair[1]);
            UUID reordered = rfcToBigEndian(rfc);
            assertEquals(expected, reordered);
            assertEquals(rfc, bigEndianToRfc(reordered));
        }
    }

    // formatted in {RFC, big-endian}
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

    @Test
    public void lowestBoundAcceptsHighestValue() {
        UUID bigTimestamp = UUID.fromString("ffffffff-ffff-1fff-8000-000000000000");
        assertEquals(bigTimestamp, lowestBound(UUID_TIMESTAMP_ROLLOVER));
    }

    @Test(expected = IllegalArgumentException.class)
    public void lowestBoundRejectsBigInstants() {
        Instant overflow = UUID_TIMESTAMP_ROLLOVER.plus(100, NANOS);
        lowestBound(overflow);
    }

    @Test
    public void lowestBoundCalculatesCorrectly() {
        // timestamp taken at development time, correct answer worked out
        Instant timestamp = Instant.ofEpochSecond(1543470108);
        UUID lb = lowestBound(timestamp);
        assertEquals(UUID.fromString("1e8f3997-697b-1600-8000-000000000000"), lb);
    }

    @Test
    public void rfcUuidToInstantCalculatesCorrectly() {
        // timestamp taken at development time, correct answer worked out
        UUID rfc = fromString("c00a8592-fe7f-11e8-8eb2-f2801f1b9fd1");
        Instant timestamp = Instant.ofEpochSecond(1544668527, 1016850_00);
        assertEquals(timestamp, rfcUuidToInstant(rfc));
    }

    @Test
    public void bigEndianToInstantCalculatesCorrectly() {
        // timestamp taken at development time, correct answer worked out
        UUID rfc = fromString("c00a8592-fe7f-11e8-8eb2-f2801f1b9fd1");
        UUID bigEndian = rfcToBigEndian(rfc);
        Instant timestamp = Instant.ofEpochSecond(1544668527, 1016850_00);
        assertEquals(timestamp, bigEndianToInstant(bigEndian));
    }

}
