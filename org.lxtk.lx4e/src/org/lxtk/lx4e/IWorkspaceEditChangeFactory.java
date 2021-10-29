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
package org.lxtk.lx4e;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.ltk.core.refactoring.Change;

/**
 * Creates a {@link Change} object for a given {@link WorkspaceEdit}.
 */
public interface IWorkspaceEditChangeFactory
{
    /**
     * Creates a {@link Change} object that performs the workspace transformation
     * described by the given {@link WorkspaceEdit}.
     *
     * @param name the human readable name of the change. Will
     *  be used to display the change in the user interface
     * @param workspaceEdit a {@link WorkspaceEdit} describing the workspace
     *  transformation that will be performed by the change (not <code>null</code>)
     * @param pm a progress monitor, or <code>null</code>
     *  if progress reporting is not desired. The caller must not rely on
     *  {@link IProgressMonitor#done()} having been called by the receiver
     * @return the created change (never <code>null</code>)
     * @throws CoreException if this method could not create a change
     * @throws OperationCanceledException if this method is canceled
     */
    Change createChange(String name, WorkspaceEdit workspaceEdit, IProgressMonitor pm)
        throws CoreException;
}
