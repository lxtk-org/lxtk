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
package org.lxtk.util.connect;

import org.lxtk.util.Disposable;
import org.lxtk.util.EventStream;

/**
 * Interface for connecting, disconnecting, and monitoring the connection state
 * of <i>connectable</i> objects.
 * <p>
 * Rather than implementing this interface directly, clients should extend
 * {@link AbstractConnectable}.
 * </p>
 *
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface Connectable
    extends Disposable
{
    /**
     * If the connection state is <code>DISCONNECTED</code> or
     * <code>DISCONNECTING</code>, changes it to <code>CONNECTING</code>
     * and starts creating a new connection. Otherwise, does nothing.
     * <p>
     * This method need not block the calling thread until the connection
     * is created.
     * </p>
     */
    void connect();

    /**
     * If the connection state is <code>CONNECTED</code> or <code>CONNECTING</code>,
     * changes it to <code>DISCONNECTING</code> and starts closing the current
     * connection. Otherwise, does nothing.
     * <p>
     * This method need not block the calling thread until the connection
     * is closed.
     * </p>
     */
    void disconnect();

    /**
     * Reconnects this object by closing the current connection, if any,
     * and creating a new one. Typical implementation is
     * <code>disconnect(); connect()</code>.
     * <p>
     * This method need not block the calling thread until the current connection
     * is closed and a new one is created.
     * </p>
     */
    void reconnect();

    /**
     * Returns a message that describes an unrecoverable connection error,
     * which caused this object to disconnect.
     *
     * @return the error message, or <code>null</code>
     */
    String getErrorMessage();

    /**
     * Returns the current connection state for this object.
     *
     * @return the connection state (never <code>null</code>)
     */
    ConnectionState getConnectionState();

    /**
     * An enumeration of connection states.
     */
    enum ConnectionState
    {
        /**
         * The connection is being created.
         */
        CONNECTING,
        /**
         * The connection has been created.
         */
        CONNECTED,
        /**
         * The connection is being closed.
         */
        DISCONNECTING,
        /**
         * The connection is closed.
         */
        DISCONNECTED
    }

    /**
     * Returns a stream of events that are emitted when the connection state changes
     * for this object.
     *
     * @return a stream of events that are emitted when the connection state changes
     *  (never <code>null</code>)
     */
    EventStream<Connectable> onDidChangeConnectionState();
}
