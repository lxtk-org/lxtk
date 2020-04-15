/*******************************************************************************
 * Copyright (c) 2019, 2020 1C-Soft LLC.
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
 * Represents a policy that can be checked.
 */
public interface Policy
{
    /**
     * Checks whether this policy is satisfied.
     *
     * @return <code>true</code> if this policy is satisfied,
     *  and <code>false</code> otherwise
     */
    boolean check();

    /**
     * Returns a policy that is always satisfied.
     *
     * @return a policy that is always satisfied
     */
    static Policy always()
    {
        return () -> true;
    }

    /**
     * Returns a policy that is never satisfied.
     *
     * @return a policy that is never satisfied
     */
    static Policy never()
    {
        return () -> false;
    }

    /**
     * Returns a policy that is satisfied up to but not including
     * the given number of checks.
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
     * A policy that is satisfied up to but not including
     * a given number of checks.
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
         * Returns a policy that is satisfied up to but not including
         * the number of checks specified for this policy or when
         * the last sequence of the specified number of checks is performed
         * in the amount of time that is greater than the given duration.
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
         * Returns a policy that is satisfied up to but not including
         * the number of checks specified for this policy and is
         * automatically {@link #reset() reset} once the specified number
         * of checks is reached.
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
         * Resets this policy object.
         */
        public void reset()
        {
            count = 0;
        }
    }

    /**
     * A policy that is satisfied up to but not including a given number of checks
     * or when the last sequence of the given number of checks is performed
     * in the amount of time that is greater than a given duration.
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
         * Returns a policy that is satisfied up to but not including
         * the number of checks specified for this policy or when
         * the last sequence of the specified number of checks is performed
         * in the amount of time that is greater than the duration
         * specified for this policy and is automatically {@link #reset()
         * reset} once the specified number of checks is reached in the
         * specified duration.
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
         * Resets this policy object.
         */
        public void reset()
        {
            log.clear();
        }
    }
}
