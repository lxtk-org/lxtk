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

import org.lxtk.util.Disposable;
import org.lxtk.util.EventStream;

/**
 * TODO JavaDoc
 *
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface Connectable
    extends Disposable
{
    /**
     * If the connection state is DISCONNECTED or DISCONNECTING,
     * changes it to CONNECTING and starts creating a new connection.
     * Otherwise, does nothing.
     * <p>
     * This method need not block the calling thread until the connection
     * is created.
     * </p>
     */
    void connect();

    /**
     * If the connection state is CONNECTED or CONNECTING, changes it
     * to DISCONNECTING and starts closing the current connection.
     * Otherwise, does nothing.
     * <p>
     * This method need not block the calling thread until the connection
     * is closed.
     * </p>
     */
    void disconnect();

    /**
     * TODO JavaDoc
     *
     */
    void reconnect();

    /**
     * Returns a message that describes an unrecoverable connection error,
     * which caused this instance to disconnect.
     *
     * @return the error message, or <code>null</code>
     */
    String getErrorMessage();

    /**
     * TODO JavaDoc
     *
     * @return the connection state (never <code>null</code>)
     */
    ConnectionState getConnectionState();

    /**
     * TODO JavaDoc
     */
    enum ConnectionState
    {
        /**
         * TODO JavaDoc
         */
        CONNECTING,
        /**
         * TODO JavaDoc
         */
        CONNECTED,
        /**
         * TODO JavaDoc
         */
        DISCONNECTING,
        /**
         * TODO JavaDoc
         */
        DISCONNECTED
    }

    /**
     * TODO JavaDoc
     *
     * @return an event emitter firing when the connection state changes
     *  (never <code>null</code>)
     */
    EventStream<Connectable> onDidChangeConnectionState();
}
