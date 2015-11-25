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
import com.codahale.metrics.Clock;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.TimeUnit;

/**
 * Represents a Timer that is wrapped to output Arpnetworking Metrics.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
@SuppressFBWarnings("NM_SAME_SIMPLE_NAME_AS_SUPERCLASS")
public class Timer extends com.codahale.metrics.Timer {
    /**
     * Public constructor.
     *
     * @param name name of the metric
     * @param lock lock for the metrics reference
     * @param clock the clock to use for timing
     */
    public Timer(final String name, final SafeRefLock<Metrics> lock, final Clock clock) {
        _name = name;
        _lock = lock;
        _clock = clock;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void update(final long duration, final TimeUnit unit) {
        _lock.readLocked(metrics -> metrics.setTimer(_name, duration, unit));
        super.update(duration, unit);
    }

    @Override
    public Context time() {
        try {
            return CONTEXT_CONSTRUCTOR.newInstance(this, _clock);
        } catch (final InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private final String _name;
    private final SafeRefLock<Metrics> _lock;
    private final Clock _clock;

    private static final Constructor<Context> CONTEXT_CONSTRUCTOR;
    static {
        try {
            CONTEXT_CONSTRUCTOR = Context.class.getDeclaredConstructor(com.codahale.metrics.Timer.class, Clock.class);
            CONTEXT_CONSTRUCTOR.setAccessible(true);
        } catch (final NoSuchMethodException e) {
            throw new RuntimeException(e);
        }

    }
}
