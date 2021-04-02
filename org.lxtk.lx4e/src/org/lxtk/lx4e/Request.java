/*******************************************************************************
 * Copyright (c) 2020, 2021 1C-Soft LLC.
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

import java.text.MessageFormat;
import java.time.Duration;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.lxtk.PartialResultProgress;
import org.lxtk.WorkDoneProgress;
import org.lxtk.lx4e.internal.Activator;
import org.lxtk.lx4e.util.FutureSupport;

/**
 * A command object that sends a request to an asynchronous service
 * and waits until a response is received.
 *
 * @param <T> result type
 */
public abstract class Request<T>
{
    private Handler<T> handler;
    private Duration timeout;
    private boolean strictTimeout;
    private IProgressMonitor monitor;
    private WorkDoneProgress workDoneProgress;
    private PartialResultProgress partialResultProgress;
    private boolean mayThrow = true;
    private T defaultResult;
    private ILog log;
    private String title;

    /**
     * Sets a handler for this request.
     *
     * @param handler a request handler or <code>null</code>
     */
    public void setHandler(Handler<T> handler)
    {
        this.handler = handler;
        if (handler != null)
            handler.setRequest(this);
    }

    /**
     * Returns the handler for this request. If no handler is set,
     * sets a default handler.
     *
     * @return the request handler (never <code>null</code>)
     */
    public Handler<T> getHandler()
    {
        if (handler == null)
            setHandler(new Handler<>());
        return handler;
    }

    /**
     * Sets a timeout for this request.
     *
     * @param timeout a positive duration or <code>null</code>
     */
    public void setTimeout(Duration timeout)
    {
        if (timeout != null && (timeout.isZero() || timeout.isNegative()))
            throw new IllegalArgumentException();
        this.timeout = timeout;
    }

    /**
     * Returns the timeout for this request.
     *
     * @return a positive duration or <code>null</code>
     */
    public Duration getTimeout()
    {
        return timeout;
    }

    /**
     * Sets whether the timeout for this request (if any) is strict.
     *
     * @param value <code>true</code> if the timeout is strict, and <code>false</code> otherwise
     */
    public void setStrictTimeout(boolean value)
    {
        strictTimeout = value;
    }

    /**
     * Returns whether the timeout for this request (if any) is strict.
     *
     * @return <code>true</code> if the timeout is strict, and <code>false</code> otherwise
     */
    public boolean isStrictTimeout()
    {
        return strictTimeout;
    }

    /**
     * Sets a progress monitor for this request.
     *
     * @param monitor a progress monitor or <code>null</code>
     */
    public void setProgressMonitor(IProgressMonitor monitor)
    {
        this.monitor = monitor;
    }

    /**
     * Returns the progress monitor for this request.
     *
     * @return the progress monitor or <code>null</code>
     */
    public IProgressMonitor getProgressMonitor()
    {
        return monitor;
    }

    /**
     * Sets a work done progress for this request.
     *
     * @param workDoneProgress a work done progress or <code>null</code>
     */
    protected void setWorkDoneProgress(WorkDoneProgress workDoneProgress)
    {
        this.workDoneProgress = workDoneProgress;
    }

    /**
     * Returns the work done progress for this request.
     *
     * @return the work done progress or <code>null</code>
     */
    protected WorkDoneProgress getWorkDoneProgress()
    {
        return workDoneProgress;
    }

    /**
     * Sets a partial result progress for this request.
     *
     * @param partialResultProgress a partial result progress or <code>null</code>
     */
    protected void setPartialResultProgress(PartialResultProgress partialResultProgress)
    {
        this.partialResultProgress = partialResultProgress;
    }

    /**
     * Return the partial result progress for this request.
     *
     * @return the partial result progress or <code>null</code>
     */
    protected PartialResultProgress getPartialResultProgress()
    {
        return partialResultProgress;
    }

    /**
     * Sets whether the {@link #sendAndReceive()} method of this request
     * may throw an exception.
     *
     * @param mayThrow if <code>false</code>, exceptions will be suppressed
     */
    public void setMayThrow(boolean mayThrow)
    {
        this.mayThrow = mayThrow;
        if (!mayThrow && log == null)
            setLog(Activator.getDefault().getLog());
    }

    /**
     * Returns whether the {@link #sendAndReceive()} method of this request
     * may throw an exception.
     *
     * @return <code>true</code> if exceptions may be thrown,
     *  and <code>false</code> if exceptions will be suppressed
     */
    public boolean mayThrow()
    {
        return mayThrow;
    }

    /**
     * Sets a result that will be returned from the {@link #sendAndReceive()}
     * method of this request when an error occurs and {@link #mayThrow()}
     * is <code>false</code>.
     *
     * @param defaultResult a default result (may be <code>null</code>)
     */
    public void setDefaultResult(T defaultResult)
    {
        this.defaultResult = defaultResult;
    }

