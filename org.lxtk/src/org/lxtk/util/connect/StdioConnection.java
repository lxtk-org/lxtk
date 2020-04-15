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

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

import org.lxtk.util.DisposableObject;

/**
 * Implements {@link StreamBasedConnection} over the standard input and output
 * of a process.
 */
public final class StdioConnection
    extends DisposableObject
    implements StreamBasedConnection
{
    private final Process process;

    /**
     * Constructor.
     * <p>
     * <b>Note:</b> When the connection is disposed, it will not destroy the
     * process. If necessary, clients can destroy the process in a {@link #onDispose()
     * dispose handler}.
     * </p>
     *
     * @param process not <code>null</code>
     */
    public StdioConnection(Process process)
    {
        this.process = Objects.requireNonNull(process);
    }

    @Override
    public InputStream getInputStream()
    {
        return process.getInputStream();
    }

    @Override
    public OutputStream getOutputStream()
    {
        return process.getOutputStream();
    }

    @Override
    public boolean isClosed()
    {
        return isDisposed() || !process.isAlive();
    }
}
