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
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BooleanSupplier;

import org.eclipse.lsp4j.ClientCapabilities;
import org.eclipse.lsp4j.DocumentFilter;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.InitializedParams;
import org.eclipse.lsp4j.services.LanguageServer;
import org.lxtk.jsonrpc.JsonRpcConnection;
import org.lxtk.jsonrpc.JsonRpcConnectionFactory;
import org.lxtk.util.Policy;
import org.lxtk.util.SafeRun;
import org.lxtk.util.connect.AbstractConnectable;

/**
 * Provides API and partial implementation for controlling a language client
 * talking to a language server according to the Language Server Protocol.
 * <p>
 * This implementation is thread-safe.
 * </p>
 *
 * @param <S> server interface type
 */
public abstract class AbstractLanguageClientController<S extends LanguageServer>
    extends AbstractConnectable
{
    private BooleanSupplier autoReconnect =
        Policy.upTo(5).in(Duration.ofSeconds(180)).thenReset()::check; // like in VS Code

    /**
     * Sets the auto-reconnect policy. In case the current connection to the
     * server gets closed unexpectedly, the client will check the auto-reconnect
     * policy to decide whether to {@link #reconnect()} or {@link #disconnect()}
     * itself.
     *
     * @param policy not <code>null</code>
     * @see Policy
     */
    protected final void setAutoReconnect(BooleanSupplier policy)
    {
        autoReconnect = Objects.requireNonNull(policy);
    }

    /**
     * Checks the auto-reconnect policy.
     *
     * @return <code>true</code> if auto-reconnect is enabled,
     *  and <code>false</code> otherwise
     * @see #setAutoReconnect(BooleanSupplier)
     */
    protected final boolean isAutoReconnect()
    {
        return autoReconnect.getAsBoolean();
    }

    /**
     * Returns the timeout for the 'initialize' request.
     *
     * @return initialize timeout (a positive duration)
     */
    protected Duration getInitializeTimeout()
    {
        return Duration.ofSeconds(10);
    }

    /**
     * Returns the timeout for the 'shutdown' request.
     *
     * @return shutdown timeout (a positive duration)
     */
    protected Duration getShutdownTimeout()
    {
        return Duration.ofSeconds(5);
    }

    /**
     * Returns the document selector that will be passed to
     * the {@link Feature#initialize} method.
     *
     * @return a document selector (may be <code>null</code>)
     */
    protected abstract List<DocumentFilter> getDocumentSelector();

    /**
     * Returns the server interface class (remote service interface).
     *
     * @return server interface class (never <code>null</code>)
     */
    protected abstract Class<S> getServerInterface();

    /**
     * Returns a language client (local service) object. This method is called
     * each time a connection is created. The returned object will automatically
     * be {@link AbstractLanguageClient#dispose() disposed} when the connection
     * gets closed.
     *
     * @return language client (never <code>null</code>)
     */
    protected abstract AbstractLanguageClient<S> getLanguageClient();

    /**
     * Returns a {@link JsonRpcConnectionFactory} for creating a connection
     * to the language server.
     *
     * @return connection factory (never <code>null</code>)
     */
    protected abstract JsonRpcConnectionFactory<S> getConnectionFactory();

    /**
     * Returns a new instance of {@link ConnectionInitializer} for the given
     * connection between the language client and server.
     *
     * @param client never <code>null</code>
     * @param connection never <code>null</code>
     * @return a new connection initializer (not <code>null</code>)
     */
    protected ConnectionInitializer newConnectionInitializer(AbstractLanguageClient<S> client,
        JsonRpcConnection<S> connection)
    {
        return new ConnectionInitializer(client, connection, () -> computeInitializeParams(client),
            getInitializeTimeout());
    }

    /**
     * Computes the {@link InitializeParams} for the language client.
     *
     * @param client not <code>null</code>
     * @return initialize params (never <code>null</code>)
     */
    protected InitializeParams computeInitializeParams(AbstractLanguageClient<S> client)
    {
        ClientCapabilities capabilities = new ClientCapabilities();
        client.fillClientCapabilities(capabilities);

        InitializeParams params = new InitializeParams();
        params.setCapabilities(capabilities);
        client.fillInitializeParams(params);

        return params;
    }

    @Override
    protected final ConnectionTask newConnectionTask()
    {
        return new ConnectionTask()
        {
            private Runnable connectionCloser;

            @Override
            public void connect()
            {
                SafeRun.run(rollback ->
                {
                    AbstractLanguageClient<S> client = getLanguageClient();
                    rollback.add(() -> client.dispose());

                    ExecutorService messageListener = Executors.newSingleThreadExecutor();
                    rollback.add(() -> messageListener.shutdown());

                    JsonRpcConnection<S> connection = getConnectionFactory().newConnection(client,
                        getServerInterface(), messageListener, null);
                    rollback.add(() -> connection.dispose());

                    S server = connection.getRemoteProxy();
                    rollback.add(() -> server.exit());

                    InitializeResult result =
                        newConnectionInitializer(client, connection).initialize();
                    rollback.add(() ->
                    {
                        try
                        {
                            server.shutdown().get(getShutdownTimeout().toMillis(),
                                TimeUnit.MILLISECONDS);
                        }
                        catch (InterruptedException | ExecutionException | TimeoutException e)
                        {
                            throw new RuntimeException(e);
                        }
                    });

                    server.initialized(new InitializedParams());

                    ExecutorService grimReaper = Executors.newSingleThreadExecutor();
                    rollback.add(() -> grimReaper.shutdown());
                    CompletableFuture<?> connectionMonitor = connection.monitor(grimReaper);
                    rollback.add(() -> connectionMonitor.cancel(true));
                    connectionMonitor.thenRun(new ConnectionCloseHandler(this)
                    {
                        @Override
                        protected boolean shouldReconnect()
                        {
                            return isAutoReconnect();
                        }
                    });

                    client.initialize(server, result.getCapabilities(), getDocumentSelector());

                    rollback.setLogger(
                        t -> log().error("An error occurred while disconnecting", t)); //$NON-NLS-1$
                    connectionCloser = rollback;
                });
            }

            @Override
            public void disconnect()
            {
                if (connectionCloser != null)
                    connectionCloser.run();
            }
        };
    }
}
