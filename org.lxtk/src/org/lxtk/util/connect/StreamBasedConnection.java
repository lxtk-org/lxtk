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

import java.io.InputStream;
import java.io.OutputStream;

/**
 * TODO JavaDoc
 */
public interface StreamBasedConnection
    extends Connection
{
    /**
     * TODO JavaDoc
     *
     * @return the input stream (never <code>null</code>)
     */
    InputStream getInputStream();

    /**
     * TODO JavaDoc
     *
     * @return the output stream (never <code>null</code>)
     */
    OutputStream getOutputStream();
}
