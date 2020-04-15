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

/**
 * Represents a connection based on input and output streams.
 */
public interface StreamBasedConnection
    extends Connection
{
    /**
     * Returns the input stream for this connection.
     *
     * @return the input stream (never <code>null</code>)
     */
    InputStream getInputStream();

    /**
     * Returns the output stream for this connection.
     *
     * @return the output stream (never <code>null</code>)
     */
    OutputStream getOutputStream();
}
