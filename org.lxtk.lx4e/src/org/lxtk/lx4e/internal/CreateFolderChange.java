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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

/**
 * Creates a given folder and all folders leading up to the given folder that do not exist already.
 */
public class CreateFolderChange
    extends Change
{
    private final IFolder folder;
    private final long[] folderStamps;

    /**
     * Constructor.
     *
     * @param folder not <code>null</code>
     */
    public CreateFolderChange(IFolder folder)
    {
        this(folder, null);
    }

    /**
     * Constructor.
     *
     * @param folder not <code>null</code>
     * @param folderStamps the stamps to restore on the folder path,
     *  or <code>null</code> if none
     */
    protected CreateFolderChange(IFolder folder, long[] folderStamps)
    {
        this.folder = Objects.requireNonNull(folder);
        this.folderStamps = folderStamps;
    }

    @Override
    public String getName()
    {
        return MessageFormat.format(Messages.CreateFolderChange_name,
            folder.getFullPath().makeRelative());
    }

    @Override
    public void initializeValidationData(IProgressMonitor pm)
    {
    }

    @Override
    public RefactoringStatus isValid(IProgressMonitor pm)
        throws CoreException, OperationCanceledException
    {
        if (!folder.getProject().isAccessible())
            return RefactoringStatus.createFatalErrorStatus(MessageFormat.format(
                Messages.CreateFolderChange_Project_is_not_accesible, folder.getFullPath()));

        return new RefactoringStatus();
    }

    @Override
    public Change perform(IProgressMonitor pm) throws CoreException
    {
        List<IFolder> folders = getFoldersToCreate();

        SubMonitor subMonitor = SubMonitor.convert(pm, folders.size());

        for (IFolder folder : folders)
        {
            folder.create(false, true, subMonitor.split(1));
        }

        Collections.reverse(folders);

        if (folderStamps != null)
        {
            int i = 0;
            for (IFolder folder : folders)
            {
                folder.revertModificationStamp(folderStamps[i++]);
            }
        }

        return new UndoChange(folder, folderStamps != null ? folderStamps : computeFolderStamps(),
            folders);
    }

    @Override
    public Object getModifiedElement()
    {
        return folder;
    }

    private List<IFolder> getFoldersToCreate()
    {
        if (folder.exists())
            return Collections.emptyList();

        List<IFolder> result = new ArrayList<>();
        result.add(folder);
        for (IContainer parent = folder.getParent(); parent instanceof IFolder && !parent.exists();
            parent = parent.getParent())
        {
            result.add((IFolder)parent);
        }
        Collections.reverse(result);
        return result;
    }

    private long[] computeFolderStamps()
    {
        long[] stamps = new long[folder.getFullPath().segmentCount() - 1];
        int i = 0;
        for (IResource current = folder; current instanceof IFolder; current = current.getParent())
        {
            stamps[i++] = current.getModificationStamp();
        }
        return stamps;
    }

    private static class UndoChange
        extends Change
    {
        private final IFolder folder;
        private final long[] folderStamps;
        private final List<IFolder> createdFolders;

        UndoChange(IFolder folder, long[] folderStamps, List<IFolder> createdFolders)
        {
            this.folder = Objects.requireNonNull(folder);
            this.folderStamps = Objects.requireNonNull(folderStamps);
            this.createdFolders = Objects.requireNonNull(createdFolders);
        }

        @Override
        public String getName()
        {
            return MessageFormat.format(Messages.CreateFolderChange_Undo_name,
                folder.getFullPath().makeRelative());
        }

        @Override
        public void initializeValidationData(IProgressMonitor pm)
        {
        }

        @Override
        public RefactoringStatus isValid(IProgressMonitor pm)
            throws CoreException, OperationCanceledException
        {
            return new RefactoringStatus();
        }

        @Override
        public Change perform(IProgressMonitor pm) throws CoreException
        {
            IFolder folderToDelete = getFolderToDelete();
            if (folderToDelete != null)
            {
                folderToDelete.delete(false, pm);
            }

            return new CreateFolderChange(folder, folderStamps);
        }

        @Override
        public Object getModifiedElement()
        {
            return folder;
        }

        private IFolder getFolderToDelete() throws CoreException
        {
            IFolder result = null;
            for (IFolder folder : createdFolders)
            {
                if (!folder.exists())
                    continue;
                if (folder.members().length > (result == null ? 0 : 1))
                    break;
                result = folder;
            }
            return result;
        }
    }
}
