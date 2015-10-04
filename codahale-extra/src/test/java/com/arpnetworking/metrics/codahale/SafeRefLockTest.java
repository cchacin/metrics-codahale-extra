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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * Tests for the SafeRefLock class.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class SafeRefLockTest {
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void readLock() {
        final Lock readLock = Mockito.mock(Lock.class);
        Mockito.when(_lock.readLock()).thenReturn(readLock);
        final SafeRefLock<Object> safeLock = new SafeRefLock<>(_reference, _lock);
        safeLock.readLocked(
                obj -> {
                    Mockito.verify(readLock).lock();
                    Assert.assertSame(_object, obj);
                    final Lock readLockCall = Mockito.verify(_lock).readLock();
                    Mockito.verifyNoMoreInteractions(_lock);
                });

        Mockito.verify(readLock).unlock();
        Mockito.verifyNoMoreInteractions(readLock);
    }

    @Test
    public void writeLock() {
        final Lock writeLock = Mockito.mock(Lock.class);
        Mockito.when(_lock.writeLock()).thenReturn(writeLock);
        final SafeRefLock<Object> safeLock = new SafeRefLock<>(_reference, _lock);
        safeLock.writeLocked(
                obj -> {
                    Mockito.verify(writeLock).lock();
                    Assert.assertSame(_object, obj);
                    final Lock writeLockCall = Mockito.verify(_lock).writeLock();
                    Mockito.verifyNoMoreInteractions(_lock);
                });

        Mockito.verify(writeLock).unlock();
        Mockito.verifyNoMoreInteractions(writeLock);
    }

    @Test(expected = RuntimeException.class)
    public void handlesException() throws InterruptedException {
        final Lock readLock = Mockito.mock(Lock.class);
        Mockito.when(_lock.readLock()).thenReturn(readLock);
        final SafeRefLock<Object> safeLock = new SafeRefLock<>(_reference, _lock);
        safeLock.readLocked(
                obj -> {
                    throw new RuntimeException("test exception");
                });

        Mockito.verify(readLock).unlock();
        Mockito.verifyNoMoreInteractions(readLock);
    }

    @Mock
    private ReadWriteLock _lock;
    private final Object _object = new Object();
    private final AtomicReference<Object> _reference = new AtomicReference<>(_object);
}
