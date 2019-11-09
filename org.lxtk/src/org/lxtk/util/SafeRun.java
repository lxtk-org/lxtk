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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * TODO JavaDoc
 */
public class SafeRun
{
    private SafeRun()
    {
    }

    /**
     * TODO JavaDoc
     *
     * @param task not <code>null</code>
     */
    public static void run(Consumer<Rollback> task)
    {
        runWithResult(rollback ->
        {
            task.accept(rollback);
            return null;
        });
    }

    /**
     * TODO JavaDoc
     *
     * @param <T> result type
     * @param task not <code>null</code>
     * @return the result of the task
     */
    public static <T> T runWithResult(Function<Rollback, T> task)
    {
        Rollback rollback = new Rollback();
        try
        {
            return task.apply(rollback);
        }
        catch (Throwable t)
        {
            if (rollback.getLogger() == null)
                rollback.setLogger(e -> t.addSuppressed(e));

            rollback.run();
            throw t;
        }
    }

    /**
     * TODO JavaDoc
     */
    public final static class Rollback
        implements Runnable
    {
        private List<Runnable> tasks = new ArrayList<>();
        private Consumer<Throwable> logger;

        /**
         * TODO JavaDoc
         *
         * @param logger may be <code>null</code>
         */
        public void setLogger(Consumer<Throwable> logger)
        {
            this.logger = logger;
        }

        /**
         * TODO JavaDoc
         *
         * @return the rollback logger (may be <code>null</code>)
         */
        public Consumer<Throwable> getLogger()
        {
            return logger;
        }

        /**
         * TODO JavaDoc
         *
         * @param task a rollback task (not <code>null</code>)
         */
        public void add(Runnable task)
        {
            tasks.add(Objects.requireNonNull(task));
        }

        /**
         * TODO JavaDoc
         *
         */
        public void reset()
        {
            tasks.clear();
        }

        @Override
        public void run()
        {
            for (int i = tasks.size() - 1; i >= 0; i--)
            {
                try
                {
                    tasks.get(i).run();
                }
                catch (Throwable t)
                {
                    if (logger != null)
                        logger.accept(t);
                }
            }
            reset();
        };
    }
}
