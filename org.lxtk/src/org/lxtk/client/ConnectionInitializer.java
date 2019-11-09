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
 * TODO JavaDoc
 */
public class ConnectionInitializer
{
    private static final long MONITOR_PERIOD = 500;
    /**
     * TODO JavaDoc
     */
    protected final LanguageClient client;
    /**
     * TODO JavaDoc
     */
    protected final JsonRpcConnection<? extends LanguageServer> connection;
    /**
     * TODO JavaDoc
     */
    protected final Supplier<InitializeParams> params;
    /**
     * TODO JavaDoc
     */
    protected Duration timeout;
    /**
     * TODO JavaDoc
     */
    protected Future<InitializeResult> future;

    /**
     * TODO JavaDoc
     *
     * @param client not <code>null</code>
     * @param connection not <code>null</code>
     * @param params not <code>null</code>
     * @param timeout a positive duration
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
     * TODO JavaDoc
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
     * TODO JavaDoc
     */
    protected void sendInitializeRequest()
    {
        if (future == null)
            future = connection.getRemoteProxy().initialize(params.get());
    }

    /**
     * TODO JavaDoc
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
     * TODO JavaDoc
     *
     * @param e not <code>null</code>
     * @return initialize result (never <code>null</code>)
     */
    protected InitializeResult interrupted(InterruptedException e)
    {
        future.cancel(true);
        throw new CancellationException();
    }

    /**
     * TODO JavaDoc
     *
     * @param e not <code>null</code>
     * @return initialize result (never <code>null</code>)
     */
    protected InitializeResult timedOut(TimeoutException e)
    {
        future.cancel(true);
        throw new JsonRpcException(new TimeoutException(Messages.getString(
            "ConnectionInitializer.Error.RequestTimeout"))); //$NON-NLS-1$
    }

    /**
     * TODO JavaDoc
     *
     * @param e not <code>null</code>
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
     * TODO JavaDoc
     *
     * @param e not <code>null</code>
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
