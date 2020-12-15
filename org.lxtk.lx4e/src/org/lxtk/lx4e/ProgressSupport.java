/*******************************************************************************
 * Copyright (c) 2020 1C-Soft LLC.
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
package org.lxtk.lx4e;

import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.lxtk.WorkDoneProgressState;
import org.lxtk.lx4e.util.FutureSupport;

/**
 * Extended support for progress monitoring of an asynchronous operation.
 *
 * @param <T> type of operation result
 */
public final class ProgressSupport<T>
    extends FutureSupport<T>
{
    private static final ThreadLocal<MonitorState> MONITOR_STATE =
        ThreadLocal.withInitial(() -> new MonitorState());

    private final Supplier<WorkDoneProgressState> workDoneProgress;
    private final Supplier<Long> lastUpdated;

    /**
     * Constructor.
     *
     * @param future the operation future (not <code>null</code>)
     * @param workDoneProgress the operation progress (not <code>null</code>)
     * @param lastUpdated supplies progress 'last updated' time (not <code>null</code>)
     */
    public ProgressSupport(Future<T> future, Supplier<WorkDoneProgressState> workDoneProgress,
        Supplier<Long> lastUpdated)
    {
        super(future);
        this.workDoneProgress = Objects.requireNonNull(workDoneProgress);
        this.lastUpdated = Objects.requireNonNull(lastUpdated);
    }

    @Override
    public T join(IProgressMonitor monitor) throws InterruptedException, ExecutionException
    {
        try
        {
            return super.join(monitor);
        }
        finally
        {
            MONITOR_STATE.remove();
        }
    }

    @Override
    public T join(long timeoutMillis, IProgressMonitor monitor)
        throws InterruptedException, ExecutionException, TimeoutException
    {
        try
        {
            return super.join(timeoutMillis, monitor);
        }
        finally
        {
            MONITOR_STATE.remove();
        }
    }

    /**
     * Waits if necessary until either the operation completes or the given timeout has expired,
     * using the given monitor to report operation progress and respond to cancellation,
     * and returns the operation result, if available.
     *
     * @param timeoutMillis timeout value
     * @param strict in strict mode, the timeout expires when the given amount of time elapsed
     *  since the join start; in non-strict mode, the timeout expires when the given amount of time
     *  elapsed since the last progress state update after the join start
     * @param monitor a progress monitor, or <code>null</code>
     *  if progress reporting is not desired. The caller must not rely on
     *  {@link IProgressMonitor#done()} having been called by the receiver
     * @return the operation result
     * @throws CancellationException if the operation was cancelled via the future
     * @throws OperationCanceledException if cancellation has been requested
     *  via the progress monitor
     * @throws InterruptedException if the current thread was interrupted while waiting
     * @throws ExecutionException if the operation threw an exception
     * @throws TimeoutException if the wait timed out
     */
    public T join(long timeoutMillis, boolean strict, IProgressMonitor monitor)
        throws InterruptedException, ExecutionException, TimeoutException
    {
        try
        {
            SubMonitor subMonitor = convert(monitor);
            long startTime = System.currentTimeMillis();
            while (true)
            {
                checkCanceled(subMonitor);
                try
                {
                    return future.get(Math.min(
                        Math.max(0, startTime + timeoutMillis - System.currentTimeMillis()),
                        monitorPeriod), TimeUnit.MILLISECONDS);
                }
                catch (TimeoutException e)
                {
                    if (!strict)
                    {
                        Long lastUpdatedTime = lastUpdated.get();
                        if (lastUpdatedTime != null && lastUpdatedTime > startTime)
                            startTime = lastUpdatedTime;
                    }
                    if (System.currentTimeMillis() - startTime >= timeoutMillis)
                        throw e;

                    updateProgress(subMonitor);
                }
            }
        }
        finally
        {
            MONITOR_STATE.remove();
        }
    }

    @Override
    protected SubMonitor convert(IProgressMonitor monitor)
    {
        return SubMonitor.convert(monitor, 100);
    }

    @Override
    protected void checkCanceled(SubMonitor monitor)
    {
        WorkDoneProgressState workDoneProgressState = workDoneProgress.get();
        if (workDoneProgressState == null
            || !Boolean.FALSE.equals(workDoneProgressState.getCancellable()))
        {
            super.checkCanceled(monitor);
        }
    }

    @Override
    protected void updateProgress(SubMonitor monitor)
    {
        WorkDoneProgressState workDoneProgressState = workDoneProgress.get();
        if (workDoneProgressState == null)
            return;

        MonitorState monitorState = MONITOR_STATE.get();

        String title = workDoneProgressState.getTitle();
        if (title != null && !title.equals(monitorState.title))
        {
            monitor.setTaskName(title);
            monitorState.title = title;
        }

        String message = workDoneProgressState.getMessage();
        if (message != null && !message.equals(monitorState.message))
        {
            monitor.subTask(message);
            monitorState.message = message;
        }

        Integer percentage = workDoneProgressState.getPercentage();
        if (percentage == null || monitorState.percentage == null)
        {
            super.updateProgress(monitor); // infinite progress
            monitorState.percentage = null;
        }
        else if (percentage > monitorState.percentage)
        {
            monitor.worked(percentage - monitorState.percentage);
            monitorState.percentage = percentage;
        }
    }

    private static class MonitorState
    {
        String title;
        String message;
        Integer percentage = 0; // null iff infinite progress
    }
}
