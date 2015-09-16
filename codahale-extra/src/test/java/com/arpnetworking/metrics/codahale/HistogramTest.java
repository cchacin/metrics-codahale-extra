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
import com.codahale.metrics.ExponentiallyDecayingReservoir;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.function.Consumer;

/**
 * Tests for the Histogram class.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class HistogramTest {
    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void updateInt() {
        final Histogram histogram = new Histogram("foo", _lock, new ExponentiallyDecayingReservoir());
        final int n = 55;
        histogram.update(n);
        Mockito.verifyZeroInteractions(_metrics);
        Mockito.verify(_lock).readLocked(_delegateCaptor.capture());
        _delegateCaptor.getValue().accept(_metrics);
        Mockito.verify(_metrics).incrementCounter("foo", n);
    }

    @Test
    public void updateLong() {
        final Histogram histogram = new Histogram("foo", _lock, new ExponentiallyDecayingReservoir());
        final long n = 58195;
        histogram.update(n);
        Mockito.verifyZeroInteractions(_metrics);
        Mockito.verify(_lock).readLocked(_delegateCaptor.capture());
        _delegateCaptor.getValue().accept(_metrics);
        Mockito.verify(_metrics).incrementCounter("foo", n);
    }

    @Mock
    private SafeRefLock<Metrics> _lock;
    @Captor
    private ArgumentCaptor<Consumer<Metrics>> _delegateCaptor;
    @Mock
    private Metrics _metrics;
}
