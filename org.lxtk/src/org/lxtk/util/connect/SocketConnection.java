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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.Socket;
import java.util.Objects;

import org.lxtk.util.DisposableObject;

/**
 * TODO JavaDoc
 */
public final class SocketConnection
    extends DisposableObject
    implements StreamBasedConnection
{
    private final Socket socket;

    /**
     * TODO JavaDoc
     * <p>
     * This class will not close the given socket on connection dispose.
     * If necessary, that can be done in a dispose handler.
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
        return isDisposed() || !socket.isBound() || !socket.isConnected()
            || socket.isClosed() || socket.isInputShutdown()
            || socket.isOutputShutdown();
    }
}
