/*******************************************************************************
 * Copyright (c) 2020, 2021 1C-Soft LLC.
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
import java.util.Collection;

import org.lxtk.util.EventStream;

/**
 * Provides support for workspace management.
 *
 * @see DefaultWorkspaceService
 */
public interface WorkspaceService
{
    /**
     * Sets the collection of workspace folders to be managed by this service.
     *
     * @param folders may be <code>null</code> or empty
     */
    void setWorkspaceFolders(Collection<WorkspaceFolder> folders);

    /**
     * Returns all workspace folders currently managed by this service.
     * <p>
     * This method must not try to acquire any kind of lock that might conflict
     * with any locks held during didChangeWorkspaceFolders event notification.
     * </p>
     *
     * @return all workspace folders currently managed by the service
     *  (may be <code>null</code> or empty). Clients <b>must not</b>
     *  modify the returned collection
     */
    Collection<WorkspaceFolder> getWorkspaceFolders();

    /**
     * Returns the innermost workspace folder that is managed by this service and
     * contains the given URI.
     * <p>
     * This method must not try to acquire any kind of lock that might conflict
     * with any locks held during didChangeWorkspaceFolders event notification.
     * </p>
     *
     * @param uri uri may be <code>null</code>, in which case <code>null</code>
     *  is returned
     * @return the corresponding workspace folder, or <code>null</code> if none
     */
    WorkspaceFolder getWorkspaceFolder(URI uri);

    /**
     * Returns the outermost workspace folder that is managed by this service and
     * contains the given URI.
     * <p>
     * This method must not try to acquire any kind of lock that might conflict
     * with any locks held during didChangeWorkspaceFolders event notification.
     * </p>
     *
     * @param uri uri may be <code>null</code>, in which case <code>null</code>
     *  is returned
     * @return the corresponding workspace folder, or <code>null</code> if none
     */
    WorkspaceFolder getOutermostWorkspaceFolder(URI uri);

    /**
     * Returns a stream of events that are emitted when there is a change to the collection
     * of workspace folders managed by this service.
     *
     * @return a stream of events that are emitted when there is a change to the collection
     *  of workspace folders (never <code>null</code>)
     */
    EventStream<WorkspaceFoldersChangeEvent> onDidChangeWorkspaceFolders();
}
