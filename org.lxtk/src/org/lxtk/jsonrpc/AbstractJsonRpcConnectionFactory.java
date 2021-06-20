/*******************************************************************************
 * Copyright (c) 2019, 2021 1C-Soft LLC.
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
package org.lxtk.jsonrpc;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Function;

import org.eclipse.lsp4j.jsonrpc.Endpoint;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.jsonrpc.MessageConsumer;
import org.lxtk.util.connect.StreamBasedConnection;

/**
 * Partial implementation of a {@link JsonRpcConnectionFactory} that creates
 * JSON-RPC connections on top of {@link StreamBasedConnection}s.
 *
 * @param <T> remote interface type
 */
public abstract class AbstractJsonRpcConnectionFactory<T>
    implements JsonRpcConnectionFactory<T>
{
    @Override
    public JsonRpcConnection<T> newConnection(Object localService, Class<T> remoteInterface,
        ExecutorService executorService, Function<MessageConsumer, MessageConsumer> wrapper)
    {
        StreamBasedConnection c = newStreamBasedConnection();
        try
        {
            Launcher<T> launcher = newLauncher(localService, remoteInterface, c.getInputStream(),
                c.getOutputStream(), executorService, wrapper);
            Future<?> future = launcher.startListening();
            return new JsonRpcConnection<>()
            {
                @Override
                public T getRemoteProxy()
                {
                    return launcher.getRemoteProxy();
                }

                @Override
                public Endpoint getRemoteEndpoint()
                {
                    return launcher.getRemoteEndpoint();
                }

                @Override
                public boolean isClosed()
                {
                    return c.isClosed() || future.isDone();
                }

                @Override
                public void dispose()
                {
                    c.dispose();
                }

                @Override
                public CompletionStage<?> onDispose()
                {
                    return c.onDispose();
                }
            };
        }
        catch (Throwable t)
        {
            try
            {
                c.dispose();
            }
            catch (Throwable t2)
            {
                t.addSuppressed(t);
            }
            throw t;
        }
    }

    /**
     * Returns a new {@link Launcher}.
     *
     * @param localService the object that receives method calls from the remote service
     *  (not <code>null</code>)
     * @param remoteInterface an interface on which RPC methods are looked up
     *  (not <code>null</code>)
     * @param in input stream to listen for incoming messages
     *  (not <code>null</code>)
     * @param out output stream to send outgoing messages
     *  (not <code>null</code>)
     * @param executorService the executor service used to start threads
     *  (may be <code>null</code>)
     * @param wrapper a function for plugging in additional message consumers
     *  (may be <code>null</code>)
     * @return a new launcher (never <code>null</code>)
     */
    protected Launcher<T> newLauncher(Object localService, Class<T> remoteInterface, InputStream in,
        OutputStream out, ExecutorService executorService,
        Function<MessageConsumer, MessageConsumer> wrapper)
    {
        return Launcher.createIoLauncher(localService, remoteInterface, in, out, executorService,
            wrapper);
    }

    /**
     * Returns a new {@link StreamBasedConnection}. The returned connection
     * will be used for creating a JSON-RPC connection.
     *
     * @return a new stream-based connection (never <code>null</code>)
     */
    protected abstract StreamBasedConnection newStreamBasedConnection();
}
