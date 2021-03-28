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
package org.lxtk.lx4e.internal.util;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

/**
 * Provides static utility methods for manipulating refactoring changes.
 */
public class ChangeUtil
{
    /**
     * Uses {@link SafeRunner} to safely dispose the given change.
     *
     * @param change may be <code>null</code>
     */
    public static void safelyDisposeChange(Change change)
    {
        if (change == null)
            return;

        SafeRunner.run(() -> change.dispose());
    }

    /**
     * Calls {@link #safelyDisposeChange(Change)} for each of the given changes.
     *
     * @param changes may be <code>null</code>
     */
    public static void safelyDisposeChanges(Iterable<Change> changes)
    {
        if (changes == null)
            return;

        for (Change change : changes)
        {
            safelyDisposeChange(change);
        }
    }

    /**
     * Executes the given change. Returns the undo change, if any.
     *
     * @param change not <code>null</code>
     * @param initialize indicates whether to call {@link
     *  Change#initializeValidationData(IProgressMonitor)} on the change
     * @param initializeUndo indicates whether to call {@link
     *  Change#initializeValidationData(IProgressMonitor)} on the undo change
     * @param monitor may be <code>null</code>
     * @return the undo change, or <code>null</code> if none
     * @throws OperationCanceledException
     * @throws CoreException
     */
    public static Change executeChange(Change change, boolean initialize, boolean initializeUndo,
        IProgressMonitor monitor) throws OperationCanceledException, CoreException
    {
        int work = 5;
        if (initialize)
            work++;
        if (initializeUndo)
            work++;

        SubMonitor subMonitor = SubMonitor.convert(monitor, work);

        if (initialize)
            change.initializeValidationData(subMonitor.split(1));

        RefactoringStatus status = change.isValid(subMonitor.split(2));
        if (status.hasFatalError())
            throw new CoreException(status.getEntryWithHighestSeverity().toStatus());

        Change undoChange = null;
        try
        {
            try
            {
                undoChange = change.perform(subMonitor.split(3));
            }
            finally
            {
                // It is important to send an interim notification of resource change events here.
                // Otherwise, there might be issues e.g. buffers not synchronized with file system,
                // when subsequent changes get created/performed within the same workspace operation.
                ResourcesPlugin.getWorkspace().checkpoint(false);
            }

            if (undoChange == null)
                return null;

            if (initializeUndo)
                undoChange.initializeValidationData(subMonitor.split(1));

            Change result = undoChange;
            undoChange = null; // transfer ownership
            return result;
        }
        finally
        {
            safelyDisposeChange(undoChange);
        }
    }

    private ChangeUtil()
    {
    }
}
