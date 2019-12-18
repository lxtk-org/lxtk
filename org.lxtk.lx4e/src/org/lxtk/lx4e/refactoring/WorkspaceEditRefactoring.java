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
package org.lxtk.lx4e.refactoring;

import java.util.Objects;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

/**
 * TODO JavaDoc
 */
public class WorkspaceEditRefactoring
    extends Refactoring
{
    private final String name;
    private final WorkspaceEditChangeFactory changeFactory;
    private WorkspaceEdit workspaceEdit;
    private Change change;

    /**
     * TODO JavaDoc
     *
     * @param name not <code>null</code>
     * @param changeFactory not <code>null</code>
     */
    public WorkspaceEditRefactoring(String name,
        WorkspaceEditChangeFactory changeFactory)
    {
        this.name = Objects.requireNonNull(name);
        this.changeFactory = Objects.requireNonNull(changeFactory);
    }

    /**
     * TODO JavaDoc
     *
     * @param workspaceEdit not <code>null</code>
     */
    public void setWorkspaceEdit(WorkspaceEdit workspaceEdit)
    {
        this.workspaceEdit = Objects.requireNonNull(workspaceEdit);
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public RefactoringStatus checkInitialConditions(IProgressMonitor pm)
        throws CoreException, OperationCanceledException
    {
        return new RefactoringStatus();
    }

    @Override
    public RefactoringStatus checkFinalConditions(IProgressMonitor pm)
        throws CoreException, OperationCanceledException
    {
        RefactoringStatus status = new RefactoringStatus();
        change = changeFactory.createChange(name, workspaceEdit, status, pm);
        return status;
    }

    @Override
    public Change createChange(IProgressMonitor pm) throws CoreException,
        OperationCanceledException
    {
        return change;
    }
}