    /**
     * Sets the result that will be returned from the {@link #sendAndReceive()}
     * method of this request when an error occurs and {@link #mayThrow()}
     * is <code>false</code>.
     *
     * @return the default result (may be <code>null</code>)
     */
    public T getDefaultResult()
    {
        return defaultResult;
    }

    /**
     * Sets a log for this request.
     *
     * @param log may be <code>null</code>
     */
    public void setLog(ILog log)
    {
        this.log = log;
    }

    /**
     * Returns the log for this request.
     *
     * @return the log (may be <code>null</code>)
     */
    public ILog getLog()
    {
        return log;
    }

    /**
     * Sets a title for this request.
     *
     * @param title a title (may be <code>null</code>)
     */
    public void setTitle(String title)
    {
        this.title = title;
    }

    /**
     * Returns the title for this request.
     *
     * @return the title (may be <code>null</code>)
     */
    public String getTitle()
    {
        return title;
    }

    /**
     * Sends this request and receives a response.
     *
     * @return the result (may be <code>null</code>)
     * @throws CompletionException
     * @throws OperationCanceledException
     */
    public T sendAndReceive()
    {
        return getHandler().sendAndReceive();
    }

    /**
     * Returns the error message (if any) for this request.
     * The error message is reset each time {@link #sendAndReceive()}
     * is called.
     *
     * @return the error message (may be <code>null</code>)
     */
    public String getErrorMessage()
    {
        return getHandler().getErrorMessage();
    }

    /**
     * Sends this request.
     *
     * @return the response future (not <code>null</code>)
     */
    protected abstract CompletableFuture<T> send();

    /**
     * A customizable template for sending a request and receiving a response.
     *
     * @param <T> request result type
     */
    public static class Handler<T>
    {
        private Request<T> request;
        private CompletableFuture<T> future;
        private String errorMessage;

        /**
         * Sets a request for this handler.
         *
         * @param request a request to handle
         */
        public void setRequest(Request<T> request)
        {
            this.request = request;
        }

        /**
         * Returns the request for this handler.
         *
         * @return the associated request
         */
        public Request<T> getRequest()
        {
            return request;
        }

        /**
         * Sends the request and receives a response.
         *
         * @return the result (may be <code>null</code>)
         * @throws CompletionException
         * @throws OperationCanceledException
         */
        public T sendAndReceive()
        {
            try
            {
                setErrorMessage(null);
                setFuture(request.send());
            }
            catch (Throwable e)
            {
                return handle(e);
            }
            return receive();
        }

        /**
         * Returns the error message (if any).
         * The error message is reset each time {@link #sendAndReceive()}
         * or {@link #receive()} is called.
         *
         * @return the error message (may be <code>null</code>)
         */
        public String getErrorMessage()
        {
            return errorMessage;
        }

        /**
         * Sets the error message.
         *
         * @param errorMessage may be <code>null</code>
         */
        protected void setErrorMessage(String errorMessage)
        {
            this.errorMessage = errorMessage;
        }

        /**
         * Returns the response future.
         *
         * @return the response future
         */
        protected CompletableFuture<T> getFuture()
        {
            return future;
        }

        /**
         * Sets the response future.
         *
         * @param future the response future
         */
        protected void setFuture(CompletableFuture<T> future)
        {
            this.future = future;

            WorkDoneProgress workDoneProgress = request.getWorkDoneProgress();
            if (workDoneProgress != null)
                workDoneProgress.connectWith(future);

            PartialResultProgress partialResultProgress = request.getPartialResultProgress();
            if (partialResultProgress != null)
                partialResultProgress.connectWith(future);
        }

        /**
         * Receives a response.
         *
         * @return the result (may be <code>null</code>)
         * @throws CompletionException
         * @throws OperationCanceledException
         */
        protected T receive()
        {
            try
            {
                setErrorMessage(null);
                return doReceive();
            }
            catch (ExecutionException e)
            {
                return handle(e);
            }
            catch (InterruptedException e)
            {
                return handle(e);
            }
            catch (TimeoutException e)
            {
                return handle(e);
            }
            catch (CancellationException e)
            {
                return handle(e);
            }
            catch (OperationCanceledException e)
            {
                return handle(e);
            }
            catch (Throwable e)
            {
                return handle(e);
            }
        }

