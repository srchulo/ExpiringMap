package com.srchulo.expiringmap;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import java.time.Clock;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by srchulo on 7/11/2017.
 */
public final class ExpiringMap<K, V> implements ConcurrentMap<K, V> {
    private final Map<K, ExpiringEntry<V>> map;
    private final long defaultTimeToLiveMillis;
    private final Clock clock;
    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final Lock readLock = readWriteLock.readLock();
    private final Lock writeLock = readWriteLock.writeLock();

    public ExpiringMap(long timeToLiveMillis) {
        this(timeToLiveMillis, TimeUnit.MILLISECONDS);
    }

    public ExpiringMap(long defaultTimeToLive, TimeUnit timeUnit) {
        this(defaultTimeToLive, timeUnit, Clock.systemDefaultZone());
    }

    @VisibleForTesting
    public ExpiringMap(long defaultTimeToLive, TimeUnit timeUnit, Clock clock) {
        Preconditions.checkArgument(defaultTimeToLive > 0, "defaultTimeToLive must be positive");
        Preconditions.checkNotNull(timeUnit, "timeUnit cannot be null");

        map = new HashMap<>();
        this.defaultTimeToLiveMillis = timeUnit.toMillis(defaultTimeToLive);
        this.clock = Preconditions.checkNotNull(clock);
    }

    // locked before and after
    private void removeExpiredEntries() {
        Iterator<Entry<K, ExpiringEntry<V>>> iterator = map.entrySet().iterator();
        while (iterator.hasNext()) {
            Entry<K, ExpiringEntry<V>> entry = iterator.next();

            if (entry.getValue().isExpired()) {
                iterator.remove();
            }
        }
    }

    @Override
    public int size() {
        writeLock.lock();
        removeExpiredEntries();
        int size = map.size();
        writeLock.unlock();
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public boolean containsKey(Object key) {
        readLock.lock();
        ExpiringEntry<V> expiringEntry = map.get(key);
        readLock.unlock();

        if (expiringEntry == null) {
            return false;
        }
        if (!expiringEntry.isExpired()) {
            return true;
        }

        remove(key);

        return false;
    }

    @Override
    public boolean containsValue(Object value) {
        readLock.lock();

        try {
            for (ExpiringEntry<V> expiringEntry : map.values()) {
                if (!expiringEntry.getValue().equals(value)) {
                    continue;
                }

                return expiringEntry.isNotExpired();
            }
        } finally {
            readLock.unlock();
        }

        return false;
    }

    @Override
    public V get(Object key) {
        readLock.lock();
        ExpiringEntry<V> expiringEntry = map.get(key);
        readLock.unlock();

        if (expiringEntry == null || !expiringEntry.isExpired()) {
            return expiringEntry.getValue();
        }

        remove(key);
        return null;
    }

    @Override
    public V put(K key, V value) {
        return put(key, value, defaultTimeToLiveMillis);
    }

    public V put(K key, V value, long timeToLiveMillis) {
        return put(key, value, timeToLiveMillis, TimeUnit.MILLISECONDS);
    }

    public V put(K key, V value, long timeToLive, TimeUnit timeUnit) {
        long expiresTime = clock.millis() + timeUnit.toMillis(timeToLive);
        ExpiringEntry<V> expiringEntry = new ExpiringEntry<>(value, expiresTime, clock);
        writeLock.lock();
        map.put(key, expiringEntry);
        writeLock.unlock();
        return value;
    }

    @Override
    public V remove(Object key) {
        writeLock.lock();
        ExpiringEntry<V> expiringEntry = map.remove(key);
        writeLock.unlock();

        return expiringEntry == null ? null : expiringEntry.getValue();
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        writeLock.lock();

    }

    @Override
    public void clear() {

    }

    @Override
    public Set<K> keySet() {
        return null;
    }

    @Override
    public Collection<V> values() {
        return null;
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return null;
    }

    @Override
    public V putIfAbsent(K key, V value) {
        return null;
    }

    @Override
    public boolean remove(Object key, Object value) {
        return false;
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        return false;
    }

    @Override
    public V replace(K key, V value) {
        return null;
    }
}
