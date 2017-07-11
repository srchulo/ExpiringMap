package com.srchulo.expiringmap;

import com.google.common.base.Preconditions;
import java.time.Clock;

/**
 * Created by srchulo on 7/11/2017.
 */
final class Entry<V> {
    private final V value;
    private final long expiresTime;
    private final Clock clock;

    Entry(V value, long expiresTime, Clock clock) {
        this.value = Preconditions.checkNotNull(value);

        // allow per entry expiration?
        Preconditions.checkArgument(expiresTime > 0); // what is right check?
        this.expiresTime = expiresTime;
        this.clock = Preconditions.checkNotNull(clock);
    }

    V getValue() {
        return value;
    }

    boolean isExpired() {
        return clock.millis() > expiresTime;
    }
}
