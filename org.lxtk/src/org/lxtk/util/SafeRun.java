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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Provides static methods for running a task with support for rollback.
 * <p>
 * While the task is running, it can add <i>rollback tasks</i> to its {@link
 * Rollback} object. Then, if an exception is thrown by the running task, the
 * rollback tasks added so far are executed in reverse order. Unless a rollback
 * {@link Rollback#setLogger(Consumer) logger} has been set by the failed task,
 * a default logger is set, which adds any exception thrown by a rollback task
 * to the list of suppressed exceptions of the original exception.
 * </p>
 */
public class SafeRun
{
    private SafeRun()
    {
    }

    /**
     * Runs a task with support for rollback.
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
     * Runs a computation task with support for rollback.
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
     * Contains a list of rollback tasks that are executed in reverse order
     * when the rollback is run. After a rollback runs, it {@link #reset()
     * resets} itself. If an exception is thrown while executing a rollback task,
     * the exception is passed to the rollback {@link #getLogger() logger}.
     * If no logger is set, the exception is suppressed.
     */
    public final static class Rollback
        implements Runnable
    {
        private List<Runnable> tasks = new ArrayList<>();
        private Consumer<Throwable> logger;

        /**
         * Sets the rollback logger.
         *
         * @param logger may be <code>null</code>
         */
        public void setLogger(Consumer<Throwable> logger)
        {
            this.logger = logger;
        }

        /**
         * Returns the rollback logger.
         *
         * @return the rollback logger (may be <code>null</code>)
         * @see #setLogger(Consumer)
         */
        public Consumer<Throwable> getLogger()
        {
            return logger;
        }

        /**
         * Adds a rollback task.
         *
         * @param task a rollback task (not <code>null</code>)
         */
        public void add(Runnable task)
        {
            tasks.add(Objects.requireNonNull(task));
        }

        /**
         * Resets this instance, clearing the list of rollback tasks.
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
