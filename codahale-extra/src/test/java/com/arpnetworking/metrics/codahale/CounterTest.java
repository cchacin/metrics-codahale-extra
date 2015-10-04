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

import com.arpnetworking.metrics.Metrics;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.function.Consumer;

/**
 * Tests for the Counter class.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class CounterTest {
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void increment() {
        final Counter counter = new Counter("foo", _lock);
        counter.inc();
        Mockito.verifyZeroInteractions(_metrics);
        Mockito.verify(_lock).readLocked(_delegateCaptor.capture());
        _delegateCaptor.getValue().accept(_metrics);
        Mockito.verify(_metrics).incrementCounter("foo", 1);
    }

    @Test
    public void incrementByN() {
        final int n = 16;
        final Counter counter = new Counter("foo", _lock);
        counter.inc(n);
        Mockito.verifyZeroInteractions(_metrics);
        Mockito.verify(_lock).readLocked(_delegateCaptor.capture());
        _delegateCaptor.getValue().accept(_metrics);
        Mockito.verify(_metrics).incrementCounter("foo", n);
    }

    @Test
    public void decrement() {
        final Counter counter = new Counter("foo", _lock);
        counter.dec();
        Mockito.verifyZeroInteractions(_metrics);
        Mockito.verify(_lock).readLocked(_delegateCaptor.capture());
        _delegateCaptor.getValue().accept(_metrics);
        Mockito.verify(_metrics).decrementCounter("foo", 1);
    }

    @Test
    public void decrementByN() {
        final int n = 16;
        final Counter counter = new Counter("foo", _lock);
        counter.dec(n);
        Mockito.verifyZeroInteractions(_metrics);
        Mockito.verify(_lock).readLocked(_delegateCaptor.capture());
        _delegateCaptor.getValue().accept(_metrics);
        Mockito.verify(_metrics).decrementCounter("foo", n);
    }

    @Mock
    private SafeRefLock<Metrics> _lock;
    @Captor
    private ArgumentCaptor<Consumer<Metrics>> _delegateCaptor;
    @Mock
    private Metrics _metrics;
}
