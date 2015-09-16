/**
 * Copyright 2015 Groupon.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.arpnetworking.metrics.codahale;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.function.Consumer;

/**
 * A class to ease the acquisition and use of an atomic ref, protected by a ReadWriteLock.
 * The readLocked and writeLocked methods will perform the passed action after obtaining
 * a read lock and write lock, respectively.
 *
 * @param <T> The type of object protected by the locks.
 * @author Brandon Arp (barp at groupon dot com)
 */
public class SafeRefLock<T> {
    /**
     * Public constructor.
     *
     * @param reference the reference to get
     * @param lock the lock protecting the reference
     */
    public SafeRefLock(final AtomicReference<T> reference, final ReadWriteLock lock) {
        _reference = reference;
        _lock = lock;
    }

    /**
     * Perform an action while the read lock is acquired.
     *
     * @param method The action to perform
     */
    public void readLocked(final Consumer<T> method) {
        locking(method, _lock.readLock());
    }

    /**
     * Perform an action while the write lock is acquired.
     *
     * @param method The action to perform
     */
    public void writeLocked(final Consumer<T> method) {
        locking(method, _lock.writeLock());
    }

    private void locking(final Consumer<T> method, final Lock lock) {
        try {
            lock.lock();
            final T resolved = _reference.get();
            method.accept(resolved);
        } finally {
            lock.unlock();
        }
    }

    private final AtomicReference<T> _reference;
    private final ReadWriteLock _lock;
}
