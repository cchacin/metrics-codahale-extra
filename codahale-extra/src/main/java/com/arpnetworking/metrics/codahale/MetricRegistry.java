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
import com.arpnetworking.metrics.MetricsFactory;
import com.arpnetworking.metrics.impl.TsdLogSink;
import com.arpnetworking.metrics.impl.TsdMetricsFactory;
import com.codahale.metrics.Clock;
import com.codahale.metrics.ExponentiallyDecayingReservoir;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;

/**
 * The replacement class for the MetricRegistry.  In the case of using the shaded library
 * which replaces your Codahale dependency this class will serve as the provided MetricRegistry.
 * In the case of using the supplementary library, you should use this class instead of the
 * Codahale-provided MetricRegistry in your code.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
@SuppressFBWarnings("NM_SAME_SIMPLE_NAME_AS_SUPERCLASS")
public class MetricRegistry extends com.codahale.metrics.MetricRegistry {

    /**
     * Public constructor.
     */
    public MetricRegistry() {
        // TODO(barp): Read the settings from a config file [#2]
        this(
                new TsdMetricsFactory.Builder()
                        .setClusterName(System.getProperty("METRICS_CODAHALE_EXTRA_CLUSTER", "CodahaleCluster"))
                        .setServiceName(System.getProperty("METRICS_CODAHALE_EXTRA_SERVICE", "CodahaleService"))
                        .setSinks(Collections.singletonList(
                                new TsdLogSink.Builder()
                                        .setDirectory(
                                                new File(System.getProperty("METRICS_CODAHALE_EXTRA_DIRECTORY", "/tmp")))
                                        .build()))
                        .build());
    }

    /**
     * Public constructor.
     *
     * @param metricsFactory The metrics factory to use to create metrics.
     */
    public MetricRegistry(final MetricsFactory metricsFactory) {
        _metricsFactory = metricsFactory;
        _openMetrics.set(_metricsFactory.create());
        _closingExecutor = Executors.newSingleThreadScheduledExecutor(
                (r) -> {
                    final Thread thread = new Thread(r, "metrics-closer");
                    thread.setDaemon(true);
                    return thread;
                });
        _closingExecutor.scheduleAtFixedRate(
                new Closer(_lock, _metricsFactory, _openMetrics, this),
                CLOSER_PERIOD,
                CLOSER_PERIOD,
                TimeUnit.MILLISECONDS);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Timer timer(final String name) {
        final Timer timer = getOrCreate(name, _timerBuilder);
        return timer;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Counter counter(final String name) {
        final Counter counter = getOrCreate(name, _counterBuilder);
        return counter;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Histogram histogram(final String name) {
        final Histogram histogram = getOrCreate(name, _histogramBuilder);
        return histogram;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Meter meter(final String name) {
        final Meter meter = getOrCreate(name, _meterBuilder);
        return meter;
    }

    private <T extends Metric> T getOrCreate(final String name, final Function<String, T> builder) {
        @SuppressWarnings("unchecked")
        final T metric = (T) _metrics.computeIfAbsent(name, builder);
        return metric;
    }

    private ScheduledExecutorService _closingExecutor;

    private final ConcurrentMap<String, Metric> _metrics = new ConcurrentHashMap<>();
    private final AtomicReference<Metrics> _openMetrics = new AtomicReference<>();
    private final SafeRefLock<Metrics> _lock = new SafeRefLock<>(_openMetrics, new ReentrantReadWriteLock(false));

    // These are Functions instead of Suppliers so that we don't have to close over them in the getOrCreate function
    private final Function<String, Counter> _counterBuilder = (n) -> {
        final Counter counter = new Counter(n, _lock);
        register(n, counter);
        return counter;
    };
    private final Function<String, Timer> _timerBuilder = (n) -> {
        final Timer timer = new Timer(n, _lock, Clock.defaultClock());
        register(n, timer);
        return timer;
    };
    private final Function<String, Histogram> _histogramBuilder = (n) -> {
        final Histogram histogram = new Histogram(n, _lock, new ExponentiallyDecayingReservoir());
        register(n, histogram);
        return histogram;
    };
    private final Function<String, Meter> _meterBuilder = (n) -> {
        final Meter meter = new Meter(n, _lock);
        register(n, meter);
        return meter;
    };

    // TODO(barp): Configurable closer period [#3]
    private static final int CLOSER_PERIOD = 500;

    private final MetricsFactory _metricsFactory;

    public MetricsFactory getMetricsFactory() {
        return _metricsFactory;
    }

    /**
     * Closes a metric instance in a MetricRegistry.  Public to allow cross-package use after shading.
     *
     * @author Brandon Arp (barp at groupon dot com)
     */
    public static class Closer implements Runnable {
        /**
         * Public constructor.
         *
         * @param lock lock to acquire
         * @param factory the metrics factory
         * @param metricsRef atomic reference to the open metrics instance
         * @param registry the registry to operate on
         */
        public Closer(
                final SafeRefLock<Metrics> lock,
                final MetricsFactory factory,
                final AtomicReference<Metrics> metricsRef,
                final MetricRegistry registry) {
            _lock = lock;
            _factory = factory;
            _metricsRef = metricsRef;
            _registry = registry;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void run() {
            final Metrics metrics = _metricsRef.get();
            _lock.writeLocked(ignored -> _metricsRef.set(_factory.create()));
            try {
                @SuppressWarnings("unchecked")
                final SortedMap<String, Gauge<?>> gauges = (SortedMap<String, Gauge<?>>) (SortedMap<String, ?>) _registry.getGauges();
                for (final Map.Entry<String, Gauge<?>> entry : gauges.entrySet()) {
                    final Object value = entry.getValue().getValue();
                    if (value instanceof Number) {
                        metrics.setGauge(entry.getKey(), ((Number) value).doubleValue());
                    }
                }
                //CHECKSTYLE.OFF: IllegalCatch - we need to catch everything
            } catch (final Exception ex) {
                //CHECKSTYLE.ON: IllegalCatch
                System.err.println(ex);
                ex.printStackTrace();

            }
            metrics.close();
        }

        private final SafeRefLock<Metrics> _lock;
        private final MetricsFactory _factory;
        private final AtomicReference<Metrics> _metricsRef;
        private final MetricRegistry _registry;
    }
}
