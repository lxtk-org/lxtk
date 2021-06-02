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
package org.lxtk.lx4e.internal.ui;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CheckConditionsOperation;
import org.eclipse.ltk.core.refactoring.CreateChangeOperation;
import org.eclipse.ltk.core.refactoring.PerformChangeOperation;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.WorkspaceModifyOperation;

public class RefactoringExecutor
{
    /**
     * Executes the given refactoring. This method must be called in the UI thread.
     *
     * @param refactoring not <code>null</code>
     * @param parent may be <code>null</code>
     * @return a refactoring status (never <code>null</code>). If the status is {@link
     *  RefactoringStatus#FATAL} the refactoring has to be considered as not being executable
     * @throws InvocationTargetException if an error occurred during refactoring execution
     * @throws InterruptedException if refactoring execution is canceled
     */
    public static RefactoringStatus execute(Refactoring refactoring, Shell parent)
        throws InvocationTargetException, InterruptedException
    {
        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (window == null)
            throw new IllegalStateException();
        CreateChangeOperation createChangeOperation = new CreateChangeOperation(
            new CheckConditionsOperation(refactoring, CheckConditionsOperation.ALL_CONDITIONS),
            RefactoringCore.getConditionCheckingFailedSeverity());
        window.run(true, true, new WorkspaceModifyOperation()
        {
            @Override
            protected void execute(IProgressMonitor monitor)
                throws CoreException, InvocationTargetException, InterruptedException
            {
                createChangeOperation.run(monitor);
            }
        });
        RefactoringStatus status = createChangeOperation.getConditionCheckingStatus();
        if (status.hasFatalError())
            return status;
        Change change = createChangeOperation.getChange();
        if (change != null)
        {
            status.merge(performChange(new PerformChangeOperation(change), refactoring,
                new ProgressMonitorDialog(parent)));
        }
        else // condition checking failed
        {
            InvocationTargetException[] exception = new InvocationTargetException[1];
            RefactoringWizard refactoringWizard =
                new RefactoringWizard(refactoring, RefactoringWizard.DIALOG_BASED_USER_INTERFACE)
                {
                    @Override
                    protected void addUserInputPages()
                    {
                    }

                    @Override
                    public boolean performFinish()
                    {
                        Change change = getChange();
                        PerformChangeOperation op = change != null
                            ? new PerformChangeOperation(change)
                            : new PerformChangeOperation(new CreateChangeOperation(refactoring));
                        try
                        {
                            status.merge(performChange(op, refactoring, getContainer()));
                            return true;
                        }
                        catch (InvocationTargetException e)
                        {
                            exception[0] = e;
                            return true;
                        }
                        catch (InterruptedException e)
                        {
                            return false;
                        }
                    }
                };
            refactoringWizard.setDefaultPageTitle(refactoring.getName());
            int result = new RefactoringWizardOpenOperation(refactoringWizard).run(parent,
                refactoring.getName());
            if (result != IDialogConstants.OK_ID)
                throw new InterruptedException();
            if (exception[0] != null)
                throw exception[0];
        }
        return status;
    }

    private static RefactoringStatus performChange(PerformChangeOperation op,
        Refactoring refactoring, IRunnableContext context)
        throws InvocationTargetException, InterruptedException
    {
        op.setUndoManager(RefactoringCore.getUndoManager(), refactoring.getName());
        // Note that it is important to execute the refactoring's change in the UI thread.
        // Otherwise, there might be timing issues, e.g. incorrect modification stamps.
        // See also org.eclipse.jdt.internal.ui.refactoring.RefactoringExecutionHelper.
        // Note that WorkspaceModifyOperation is used to get EventLoopProgressMonitor.
        context.run(false, false, new WorkspaceModifyOperation()
        {
            @Override
            protected void execute(IProgressMonitor monitor)
                throws CoreException, InvocationTargetException, InterruptedException
            {
                op.run(monitor);
            }
        });
        return op.getValidationStatus();
    }

    private RefactoringExecutor()
    {
    }
}
