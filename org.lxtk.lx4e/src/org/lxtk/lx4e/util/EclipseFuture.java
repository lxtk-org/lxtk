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
package org.lxtk.lx4e.util;

import java.time.Duration;
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
 * A {@link Future} that supports progress reporting and cancellation via
 * {@link IProgressMonitor}.
 *
 * @param <T> result type
 * @deprecated Replaced with {@link FutureSupport} class hierarchy
 */
public class EclipseFuture<T>
    implements Future<T>
{
    private final Future<T> future;
    private final int monitorPeriod;

    /**
     * Returns an {@link EclipseFuture} based on the given future.
     *
     * @param <T> future result type
     * @param future not <code>null</code>
     * @return an <code>EclipseFuture</code> based on the given future
     *  (never <code>null</code>)
     */
    public static <T> EclipseFuture<T> of(Future<T> future)
    {
        if (future instanceof EclipseFuture)
            return (EclipseFuture<T>)future;
        return new EclipseFuture<>(future, 100);
    }

    private EclipseFuture(Future<T> future, long monitorPeriod)
    {
        this.future = Objects.requireNonNull(future);
        if (monitorPeriod < 10)
            monitorPeriod = 10;
        else if (monitorPeriod > 500)
            monitorPeriod = 500;
        this.monitorPeriod = (int)monitorPeriod;
    }

    /**
     * Returns an {@link EclipseFuture} that is based on this future
     * and has the given monitor period.
     *
     * @param value a positive duration
     * @return an <code>EclipseFuture</code> that is based on this future
     *  and has the given monitor period
     */
    public EclipseFuture<T> withMonitorPeriod(Duration value)
    {
        if (value.isNegative() || value.isZero())
            throw new IllegalArgumentException();
        return new EclipseFuture<>(future, value.toMillis());
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning)
    {
        return future.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled()
    {
        return future.isCancelled();
    }

    @Override
    public boolean isDone()
    {
        return future.isDone();
    }

    /**
     * {@inheritDoc}
     * @throws CancellationException {@inheritDoc}
     */
    @Override
    public T get() throws InterruptedException, ExecutionException
    {
        return future.get();
    }

    /**
     * {@inheritDoc}
     * @throws CancellationException {@inheritDoc}
     */
    @Override
    public T get(long timeout, TimeUnit unit)
        throws InterruptedException, ExecutionException, TimeoutException
    {
        return future.get(timeout, unit);
    }

    /**
     * Waits if necessary for the computation to complete, using the given
     * monitor to report progress and respond to cancellation, and returns
     * the computed result.
     *
     * @param monitor a progress monitor, or <code>null</code>
     *  if progress reporting is not desired. The caller must not rely on
     *  {@link IProgressMonitor#done()} having been called by the receiver
     * @return the computed result
     * @throws CancellationException if the computation was cancelled
     * @throws OperationCanceledException if cancellation has been requested
     *  via the progress monitor
     * @throws InterruptedException if the current thread was interrupted
     *  while waiting
     * @throws ExecutionException if the computation threw an exception
     */
    public T get(IProgressMonitor monitor) throws InterruptedException, ExecutionException
    {
        SubMonitor subMonitor = SubMonitor.convert(monitor);
        subMonitor.checkCanceled();
        while (true)
        {
            subMonitor.setWorkRemaining(2000);
            try
            {
                return get(monitorPeriod, TimeUnit.MILLISECONDS);
            }
            catch (TimeoutException e)
            {
                subMonitor.split(monitorPeriod);
            }
        }
    }

    /**
     * Waits if necessary for at most the given time for the computation
     * to complete, using the given monitor to report progress and respond
     * to cancellation, and returns the computed result, if available.
     *
     * @param timeout the maximum time to wait, a positive duration
     * @param monitor a progress monitor, or <code>null</code>
     *  if progress reporting is not desired. The caller must not rely on
     *  {@link IProgressMonitor#done()} having been called by the receiver
     * @return the computed result
     * @throws CancellationException if the computation was cancelled
     * @throws OperationCanceledException if cancellation has been requested
     *  via the progress monitor
     * @throws InterruptedException if the current thread was interrupted
     *  while waiting
     * @throws ExecutionException if the computation threw an exception
     * @throws TimeoutException if the wait timed out
     */
    public T get(Duration timeout, IProgressMonitor monitor)
        throws InterruptedException, ExecutionException, TimeoutException
    {
        if (timeout.isNegative() || timeout.isZero())
            throw new IllegalArgumentException();
        long timeRemaining = timeout.toMillis();
        SubMonitor subMonitor = SubMonitor.convert(monitor);
        subMonitor.checkCanceled();
        while (true)
        {
            subMonitor.setWorkRemaining(2000);
            try
            {
                return get(Math.min(monitorPeriod, timeRemaining), TimeUnit.MILLISECONDS);
            }
            catch (TimeoutException e)
            {
                subMonitor.split(monitorPeriod);
                timeRemaining -= monitorPeriod;
                if (timeRemaining <= 0)
                    throw e;
            }
        }
    }
}
