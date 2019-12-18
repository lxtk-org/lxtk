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

import static org.lxtk.lx4e.UriHandlers.exists;
import static org.lxtk.lx4e.UriHandlers.getBuffer;
import static org.lxtk.lx4e.UriHandlers.toDisplayString;

import java.net.URI;
import java.text.MessageFormat;
import java.util.Objects;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.handly.buffer.IBuffer;
import org.eclipse.handly.buffer.IBufferChange;
import org.eclipse.handly.snapshot.StaleSnapshotException;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.lxtk.lx4e.IUriHandler;
import org.lxtk.lx4e.internal.Activator;

class UndoTextFileChange
    extends Change
{
    private final String name;
    private final URI uri;
    private final IUriHandler uriHandler;
    private final IBufferChange undoChange;
    private boolean existed;

    UndoTextFileChange(String name, URI uri, IUriHandler uriHandler,
        IBufferChange undoChange)
    {
        this.name = Objects.requireNonNull(name);
        this.uri = Objects.requireNonNull(uri);
        this.uriHandler = Objects.requireNonNull(uriHandler);
        this.undoChange = Objects.requireNonNull(undoChange);
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public void initializeValidationData(IProgressMonitor pm)
    {
        existed = exists(uri, uriHandler);
    }

    @Override
    public RefactoringStatus isValid(IProgressMonitor pm) throws CoreException,
        OperationCanceledException
    {
        RefactoringStatus result = new RefactoringStatus();

        if (!existed) // got deleted/moved during refactoring
        {
            if (exists(uri, uriHandler))
            {
                result.addFatalError(MessageFormat.format(
                    Messages.UndoTextFileChange_File_should_not_exist, toDisplayString(uri,
                        uriHandler)));
            }
            return result; // let the delete/move undo change handle the rest
        }
        else if (!exists(uri, uriHandler))
        {
            result.addFatalError(MessageFormat.format(
                Messages.UndoTextFileChange_File_should_exist, toDisplayString(uri, uriHandler)));
            return result;
        }

        if (undoChange.getBase() == null)
            return result; // OK

        try (IBuffer buffer = getBuffer(uri, uriHandler))
        {
            if (!undoChange.getBase().isEqualTo(buffer.getSnapshot()))
            {
                result.addFatalError(MessageFormat.format(
                    Messages.UndoTextFileChange_Cannot_undo_stale_change, toDisplayString(uri,
                        uriHandler)));
            }
        }
        return result;
    }

    @Override
    public Change perform(IProgressMonitor pm) throws CoreException
    {
        SubMonitor subMonitor = SubMonitor.convert(pm, 1);
        try (IBuffer buffer = getBuffer(uri, uriHandler))
        {
            IBufferChange redoChange;

            try
            {
                redoChange = buffer.applyChange(undoChange, subMonitor.split(1,
                    SubMonitor.SUPPRESS_ISCANCELED
                        | SubMonitor.SUPPRESS_BEGINTASK));
            }
            catch (StaleSnapshotException e)
            {
                throw new CoreException(Activator.createErrorStatus(
                    MessageFormat.format(Messages.UndoTextFileChange_Cannot_undo_stale_change,
                        toDisplayString(uri, uriHandler)), e));
            }

            return new UndoTextFileChange(name, uri, uriHandler, redoChange);
        }
    }

    @Override
    public Object getModifiedElement()
    {
        return uriHandler.getCorrespondingElement(uri);
    }

    @Override
    public Object[] getAffectedObjects()
    {
        Object element = getModifiedElement();
        if (element != null)
            return new Object[] { element };
        return null;
    }
}
