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
package org.lxtk.lx4e;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.lxtk.WorkspaceFolder;
import org.lxtk.WorkspaceService;
import org.lxtk.lx4e.internal.Activator;
import org.lxtk.util.SafeRun;

/**
 * Manages a mapping from the Eclipse workspace to the collection of workspace folders
 * of a given {@link WorkspaceService}.
 */
public class WorkspaceFoldersManager
{
    private final WorkspaceService workspaceService;
    private Runnable shutdownRunnable;

    /**
     * Constructor.
     *
     * @param workspaceService not <code>null</code>
     */
    public WorkspaceFoldersManager(WorkspaceService workspaceService)
    {
        this.workspaceService = Objects.requireNonNull(workspaceService);
    }

    /**
     * Starts this manager up.
     */
    public void startup()
    {
        SafeRun.run(rollback ->
        {
            rollback.add(() -> workspaceService.setWorkspaceFolders(null));

            IResourceChangeListener listener = event ->
            {
                if (affectsWorkspaceFolders(event))
                    workspaceService.setWorkspaceFolders(computeWorkspaceFolders());
            };
            IWorkspace workspace = ResourcesPlugin.getWorkspace();
            workspace.addResourceChangeListener(listener, IResourceChangeEvent.POST_CHANGE);
            rollback.add(() -> workspace.removeResourceChangeListener(listener));

            workspaceService.setWorkspaceFolders(computeWorkspaceFolders());

            rollback.setLogger(e -> Activator.logError(e));
            shutdownRunnable = rollback;
        });
    }

    /**
     * Shutdowns this manager.
     */
    public void shutdown()
    {
        if (shutdownRunnable != null)
        {
            shutdownRunnable.run();
            shutdownRunnable = null;
        }
    }

    /**
     * Computes the current workspace folders.
     *
     * @return the workspace folders (may be <code>null</code> or empty)
     */
    protected Collection<WorkspaceFolder> computeWorkspaceFolders()
    {
        Collection<WorkspaceFolder> result = new ArrayList<>();
        IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
        for (IProject project : projects)
        {
            URI uri = project.getLocationURI();
            if (uri != null && project.isOpen())
                result.add(new WorkspaceFolder(uri, project.getName()));
        }
        return result;
    }

    /**
     * Checks whether the given resource change event might affect the workspace folders.
     *
     * @param event never <code>null</code>
     * @return <code>true</code> if the given event might affect the workspace folders,
     *  and <code>false</code> otherwise
     */
    protected boolean affectsWorkspaceFolders(IResourceChangeEvent event)
    {
        IResourceDelta[] children = event.getDelta().getAffectedChildren();
        for (IResourceDelta child : children)
        {
            int kind = child.getKind();
            if (kind == IResourceDelta.ADDED || kind == IResourceDelta.REMOVED
                || (child.getFlags() & (IResourceDelta.OPEN | IResourceDelta.REPLACED
                    | IResourceDelta.DESCRIPTION)) != 0)
                return true;
        }
        return false;
    }
}
