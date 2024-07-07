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

import static java.util.Collections.unmodifiableList;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

import org.lxtk.util.EventEmitter;
import org.lxtk.util.EventStream;
import org.lxtk.util.UriUtil;

/**
 * Default implementation of the {@link WorkspaceService} interface.
 * <p>
 * This implementation is thread-safe.
 * </p>
 */
public class DefaultWorkspaceService
    implements WorkspaceService
{
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private Collection<WorkspaceFolder> folders;
    private TreeMap<String, WorkspaceFolder> sortedFolders;
    private final EventEmitter<WorkspaceFoldersChangeEvent> onDidChangeWorkspaceFolders =
        new EventEmitter<>();

    @Override
    public void setWorkspaceFolders(Collection<WorkspaceFolder> newFolders)
    {
        if (!lock.isWriteLockedByCurrentThread() && lock.getReadHoldCount() > 0)
            throw new IllegalStateException(
                "Workspace folder changes are disallowed during change event notification"); //$NON-NLS-1$

        TreeMap<String, WorkspaceFolder> newSortedFolders = null;
        if (newFolders != null && !newFolders.isEmpty())
        {
            newSortedFolders =
                new TreeMap<>(Comparator.comparingInt((String s) -> s.length()).thenComparing(
                    String::compareTo).reversed());
            dryRun(newFolders, newSortedFolders);
        }

        Collection<WorkspaceFolder> oldFolders;
        lock.writeLock().lock();
        try
        {
            oldFolders = folders;
            folders = newFolders != null ? unmodifiableList(new ArrayList<>(newFolders)) : null;
            sortedFolders = newSortedFolders;

            lock.readLock().lock();
        }
        finally
        {
            lock.writeLock().unlock();
        }
        try
        {
            onDidChangeWorkspaceFolders.emit(new WorkspaceFoldersChangeEvent(oldFolders, folders),
                getLogger());
        }
        finally
        {
            lock.readLock().unlock();
        }
    }

    private static void dryRun(Collection<WorkspaceFolder> folders,
        Map<String, WorkspaceFolder> foldersMap)
    {
        for (WorkspaceFolder folder : folders)
        {
            String key = getKey(folder.getUri());
            WorkspaceFolder other = foldersMap.get(key);
            if (other != null)
            {
                throw new IllegalArgumentException(
                    "Two or more workspace folders have the same URI: " + folder + " and " + other); //$NON-NLS-1$ //$NON-NLS-2$
            }
            foldersMap.put(key, folder);
        }
    }

    @Override
    public Collection<WorkspaceFolder> getWorkspaceFolders()
    {
        lock.readLock().lock();
        try
        {
            return folders;
        }
        finally
        {
            lock.readLock().unlock();
        }
    }

    @Override
    public WorkspaceFolder getWorkspaceFolder(URI uri)
    {
        lock.readLock().lock();
        try
        {
            if (sortedFolders == null)
                return null;
            return getWorkspaceFolder(uri, sortedFolders);
        }
        finally
        {
            lock.readLock().unlock();
        }
    }

    @Override
    public WorkspaceFolder getOutermostWorkspaceFolder(URI uri)
    {
        lock.readLock().lock();
        try
        {
            if (sortedFolders == null)
                return null;
            return getWorkspaceFolder(uri, sortedFolders.descendingMap());
        }
        finally
        {
            lock.readLock().unlock();
        }
    }

    private static WorkspaceFolder getWorkspaceFolder(URI uri,
        SortedMap<String, WorkspaceFolder> sortedFolders)
    {
        String key = getKey(uri);
        for (Entry<String, WorkspaceFolder> entry : sortedFolders.entrySet())
        {
            if (key.startsWith(entry.getKey()))
                return entry.getValue();
        }
        return null;
    }

    @Override
    public EventStream<WorkspaceFoldersChangeEvent> onDidChangeWorkspaceFolders()
    {
        return onDidChangeWorkspaceFolders;
    }

    private static String getKey(URI uri)
    {
        String key = UriUtil.normalize(uri).toString();
        if (key.charAt(key.length() - 1) != '/')
            key += '/';
        return key;
    }

    /**
     * Returns an exception logger for this service.
     *
     * @return a logger instance (may be <code>null</code>)
     */
    protected Consumer<Throwable> getLogger()
    {
        return null;
    }
}
