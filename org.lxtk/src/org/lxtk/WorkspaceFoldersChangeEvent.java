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

import java.util.Collection;

/**
 * An event describing a change to a collection of workspace folders.
 *
 * @see WorkspaceFolder
 */
public final class WorkspaceFoldersChangeEvent
{
    private final Collection<WorkspaceFolder> oldFolders;
    private final Collection<WorkspaceFolder> newFolders;

    /**
     * Constructor.
     *
     * @param oldFolders may be <code>null</code> or empty
     * @param newFolders may be <code>null</code> or empty
     */
    public WorkspaceFoldersChangeEvent(Collection<WorkspaceFolder> oldFolders,
        Collection<WorkspaceFolder> newFolders)
    {
        this.oldFolders = oldFolders;
        this.newFolders = newFolders;
    }

    /**
     * Returns the old folders.
     *
     * @return the old folders (may be <code>null</code> or be empty).
     *  Clients <b>must not</b> modify the returned collection.
     */
    public Collection<WorkspaceFolder> getOldFolders()
    {
        return oldFolders;
    }

    /**
     * Returns the new folders.
     *
     * @return the new folders (may be <code>null</code> or empty).
     *  Clients <b>must not</b> modify the returned collection.
     */
    public Collection<WorkspaceFolder> getNewFolders()
    {
        return newFolders;
    }
}
