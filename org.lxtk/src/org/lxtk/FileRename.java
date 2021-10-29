/*******************************************************************************
 * Copyright (c) 2021 1C-Soft LLC.
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
package org.lxtk;

import java.net.URI;
import java.util.Objects;

/**
 * Describes a file rename.
 */
public final class FileRename
{
    private final URI oldUri;
    private final URI newUri;

    /**
     * Constructor.
     *
     * @param oldUri not <code>null</code>
     * @param newUri not <code>null</code>
     */
    public FileRename(URI oldUri, URI newUri)
    {
        this.oldUri = Objects.requireNonNull(oldUri);
        this.newUri = Objects.requireNonNull(newUri);
    }

    /**
     * Returns the URI for the original location of the renamed file.
     *
     * @return the old URI of the file (never <code>null</code>)
     */
    public URI getOldUri()
    {
        return oldUri;
    }

    /**
     * Returns the URI for the new location of the renamed file.
     *
     * @return the new URI of the file (never <code>null</code>)
     */
    public URI getNewUri()
    {
        return newUri;
    }
}
