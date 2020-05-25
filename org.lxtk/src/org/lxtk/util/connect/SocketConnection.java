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
package org.lxtk.util.connect;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.Socket;
import java.util.Objects;

import org.lxtk.util.DisposableObject;

/**
 * Implements {@link StreamBasedConnection} over a socket.
 */
public final class SocketConnection
    extends DisposableObject
    implements StreamBasedConnection
{
    private final Socket socket;

    /**
     * Constructor.
     * <p>
     * <b>Note:</b> When the connection is disposed, it will not close the socket.
     * If necessary, clients can close the socket in a {@link #onDispose()
     * dispose handler}.
     * </p>
     *
     * @param socket not <code>null</code>
     */
    public SocketConnection(Socket socket)
    {
        this.socket = Objects.requireNonNull(socket);
    }

    @Override
    public InputStream getInputStream()
    {
        try
        {
            return socket.getInputStream();
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public OutputStream getOutputStream()
    {
        try
        {
            return socket.getOutputStream();
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public boolean isClosed()
    {
        return isDisposed() || !socket.isBound() || !socket.isConnected() || socket.isClosed()
            || socket.isInputShutdown() || socket.isOutputShutdown();
    }
}
