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
import com.codahale.metrics.ContextInterceptor;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

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
        return new Context(this, _clock);
    }

    private final String _name;
    private final SafeRefLock<Metrics> _lock;
    private final Clock _clock;

    /**
     * Context class to stop and record a timer sample.
     */
    public static class Context extends ContextInterceptor {
        Context(final com.codahale.metrics.Timer timer, final Clock clock) {
            super(timer, clock);
        }
    }
}
