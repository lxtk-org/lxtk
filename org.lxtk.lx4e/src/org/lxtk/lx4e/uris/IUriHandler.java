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
package org.lxtk.lx4e.uris;

import java.net.URI;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.handly.buffer.IBuffer;

/**
 * TODO JavaDoc
 */
public interface IUriHandler
{
    /**
     * TODO JavaDoc
     *
     * @param uri not <code>null</code>
     * @return a model element corresponding to the given URI,
     *  or <code>null</code> if none
     */
    Object getCorrespondingElement(URI uri);

    /**
     * TODO JavaDoc
     *
     * @param uri not <code>null</code>
     * @return whether the given URI has contents, or <code>null</code>
     *  if unknown
     */
    Boolean exists(URI uri);

    /**
     * TODO JavaDoc
     *
     * @param uri not <code>null</code>
     * @return a buffer for the given URI, or <code>null</code> if none.
     *  It is the client responsibility to {@link IBuffer#release() release}
     *  the returned buffer after it is no longer needed
     * @throws CoreException
     */
    IBuffer getBuffer(URI uri) throws CoreException;

    /**
     * TODO JavaDoc
     *
     * @param uri not <code>null</code>
     * @return a specialized representation for the given URI suitable for human
     *  consumption, or <code>null</code> if no specialized representation is
     *  available
     */
    String toDisplayString(URI uri);
}
