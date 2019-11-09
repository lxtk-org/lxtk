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
 * TODO JavaDoc
 *
 * @param <T> result type 
 */
public class EclipseFuture<T>
    implements Future<T>
{
    private final Future<T> future;
    private final long monitorPeriod;

    /**
     * TODO JavaDoc
     *
     * @param <V> future result type
     * @param future not <code>null</code>
     * @return an {@link EclipseFuture} based on the given future
     *  (never <code>null</code>)
     */
    public static <V> EclipseFuture<V> of(Future<V> future)
    {
        if (future instanceof EclipseFuture)
            return (EclipseFuture<V>)future;
        return new EclipseFuture<>(future, 50);
    }

    private EclipseFuture(Future<T> future, long monitorPeriod)
    {
        this.future = Objects.requireNonNull(future);
        this.monitorPeriod = monitorPeriod;
    }

    /**
     * TODO JavaDoc
     *
     * @param value a positive duration
     * @return an {@link EclipseFuture} based on this future,
     *  with the given monitor period
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

    @Override
    public T get() throws InterruptedException, ExecutionException
    {
        return future.get();
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException,
        ExecutionException, TimeoutException
    {
        return future.get(timeout, unit);
    }

    /**
     * TODO JavaDoc
     *
     * @param monitor a progress monitor, or <code>null</code>
     *  if progress reporting is not desired. The caller must not rely on
     *  {@link IProgressMonitor#done()} having been called by this method
     * @return the computed result
     * @throws OperationCanceledException if the computation was cancelled
     *  or if cancellation has been requested via the progress monitor
     * @throws InterruptedException if the current thread was interrupted
     *  while waiting
     * @throws ExecutionException if the computation threw an exception
     */
    public T get(IProgressMonitor monitor) throws InterruptedException,
        ExecutionException
    {
        SubMonitor subMonitor = SubMonitor.convert(monitor);
        subMonitor.checkCanceled();
        while (true)
        {
            // Regardless of the amount of progress reported so far, use 0.01%
            // of the space remaining in the monitor (logarithmic progress).
            subMonitor.setWorkRemaining(10000);
            try
            {
                return get(monitorPeriod, TimeUnit.MILLISECONDS);
            }
            catch (CancellationException cause)
            {
                OperationCanceledException e = new OperationCanceledException();
                e.initCause(cause);
                throw e;
            }
            catch (TimeoutException e)
            {
                subMonitor.split(1);
            }
        }
    }

    /**
     * TODO JavaDoc
     *
     * @param timeout the maximum time to wait, a positive duration
     * @param monitor a progress monitor, or <code>null</code>
     *  if progress reporting is not desired. The caller must not rely on
     *  {@link IProgressMonitor#done()} having been called by this method
     * @return the computed result
     * @throws OperationCanceledException if the computation was cancelled
     *  or if cancellation has been requested via the progress monitor
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
            // Regardless of the amount of progress reported so far, use 0.01%
            // of the space remaining in the monitor (logarithmic progress).
            subMonitor.setWorkRemaining(10000);
            try
            {
                return get(Math.min(monitorPeriod, timeRemaining),
                    TimeUnit.MILLISECONDS);
            }
            catch (CancellationException cause)
            {
                OperationCanceledException e = new OperationCanceledException();
                e.initCause(cause);
                throw e;
            }
            catch (TimeoutException e)
            {
                subMonitor.split(1);
                timeRemaining -= monitorPeriod;
                if (timeRemaining <= 0)
                    throw e;
            }
        }
    }
}
