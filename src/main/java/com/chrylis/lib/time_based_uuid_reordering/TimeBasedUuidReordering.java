package com.chrylis.lib.time_based_uuid_reordering;

import java.util.UUID;

/**
 * Reorders time-based (version 1) UUIDs with the most significant time information first
 * so that they will sort lexographically.
 *
 * The RFC 4122 definition of the version 1 UUID specifies the following structure:
 *
 * <pre>
 * 0                   1                   2                   3
 *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                          time_low                             |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |       time_mid                |         time_hi_and_version   |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |clk_seq_hi_res |  clk_seq_low  |         node (0-1)            |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                         node (2-5)                            |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * </pre>
 *
 * Unfortunately, this structure uses a middle-endian representation of the time that results in
 * random sequencing lexographically, making it unsuitable for such tasks as database indexing.
 *
 * The methods in this class reorder the {@code time_*} bits to pack them in big-endian order. Note
 * that to maintain compatibility (and thus reduce the risk of collision), the {@code version}
 * field is left in its correct place. The sequence fields are not reordered, as they are not
 * formally monotonic.
 *
 * <pre>
 * 0                   1                   2                   3
 *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |        time_hi        |           time_mid            |tl(0:3)|
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |        time_low(4:19)         |version|    time_low(20:31)    |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |clk_seq_hi_res |  clk_seq_low  |         node (0-1)            |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                         node (2-5)                            |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * </pre>
 *
 * Applications that reorder <em>all</em> version 1 UUIDs according to this algorithm have exactly
 * the same collision risk as applications that do not reorder at all. Applications that reorder
 * <em>only some</em> version 1 UUIDs are unlikely to collide (especially if only one generator
 * application is running with a particular node ID), but theoretically carry a 2^60 chance of
 * collision. No collision is possible with any other UUID version.
 *
 * <strong>N.B.: Hibernate's {@code CustomVersionOneStrategy} already uses a non-standard ordering
 * that places the machine identifiers in most-significant bytes.</strong> This means that records
 * originating from the same machine are grouped before sorting by time. That generator also uses
 * a timestamp based on milliseconds since the epoch; since that is not bitwise compatible with
 * the RFC's 100-nanosecond steps, this class does not provide a converter for it.
 *
 * @author Christopher Smith
 *
 */
public final class TimeBasedUuidReordering {

    private TimeBasedUuidReordering() {
    }

    private static final long VERSION_ONE = 0x0000_0000_0000_1000L;

    private static void checkVersion(UUID input) {
        if (!(input.version() == 1)) {
            throw new IllegalArgumentException("input UUID was version " + input.version());
        }
    }

    /**
     * Reorder an RFC 4122 version 1 UUID into a big-endian timestamp.
     *
     * @param rfc
     *            a UUID with timestamp bits in RFC order
     * @return a version 1 UUID with timestamp bits in big-endian order
     */
    public static UUID rfcToBigEndian(UUID rfc) {
        checkVersion(rfc);

        long low = rfc.getLeastSignificantBits(); // not modified; contains variant field
        long oldHi = rfc.getMostSignificantBits();

        // @formatter:off
        long time_hi =         oldHi & 0x0000_0000_0000_0fffL;
        long time_mid =       (oldHi & 0x0000_0000_ffff_0000L) >>> 16;
        long time_low_0_19 =  (oldHi & 0xffff_f000_0000_0000L) >>> 44;
        long time_low_20_31 = (oldHi & 0x0000_0fff_0000_0000L) >>> 32;
        // @formatter:on

        long newHi = (VERSION_ONE)
                | time_hi << 52
                | time_mid << 36
                | time_low_0_19 << 16
                | time_low_20_31;

        return new UUID(newHi, low);
    }

    /**
     * Reorder a version 1 UUID with a big-endian timestamp to RFC 4122 order.
     *
     * @param bigEndian
     *            a version 1 UUID with timestamp bits in big-endian order
     * @return a UUID with timestamp bits in RFC order
     */
    public static UUID bigEndianToRfc(UUID bigEndian) {
        checkVersion(bigEndian);

        long low = bigEndian.getLeastSignificantBits(); // not modified
        long oldHi = bigEndian.getMostSignificantBits();

        // @formatter:off
        long time_hi        = (oldHi & 0xfff0_0000_0000_0000L) >>> 52;
        long time_mid       = (oldHi & 0x000f_fff0_0000_0000L) >>> 36;
        long time_low_0_19  = (oldHi & 0x0000_000f_ffff_0000L) >>> 16;
        long time_low_20_31 =  oldHi & 0x0000_0000_0000_0fffL;
        // @formatter:on

        long newHi = (VERSION_ONE)
                | time_low_0_19 << 44
                | time_low_20_31 << 32
                | time_mid << 16
                | time_hi;

        return new UUID(newHi, low);
    }

    /*
     * Documentation for Hibernate's {@code CustomVersionOneStrategy}. Since the timestamp provided
     * there is not bitwise compatible with the RFC timestamp, no converter method is implemented.
     *
     * long oldLow = hibernate.getLeastSignificantBits();
     * long oldHi = hibernate.getMostSignificantBits();
     *
     * long address = (oldHi & 0xffff_ffff_0000_0000L) >>> 32; // 32 bits
     * long jvmIdentifier = (oldHi & 0x0000_0000_ffff_0fffL); // 32 bits minus 4-bit hole
     *
     * long hiTime = (oldLow & 0x3fff_0000_0000_0000L) >>> 48; // 14 bits
     * long loTime = (oldLow & 0x0000_ffff_ffff_0000L) >>> 16; // 32 bits
     * long counter = (oldLow & 0x0000_0000_0000_ffffL); // 16 bits
     */
}
