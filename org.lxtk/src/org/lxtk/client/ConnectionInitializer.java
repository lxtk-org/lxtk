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
package org.lxtk.client;

import java.time.Duration;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import org.eclipse.lsp4j.InitializeError;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.MessageActionItem;
import org.eclipse.lsp4j.MessageType;
import org.eclipse.lsp4j.ShowMessageRequestParams;
import org.eclipse.lsp4j.jsonrpc.JsonRpcException;
import org.eclipse.lsp4j.jsonrpc.ResponseErrorException;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseError;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageServer;
import org.lxtk.jsonrpc.JsonRpcConnection;

/**
 * Performs initialization of a given LSP client/server connection.
 */
public class ConnectionInitializer
{
    private static final long MONITOR_PERIOD = 500;
    /**
     * The given client (never <code>null</code>).
     */
    protected final LanguageClient client;
    /**
     * The given client/server connection (never <code>null</code>).
     */
    protected final JsonRpcConnection<? extends LanguageServer> connection;
    /**
     * The given supplier of initialize params (never <code>null</code>).
     */
    protected final Supplier<InitializeParams> params;
    /**
     * The given initialization timeout (a positive duration).
     */
    protected Duration timeout;
    /**
     * The initialize request future.
     */
    protected Future<InitializeResult> future;

    /**
     * Constructor.
     *
     * @param client a {@link LanguageClient} (not <code>null</code>)
     * @param connection a {@link JsonRpcConnection} to the language server
     *  (not <code>null</code>)
     * @param params a supplier of the initialize params (not <code>null</code>)
     * @param timeout initialization timeout (a positive duration)
     */
    public ConnectionInitializer(LanguageClient client,
        JsonRpcConnection<? extends LanguageServer> connection,
        Supplier<InitializeParams> params, Duration timeout)
    {
        this.client = Objects.requireNonNull(client);
        this.connection = Objects.requireNonNull(connection);
        this.params = Objects.requireNonNull(params);
        if (timeout.isNegative() || timeout.isZero())
            throw new IllegalArgumentException();
        this.timeout = timeout;
    }

    /**
     * Initializes the given client/server connection.
     *
     * @return initialize result (never <code>null</code>)
     */
    public InitializeResult initialize()
    {
        sendInitializeRequest();
        try
        {
            return getInitializeResult();
        }
        catch (InterruptedException e)
        {
            return interrupted(e);
        }
        catch (TimeoutException e)
        {
            return timedOut(e);
        }
        catch (ExecutionException e)
        {
            return failed(e);
        }
    }

    /**
     * Sends the initialize request.
     */
    protected void sendInitializeRequest()
    {
        if (future == null)
            future = connection.getRemoteProxy().initialize(params.get());
    }

    /**
     * Waits until the initialize result is ready or an exception is thrown.
     *
     * @return initialize result (never <code>null</code>)
     * @throws InterruptedException if the current thread was interrupted
     *  while waiting
     * @throws ExecutionException if the computation threw an exception
     * @throws TimeoutException if the wait timed out
     */
    protected InitializeResult getInitializeResult()
        throws InterruptedException, ExecutionException, TimeoutException
    {
        long timeRemaining = timeout.toMillis();
        while (true)
        {
            if (connection.isClosed())
                throw new JsonRpcException(new TimeoutException(
                    Messages.getString(
                        "ConnectionInitializer.Error.ConnectionAborted"))); //$NON-NLS-1$
            try
            {
                return future.get(Math.min(timeRemaining, MONITOR_PERIOD),
                    TimeUnit.MILLISECONDS);
            }
            catch (TimeoutException e)
            {
                timeRemaining -= MONITOR_PERIOD;
                if (timeRemaining <= 0)
                    throw e;
            }
        }
    }

    /**
     * Called in case {@link #getInitializeResult()} threw
     * an {@link InterruptedException}.
     *
     * @param e the thrown exception (never <code>null</code>)
     * @return initialize result (never <code>null</code>)
     */
    protected InitializeResult interrupted(InterruptedException e)
    {
        future.cancel(true);
        throw new CancellationException();
    }

    /**
     * Called in case {@link #getInitializeResult()} threw
     * a {@link TimeoutException}.
     *
     * @param e the thrown exception (never <code>null</code>)
     * @return initialize result (never <code>null</code>)
     */
    protected InitializeResult timedOut(TimeoutException e)
    {
        future.cancel(true);
        throw new JsonRpcException(new TimeoutException(Messages.getString(
            "ConnectionInitializer.Error.RequestTimeout"))); //$NON-NLS-1$
    }

    /**
     * Called in case {@link #getInitializeResult()} threw
     * an {@link ExecutionException}.
     *
     * @param e the thrown exception (never <code>null</code>)
     * @return initialize result (never <code>null</code>)
     */
    protected InitializeResult failed(ExecutionException e)
    {
        if (shouldRetry(e))
        {
            // retry sending the initialize request
            future = null;
            return initialize();
        }
        throw new CompletionException(e.getCause());
    }

    /**
     * Determines whether a failed initialize request should be retried.
     *
     * @param e the {@link ExecutionException} describing the cause of the
     *  failure (never <code>null</code>)
     * @return <code>true</code> if the initialize request should be retried,
     *  and <code>false</code> otherwise
     */
    protected boolean shouldRetry(ExecutionException e)
    {
        Throwable cause = e.getCause();
        if (!(cause instanceof ResponseErrorException))
            return false;

        ResponseError error =
            ((ResponseErrorException)cause).getResponseError();
        Object data = error.getData();
        if (!(data instanceof InitializeError)
            || !((InitializeError)data).isRetry())
            return false;

        ShowMessageRequestParams params = new ShowMessageRequestParams();
        params.setType(MessageType.Error);
        params.setMessage(error.getMessage());
        String retry = Messages.getString("ConnectionInitializer.Action.Retry"); //$NON-NLS-1$
        params.setActions(Arrays.asList(new MessageActionItem(retry),
            new MessageActionItem(Messages.getString(
                "ConnectionInitializer.Action.Cancel")))); //$NON-NLS-1$
        MessageActionItem answer = null;
        try
        {
            answer = client.showMessageRequest(params).join();
        }
        catch (Throwable t)
        {
            // ignore
        }
        return answer != null && retry.equals(answer.getTitle());
    }
}