        /**
         * Actually receives a response.
         *
         * @return the result (may be <code>null</code>)
         * @throws ExecutionException
         * @throws InterruptedException
         * @throws TimeoutException
         * @throws CancellationException
         * @throws OperationCanceledException
         */
        protected T doReceive() throws ExecutionException, InterruptedException, TimeoutException
        {
            IProgressMonitor monitor = request.getProgressMonitor();
            if (monitor != null)
            {
                SubMonitor subMonitor = SubMonitor.convert(monitor, request.getTitle(), 1);
                request.setProgressMonitor(subMonitor);
                monitor = subMonitor.split(1);
            }

            WorkDoneProgress workDoneProgress = request.getWorkDoneProgress();
            PartialResultProgress partialResultProgress = request.getPartialResultProgress();

            FutureSupport<T> futureSupport;
            if (workDoneProgress == null && partialResultProgress == null)
            {
                futureSupport = new FutureSupport<>(future);
            }
            else if (workDoneProgress != null && partialResultProgress == null)
            {
                futureSupport = new ProgressSupport<>(future, workDoneProgress::getState,
                    workDoneProgress::getLastUpdated);
            }
            else if (workDoneProgress == null && partialResultProgress != null)
            {
                futureSupport = new ProgressSupport<>(future, () -> null,
                    partialResultProgress::getLastUpdated);
            }
            else
            {
                futureSupport = new ProgressSupport<>(future, workDoneProgress::getState,
                    () -> Math.max(workDoneProgress.getLastUpdated(),
                        partialResultProgress.getLastUpdated()));
            }

            Duration timeout = request.getTimeout();
            if (timeout == null)
                return futureSupport.join(monitor);

            if (futureSupport instanceof ProgressSupport)
                return ((ProgressSupport<T>)futureSupport).join(timeout.toMillis(),
                    request.isStrictTimeout(), monitor);

            return futureSupport.join(timeout.toMillis(), monitor);
        }

        /**
         * Cancels the request.
         */
        protected void cancel()
        {
            if (future != null)
                future.cancel(true);
        }

        /**
         * Handles an {@link ExecutionException}.
         *
         * @param e the exception to handle (never <code>null</code>)
         * @return the result (may be <code>null</code>)
         * @throws CompletionException
         */
        protected T handle(ExecutionException e)
        {
            String title = request.getTitle();
            String message =
                title != null ? MessageFormat.format(Messages.Request_Error_occurred__0, title)
                    : Messages.Request_Error_occurred;
            setErrorMessage(message);
            log(e, message);
            if (request.mayThrow())
                throw new CompletionException(e.getCause());
            return request.getDefaultResult();
        }

        /**
         * Handles an {@link InterruptedException}.
         *
         * @param e the exception to handle (never <code>null</code>)
         * @return the result (may be <code>null</code>)
         * @throws OperationCanceledException
         */
        protected T handle(InterruptedException e)
        {
            Thread.currentThread().interrupt();
            cancel();
            if (request.mayThrow())
                throw toOCE(e);
            return request.getDefaultResult();
        }

        /**
         * Handles a {@link TimeoutException}.
         *
         * @param e the exception to handle (never <code>null</code>)
         * @return the result (may be <code>null</code>)
         * @throws CompletionException
         * @throws OperationCanceledException
         */
        protected T handle(TimeoutException e)
        {
            long timeout = request.getTimeout().toMillis();
            String title = request.getTitle();
            String message = title != null
                ? MessageFormat.format(Messages.Request_Timeout_occurred__0__1, title, timeout)
                : MessageFormat.format(Messages.Request_Timeout_occurred__0, timeout);
            setErrorMessage(message);
            log(e, message);
            cancel();
            if (request.mayThrow())
                throw new CompletionException(e);
            return request.getDefaultResult();
        }

        /**
         * Handles a {@link CancellationException}.
         *
         * @param e the exception to handle (never <code>null</code>)
         * @return the result (may be <code>null</code>)
         * @throws OperationCanceledException
         */
        protected T handle(CancellationException e)
        {
            if (request.mayThrow())
                throw toOCE(e);
            return request.getDefaultResult();
        }

        /**
         * Handles an {@link OperationCanceledException}.
         *
         * @param e the exception to handle (never <code>null</code>)
         * @return the result (may be <code>null</code>)
         * @throws OperationCanceledException
         */
        protected T handle(OperationCanceledException e)
        {
            cancel();
            if (request.mayThrow())
                throw e;
            return request.getDefaultResult();
        }

        /**
         * Handles a {@link Throwable}. This method is invoked only when
         * the given throwable does not have a more specific handler method.
         *
         * @param e the throwable to handle (never <code>null</code>)
         * @return the result (may be <code>null</code>)
         * @throws CompletionException
         * @throws OperationCanceledException
         */
        protected T handle(Throwable e)
        {
            cancel();
            return handle(new ExecutionException(e));
        }

        /**
         * Logs an {@link ExecutionException}.
         *
         * @param e the exception to log (never <code>null</code>)
         * @param message the associated message (never <code>null</code>)
         */
        protected void log(ExecutionException e, String message)
        {
            ILog log = request.getLog();
            if (log != null)
            {
                log.log(new Status(IStatus.ERROR, log.getBundle().getSymbolicName(), message,
                    e.getCause()));
            }
        }

        /**
         * Logs a {@link TimeoutException}.
         *
         * @param e the exception to log (never <code>null</code>)
         * @param message the associated message (never <code>null</code>)
         */
        protected void log(TimeoutException e, String message)
        {
            ILog log = request.getLog();
            if (log != null)
            {
                log.log(new Status(IStatus.WARNING, log.getBundle().getSymbolicName(), message, e));
            }
        }

        private static OperationCanceledException toOCE(Throwable cause)
        {
            OperationCanceledException e = new OperationCanceledException();
            e.initCause(cause);
            return e;
        }
    }
}
