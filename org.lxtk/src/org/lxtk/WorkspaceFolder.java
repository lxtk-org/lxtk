/*******************************************************************************
 * Copyright (c) 2020 1C-Soft LLC.
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Represents a workspace folder.
 *
 * @see WorkspaceService
 */
public class WorkspaceFolder
{
    private final URI uri;
    private final String name;

    /**
     * Constructor.
     *
     * @param uri the folder's URI (not <code>null</code>)
     * @param name the folder's name (not <code>null</code>)
     */
    public WorkspaceFolder(URI uri, String name)
    {
        this.uri = Objects.requireNonNull(uri);
        this.name = Objects.requireNonNull(name);
    }

    /**
     * Returns the folder's URI.
     *
     * @return the folder's URI (never <code>null</code>)
     */
    public URI getUri()
    {
        return uri;
    }

    /**
     * Returns the folder's name.
     *
     * @return the folder's name (never <code>null</code>)
     */
    public String getName()
    {
        return name;
    }

    /**
     * Returns the LSP representation of this folder.
     *
     * @return the protocol representation of this folder (never <code>null</code>)
     */
    public org.eclipse.lsp4j.WorkspaceFolder toProtocol()
    {
        return new org.eclipse.lsp4j.WorkspaceFolder(DocumentUri.convert(uri), name);
    }

    /**
     * Returns the LSP representation of the given folders.
     *
     * @param folders may be <code>null</code> or empty
     * @return the protocol representation of the given folders (may be <code>null</code> or empty)
     */
    public static List<org.eclipse.lsp4j.WorkspaceFolder> toProtocol(
        Collection<WorkspaceFolder> folders)
    {
        if (folders == null)
            return null;
        List<org.eclipse.lsp4j.WorkspaceFolder> result = new ArrayList<>(folders.size());
        for (WorkspaceFolder folder : folders)
        {
            result.add(folder.toProtocol());
        }
        return result;
    }

    @Override
    public String toString()
    {
        return "{" + name + ": " + uri + '}'; //$NON-NLS-1$ //$NON-NLS-2$
    }
}
