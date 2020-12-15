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
package org.lxtk.lx4e.util;

import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;

/**
 * Basic support for progress monitoring of an asynchronous operation.
 *
 * @param <T> type of operation result 
 */
public class FutureSupport<T>
{
    /**
     * The operation future.
     */
    protected final Future<T> future;
    /**
     * The monitor period in milliseconds.
     */
    protected int monitorPeriod = 100;

    /**
     * Constructor.
     *
     * @param future the operation future (not <code>null</code>)
     */
    public FutureSupport(Future<T> future)
    {
        this.future = Objects.requireNonNull(future);
    }

    /**
     * Sets the monitor period.
     *
     * @param value in milliseconds
     */
    public void setMonitorPeriod(int value)
    {
        if (value < 10)
            value = 10;
        else if (value > 500)
            value = 500;
        monitorPeriod = value;
    }

    /**
     * Waits if necessary for the operation to complete, using the given
     * monitor to report operation progress and respond to cancellation,
     * and returns the operation result.
     *
     * @param monitor a progress monitor, or <code>null</code>
     *  if progress reporting is not desired. The caller must not rely on
     *  {@link IProgressMonitor#done()} having been called by the receiver
     * @return the operation result
     * @throws CancellationException if the operation was cancelled via the future
     * @throws OperationCanceledException if cancellation has been requested
     *  via the progress monitor
     * @throws InterruptedException if the current thread was interrupted while waiting
     * @throws ExecutionException if the operation threw an exception
     */
    public T join(IProgressMonitor monitor) throws InterruptedException, ExecutionException
    {
        SubMonitor subMonitor = convert(monitor);
        while (true)
        {
            checkCanceled(subMonitor);
            try
            {
                return future.get(monitorPeriod, TimeUnit.MILLISECONDS);
            }
            catch (TimeoutException e)
            {
                updateProgress(subMonitor);
            }
        }
    }

    /**
     * Waits if necessary until either the operation completes or the given timeout has expired,
     * using the given monitor to report operation progress and respond to cancellation,
     * and returns the operation result, if available.
     *
     * @param timeoutMillis timeout value
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
    public T join(long timeoutMillis, IProgressMonitor monitor)
        throws InterruptedException, ExecutionException, TimeoutException
    {
        SubMonitor subMonitor = convert(monitor);
        long startTime = System.currentTimeMillis();
        while (true)
        {
            checkCanceled(subMonitor);
            try
            {
                return future.get(
                    Math.min(Math.max(0, startTime + timeoutMillis - System.currentTimeMillis()),
                        monitorPeriod),
                    TimeUnit.MILLISECONDS);
            }
            catch (TimeoutException e)
            {
                if (System.currentTimeMillis() - startTime >= timeoutMillis)
                    throw e;

                updateProgress(subMonitor);
            }
        }
    }

    /**
     * Converts the given progress monitor into a <code>SubMonitor</code>. 
     *
     * @param monitor may be <code>null</code>
     * @return the corresponding SubMonitor (not <code>null</code>)
     */
    protected SubMonitor convert(IProgressMonitor monitor)
    {
        return SubMonitor.convert(monitor);
    }

    /**
     * Checks whether cancellation has been requested using the given SubMonitor and throws an
     * {@link OperationCanceledException} if it was the case.
     *
     * @param monitor not <code>null</code>
     */
    protected void checkCanceled(SubMonitor monitor)
    {
        monitor.checkCanceled();
    }

    /**
     * Updates the operation progress using the given SubMonitor.
     *
     * @param monitor not <code>null</code>
     */
    protected void updateProgress(SubMonitor monitor)
    {
        monitor.setWorkRemaining(2000); // infinite progress
        monitor.worked(monitorPeriod);
    }
}
