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
 * Describes file creation.
 */
public final class FileCreate
{
    private final URI uri;

    /**
     * Constructor.
     *
     * @param uri not <code>null</code>
     */
    public FileCreate(URI uri)
    {
        this.uri = Objects.requireNonNull(uri);
    }

    /**
     * Returns the URI for the location of the created file.
     *
     * @return the file URI (never <code>null</code>)
     */
    public URI getUri()
    {
        return uri;
    }
}
