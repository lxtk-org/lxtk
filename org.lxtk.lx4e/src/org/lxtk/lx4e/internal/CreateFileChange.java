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
package org.lxtk.lx4e.internal;

import java.io.ByteArrayInputStream;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Objects;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.NullChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.resource.DeleteResourceChange;

/**
 * Creates a given file and all folders leading up to the given file that do not exist already.
 */
public class CreateFileChange
    extends Change
{
    private final IFile file;
    private final CreateResourceOptions options;
    private final long[] fileAndFolderStamps;

    /**
     * Constructor.
     *
     * @param file not <code>null</code>
     * @param options not <code>null</code>
     */
    public CreateFileChange(IFile file, CreateResourceOptions options)
    {
        this(file, options, null);
    }

    /**
     * Constructor.
     *
     * @param file not <code>null</code>
     * @param options not <code>null</code>
     * @param fileAndFolderStamps the stamps to restore on the file path,
     *  or <code>null</code> if none
     */
    protected CreateFileChange(IFile file, CreateResourceOptions options,
        long[] fileAndFolderStamps)
    {
        this.file = Objects.requireNonNull(file);
        this.options = Objects.requireNonNull(options);
        this.fileAndFolderStamps = fileAndFolderStamps;
    }

    @Override
    public String getName()
    {
        return MessageFormat.format(Messages.CreateFileChange_name,
            file.getFullPath().makeRelative());
    }

    @Override
    public void initializeValidationData(IProgressMonitor pm)
    {
    }

    @Override
    public RefactoringStatus isValid(IProgressMonitor pm)
        throws CoreException, OperationCanceledException
    {
        if (!file.getProject().isAccessible())
            return RefactoringStatus.createFatalErrorStatus(MessageFormat.format(
                Messages.CreateFileChange_Project_is_not_accessible, file.getFullPath()));

        IResource resourceAtDestination = file.getParent().findMember(file.getName());
        if (resourceAtDestination != null && (!file.equals(resourceAtDestination)
            || (!options.isOverwrite() && !options.isIgnoreIfExists())))
        {
            return RefactoringStatus.createFatalErrorStatus(MessageFormat.format(
                Messages.CreateFileChange_Resource_already_exists, file.getFullPath()));
        }

        return new RefactoringStatus();
    }

    @Override
    public Change perform(IProgressMonitor pm) throws CoreException
    {
        if (file.exists() && !options.isOverwrite() && options.isIgnoreIfExists())
            return new NullChange()
            {
                @Override
                public Change perform(IProgressMonitor pm) throws CoreException
                {
                    return new CreateFileChange(file, options, fileAndFolderStamps);
                }
            };

        SubMonitor subMonitor = SubMonitor.convert(pm, 2);

        Change undoBeforeChange = performBeforeChange(subMonitor.split(1));

        file.create(new ByteArrayInputStream(new byte[0]), false, subMonitor.split(1));

        if (fileAndFolderStamps != null)
            file.revertModificationStamp(fileAndFolderStamps[0]);

        return new UndoChange(file, options,
            fileAndFolderStamps != null ? fileAndFolderStamps : computeFileAndFolderStamps(),
            undoBeforeChange);
    }

    @Override
    public Object getModifiedElement()
    {
        return file;
    }

    private Change performBeforeChange(IProgressMonitor monitor) throws CoreException
    {
        Change beforeChange = createBeforeChange();
        if (beforeChange == null)
            return null;

        try
        {
            SubMonitor subMonitor = SubMonitor.convert(monitor, 3);
            beforeChange.initializeValidationData(subMonitor.split(1));
            RefactoringStatus status = beforeChange.isValid(subMonitor.split(1));
            if (status.hasFatalError())
                return null;
            return beforeChange.perform(subMonitor.split(1));
        }
        finally
        {
            beforeChange.dispose();
        }
    }

    private Change createBeforeChange()
    {
        if (!file.exists())
        {
            IContainer parent = file.getParent();
            if (parent instanceof IFolder && !parent.exists())
            {
                return new CreateFolderChange((IFolder)parent, fileAndFolderStamps == null ? null
                    : Arrays.copyOfRange(fileAndFolderStamps, 1, fileAndFolderStamps.length));
            }
        }
        else if (options.isOverwrite())
        {
            return new DeleteResourceChange(file.getFullPath(), false);
        }
        return null;
    }

    private long[] computeFileAndFolderStamps()
    {
        long[] stamps = new long[file.getFullPath().segmentCount() - 1];
        int i = 0;
        for (IResource current = file; current.getType() != IResource.PROJECT;
            current = current.getParent())
        {
            stamps[i++] = current.getModificationStamp();
        }
        return stamps;
    }

    private static class UndoChange
        extends Change
    {
        private final IFile file;
        private final CreateResourceOptions options;
        private final long[] fileAndFolderStamps;
        private final Change delegate;
        private final Change afterChange;

        UndoChange(IFile file, CreateResourceOptions options, long[] fileAndFolderStamps,
            Change afterChange)
        {
            this.file = Objects.requireNonNull(file);
            this.options = Objects.requireNonNull(options);
            this.fileAndFolderStamps = Objects.requireNonNull(fileAndFolderStamps);
            this.delegate = new DeleteResourceChange(file.getFullPath(), false);
            this.afterChange = afterChange;
        }

        @Override
        public String getName()
        {
            return MessageFormat.format(Messages.CreateFileChange_Undo_name,
                file.getFullPath().makeRelative());
        }

        @Override
        public void dispose()
        {
            delegate.dispose();

            if (afterChange != null)
                afterChange.dispose();
        }

        @Override
        public void initializeValidationData(IProgressMonitor pm)
        {
            SubMonitor subMonitor = SubMonitor.convert(pm, 2);

            delegate.initializeValidationData(subMonitor.split(1));

            if (afterChange != null)
                afterChange.initializeValidationData(subMonitor.split(1));
        }

        @Override
        public RefactoringStatus isValid(IProgressMonitor pm)
            throws CoreException, OperationCanceledException
        {
            if (!file.exists())
                return new RefactoringStatus();

            return delegate.isValid(pm);
        }

        @Override
        public Change perform(IProgressMonitor pm) throws CoreException
        {
            SubMonitor subMonitor = SubMonitor.convert(pm, 3);

            if (file.exists())
                delegate.perform(subMonitor.split(1));

            if (afterChange != null)
            {
                RefactoringStatus status = afterChange.isValid(subMonitor.split(1));
                if (!status.hasFatalError())
                    afterChange.perform(subMonitor.split(1));
            }

            return new CreateFileChange(file, options, fileAndFolderStamps);
        }

        @Override
        public Object getModifiedElement()
        {
            return file;
        }
    }
}
