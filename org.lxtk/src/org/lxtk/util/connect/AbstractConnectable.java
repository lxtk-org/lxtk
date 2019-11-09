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
package org.lxtk.util.connect;

import java.text.MessageFormat;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;

import org.lxtk.util.EventEmitter;
import org.lxtk.util.EventStream;
import org.lxtk.util.Log;

/**
 * TODO JavaDoc
 */
public abstract class AbstractConnectable
    implements Connectable
{
    private volatile String errorMessage;
    private volatile ConnectionState connectionState =
        ConnectionState.DISCONNECTED;
    private final EventEmitter<Connectable> onDidChangeConnectionState =
        new EventEmitter<>();
    private ExecutorService connectionExecutor;
    private ConnectionTask connectionTask;
    private Future<?> connectionFuture;

    @Override
    public void dispose()
    {
        disconnect();

        if (connectionExecutor != null)
            connectionExecutor.shutdown();
    }

    @Override
    public final synchronized void connect()
    {
        ConnectionState state = getConnectionState();
        if (state == ConnectionState.DISCONNECTED
            || state == ConnectionState.DISCONNECTING)
        {
            setErrorMessage(null);
            setConnectionState(ConnectionState.CONNECTING);
            scheduleConnect();
        }
    }

    private void scheduleConnect()
    {
        if (connectionExecutor == null)
            connectionExecutor = Executors.newSingleThreadExecutor();

        ConnectionTask task = newConnectionTask();
        connectionTask = task;

        connectionFuture = connectionExecutor.submit(() ->
        {
            try
            {
                task.connect();
            }
            catch (Throwable t)
            {
                synchronized (this)
                {
                    if (task == connectionTask
                        && getConnectionState() == ConnectionState.CONNECTING)
                    {
                        log().error("An error occurred while connecting", //$NON-NLS-1$
                            t);
                        String message = t.getMessage();
                        if (message == null)
                            message = Messages.getString(
                                "AbstractConnectable.Error.CouldNotConnect"); //$NON-NLS-1$
                        else
                            message = MessageFormat.format(Messages.getString(
                                "AbstractConnectable.Error.CouldNotConnectBecauseOf"), //$NON-NLS-1$
                                message);
                        setErrorMessage(message);
                        setConnectionState(ConnectionState.DISCONNECTED);
                    }
                }
                throw t;
            }
            synchronized (this)
            {
                if (task == connectionTask
                    && getConnectionState() == ConnectionState.CONNECTING)
                {
                    setConnectionState(ConnectionState.CONNECTED);
                }
            }
        });
    }

    @Override
    public final synchronized void disconnect()
    {
        disconnect(null);
    }

    /**
     * TODO JavaDoc
     *
     * @param errorMessage if present, describes the error that caused this call
     */
    protected final synchronized void disconnect(String errorMessage)
    {
        ConnectionState state = getConnectionState();
        if (state == ConnectionState.CONNECTED
            || state == ConnectionState.CONNECTING)
        {
            setErrorMessage(errorMessage);
            setConnectionState(ConnectionState.DISCONNECTING);
            scheduleDisconnect();
        }
    }

    private void scheduleDisconnect()
    {
        ConnectionTask task = connectionTask;

        connectionFuture.cancel(true);

        connectionExecutor.execute(() ->
        {
            try
            {
                task.disconnect();
            }
            catch (Throwable t)
            {
                log().error("An error occurred while diconnecting", t); //$NON-NLS-1$
            }
            finally
            {
                synchronized (this)
                {
                    if (task == connectionTask
                        && getConnectionState() == ConnectionState.DISCONNECTING)
                    {
                        setConnectionState(ConnectionState.DISCONNECTED);
                    }
                }
            }
        });
    }

    @Override
    public final void reconnect()
    {
        disconnect();
        connect();
    }

    @Override
    public final ConnectionState getConnectionState()
    {
        return connectionState;
    }

    private void setConnectionState(ConnectionState connectionState)
    {
        this.connectionState = connectionState;
        ForkJoinPool.commonPool().execute(() -> onDidChangeConnectionState.fire(
            this));
    }

    @Override
    public final EventStream<Connectable> onDidChangeConnectionState()
    {
        return onDidChangeConnectionState;
    }

    @Override
    public final String getErrorMessage()
    {
        return errorMessage;
    }

    private void setErrorMessage(String errorMessage)
    {
        this.errorMessage = errorMessage;
    }

    /**
     * TODO JavaDoc
     *
     * @return a new connection task (never <code>null</code>)
     */
    protected abstract ConnectionTask newConnectionTask();

    /**
     * TODO JavaDoc
     *
     * @return a log (never <code>null</code>)
     */
    protected abstract Log log();

    /**
     * TODO JavaDoc
     */
    protected static abstract class ConnectionTask
    {
        /**
         * TODO JavaDoc
         * <p>
         * This method may not be called more than once. A successful call
         * to this method must eventually be followed by a call to the
         * disconnect() method.
         * </p>
         * @see #disconnect()
         */
        public abstract void connect();

        /**
         * TODO JavaDoc
         * <ul>
         * <li>This method may not be called more than once.</li>
         * <li>This method may not be called before the connect() method.</li>
         * <li>This method must be called if the connect() method was called
         * successfully (i.e., did not throw an exception).</li>
         * <li>This method may be called even if the connect() method was not
         * called.</li>
         * <li>This method will not be called if the connect() method threw
         * an exception.</li>
         * </ul>
         */
        public abstract void disconnect();
    }

    /**
     * TODO JavaDoc
     */
    protected abstract class ConnectionCloseHandler
        implements Runnable
    {
        private final ConnectionTask task;

        /**
         * TODO JavaDoc
         *
         * @param task not <code>null</code>
         */
        public ConnectionCloseHandler(ConnectionTask task)
        {
            this.task = Objects.requireNonNull(task);
        }

        @Override
        public void run()
        {
            synchronized (AbstractConnectable.this)
            {
                if (task == connectionTask)
                {
                    ConnectionState state = getConnectionState();
                    if (state == ConnectionState.CONNECTED
                        || state == ConnectionState.CONNECTING)
                    {
                        if (shouldReconnect())
                        {
                            log().warning(
                                "Connection got closed unexpectedly. Attempting to reconnect"); //$NON-NLS-1$
                            reconnect();
                        }
                        else
                        {
                            log().error("Connection got closed unexpectedly"); //$NON-NLS-1$
                            disconnect(Messages.getString(
                                "AbstractConnectable.Error.ConnectionAborted")); //$NON-NLS-1$
                        }
                    }
                }
            }
        }

        /**
         * Indicates whether to reconnect in case the connection
         * got closed unexpectedly.
         *
         * @return <code>true</code> to reconnect,
         *  and <code>false</code> otherwise
         */
        protected abstract boolean shouldReconnect();
    }
}
