/*******************************************************************************
 * Copyright (c) 2019 1C-Soft LLC.
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Vladimir Piskarev (1C) - initial API and implementation
 *******************************************************************************/
package org.lxtk.util;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * TODO JavaDoc
 */
public interface Policy
{
    /**
     * TODO JavaDoc
     *
     * @return <code>true</code> if this policy is satisfied,
     *  and <code>false</code> otherwise
     */
    boolean check();

    /**
     * TODO JavaDoc
     *
     * @return a policy that is always satisfied
     */
    static Policy always()
    {
        return () -> true;
    }

    /**
     * TODO JavaDoc
     *
     * @return a policy that is never satisfied
     */
    static Policy never()
    {
        return () -> false;
    }

    /**
     * TODO JavaDoc
     *
     * @param maxCount a positive integer
     * @return a policy that is satisfied up to but not including
     *  the given number of checks
     */
    static UpTo upTo(int maxCount)
    {
        if (maxCount < 1)
            throw new IllegalArgumentException();

        return new UpTo(maxCount);
    }

    /**
     * TODO JavaDoc
     */
    public static class UpTo
        implements Policy
    {
        private final int maxCount;
        private int count;

        private UpTo(int maxCount)
        {
            this.maxCount = maxCount;
        }

        /**
         * TODO JavaDoc
         *
         * @param duration a positive duration (not <code>null</code>)
         * @return a policy that is satisfied up to but not including
         *  the number of checks specified for this policy or when
         *  the last sequence of the specified number of checks is performed
         *  in the amount of time that is greater than the given duration
         */
        public UpToIn in(Duration duration)
        {
            if (duration.isNegative() || duration.isZero())
                throw new IllegalArgumentException();

            return new UpToIn(maxCount, duration.toMillis());
        }

        /**
         * TODO JavaDoc
         *
         * @return a policy that is satisfied up to but not including
         *  the number of checks specified for this policy and is
         *  automatically {@link #reset() reset} once the specified number
         *  of checks is reached
         */
        public Policy thenReset()
        {
            return new UpTo(maxCount)
            {
                @Override
                public boolean check()
                {
                    boolean result = super.check();
                    if (!result)
                        reset();
                    return result;
                }
            };
        }

        @Override
        public boolean check()
        {
            if (count == maxCount - 1)
                return false;

            ++count;
            return true;
        }

        /**
         * TODO JavaDoc
         *
         */
        public void reset()
        {
            count = 0;
        }
    }

    /**
     * TODO JavaDoc
     */
    public static class UpToIn
        implements Policy
    {
        private final int maxCount;
        private final long duration;
        private final List<Long> log;

        private UpToIn(int maxCount, long duration)
        {
            this.maxCount = maxCount;
            this.duration = duration;
            this.log = new ArrayList<>(maxCount);
        }

        /**
         * TODO JavaDoc
         *
         * @return a policy that is satisfied up to but not including
         *  the number of checks specified for this policy or when
         *  the last sequence of the specified number of checks is performed
         *  in the amount of time that is greater than the duration
         *  specified for this policy and is automatically {@link #reset()
         *  reset} once the specified number of checks is reached in the
         *  specified duration
         */
        public Policy thenReset()
        {
            return new UpToIn(maxCount, duration)
            {
                @Override
                public boolean check()
                {
                    boolean result = super.check();
                    if (!result)
                        reset();
                    return result;
                }
            };
        }

        @Override
        public boolean check()
        {
            long currentTime = System.currentTimeMillis();

            if (log.size() == maxCount)
                log.remove(0);

            log.add(currentTime);

            return log.size() < maxCount || currentTime - log.get(0) > duration;
        }

        /**
         * TODO JavaDoc
         *
         */
        public void reset()
        {
            log.clear();
        }
    }
}
