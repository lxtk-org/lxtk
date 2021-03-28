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
import java.util.Arrays;
import java.util.Objects;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.NullChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.resource.DeleteResourceChange;
import org.eclipse.ltk.core.refactoring.resource.ResourceChange;

/**
 * Moves a resource. Creates all folders leading up to the destination that do not exist already.
 */
public class MoveResourceChange
    extends ResourceChange
{
    private final IResource source;
    private final IPath destination;
    private final MoveResourceOptions options;
    private final long[] destinationStamps;
    private final Change afterChange;

    /**
     * Constructor.
     *
     * @param source not <code>null</code>
     * @param destination not <code>null</code>
     * @param options not <code>null</code>
     */
    public MoveResourceChange(IResource source, IPath destination, MoveResourceOptions options)
    {
        this(source, destination, options, null, null);
    }

    /**
     * Constructor.
     *
     * @param source not <code>null</code>
     * @param destination not <code>null</code>
     * @param options not <code>null</code>
     * @param destinationStamps the stamps to restore on the destination path,
     *  or <code>null</code> if none
     * @param afterChange the change to perform after the resource has been moved,
     *  or <code>null</code> if none
     */
    protected MoveResourceChange(IResource source, IPath destination, MoveResourceOptions options,
        long[] destinationStamps, Change afterChange)
    {
        this.source = Objects.requireNonNull(source);
        this.destination = Objects.requireNonNull(destination);
        this.options = Objects.requireNonNull(options);
        this.destinationStamps = destinationStamps;
        this.afterChange = afterChange;
        setValidationMethod(VALIDATE_NOT_DIRTY);
    }

    @Override
    public String getName()
    {
        return MessageFormat.format(Messages.MoveResourceChange_name,
            source.getFullPath().makeRelative(), destination.makeRelative());
    }

    @Override
    public void dispose()
    {
        if (afterChange != null)
            afterChange.dispose();

        super.dispose();
    }

    @Override
    public void initializeValidationData(IProgressMonitor pm)
    {
        SubMonitor subMonitor = SubMonitor.convert(pm, 2);

        super.initializeValidationData(subMonitor.split(1));

        if (afterChange != null)
            afterChange.initializeValidationData(subMonitor.split(1));
    }

    @Override
    public RefactoringStatus isValid(IProgressMonitor pm)
        throws CoreException, OperationCanceledException
    {
        if (!source.exists())
            return RefactoringStatus.createFatalErrorStatus(MessageFormat.format(
                Messages.MoveResourceChange_Resource_does_not_exist, source.getFullPath()));

        IResource resourceAtDestination = getResourceAtDestination();
        if (resourceAtDestination != null && !options.isOverwrite() && !options.isIgnoreIfExists())
            return RefactoringStatus.createFatalErrorStatus(
                MessageFormat.format(Messages.MoveResourceChange_Resource_already_exists,
                    source.getFullPath(), destination));

        return super.isValid(pm);
    }

    @Override
    public Change perform(IProgressMonitor pm) throws CoreException
    {
        IResource resourceAtDestination = getResourceAtDestination();

        if (resourceAtDestination != null && !options.isOverwrite() && options.isIgnoreIfExists())
            return new NullChange()
            {
                @Override
                public Change perform(IProgressMonitor pm) throws CoreException
                {
                    return new MoveResourceChange(source, destination, options, destinationStamps,
                        afterChange);
                }
            };

        SubMonitor subMonitor = SubMonitor.convert(pm, 3);

        Change undoBeforeChange = performBeforeChange(resourceAtDestination, subMonitor.split(1));

        long[] sourceStamps = getSourceStamps();

        source.move(destination, IResource.KEEP_HISTORY | IResource.SHALLOW, subMonitor.split(1));

        resourceAtDestination = getResourceAtDestination();

        if (destinationStamps != null)
            resourceAtDestination.revertModificationStamp(destinationStamps[0]);

        performAfterChange(subMonitor.split(1));

        return createReverseMoveChange(resourceAtDestination, source.getFullPath(), sourceStamps,
            undoBeforeChange);
    }

    @Override
    protected IResource getModifiedResource()
    {
        return source;
    }

    Change createReverseMoveChange(IResource source, IPath destination, long[] destinationStamps,
        Change afterChange)
    {
        MoveResourceOptions originalOptions = options;
        return new MoveResourceChange(source, destination, new MoveResourceOptions(false, false),
            destinationStamps, afterChange)
        {
            @Override
            Change createReverseMoveChange(IResource source, IPath destination,
                long[] destinationStamps, Change afterChange)
            {
                return new MoveResourceChange(source, destination, originalOptions,
                    destinationStamps, afterChange);
            }
        };
    }

    private long[] getSourceStamps()
    {
        long[] stamps = new long[source.getFullPath().segmentCount() - 1];
        int i = 0;
        for (IResource current = source; current.getType() != IResource.PROJECT;
            current = current.getParent())
        {
            stamps[i++] = current.getModificationStamp();
        }
        return stamps;
    }

    private IResource getResourceAtDestination()
    {
        return ResourcesPlugin.getWorkspace().getRoot().findMember(destination);
    }

    private Change performBeforeChange(IResource resourceAtDestination, IProgressMonitor monitor)
        throws CoreException
    {
        Change beforeChange = createBeforeChange(resourceAtDestination);
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

    private Change createBeforeChange(IResource resourceAtDestination)
    {
        if (resourceAtDestination == null) // no resource exists at the destination
        {
            IPath destinationParent = destination.removeLastSegments(1);
            if (destinationParent.segmentCount() > 1)
            {
                IFolder folder =
                    ResourcesPlugin.getWorkspace().getRoot().getFolder(destinationParent);
                if (!folder.exists())
                    return new CreateFolderChange(folder, destinationStamps == null ? null
                        : Arrays.copyOfRange(destinationStamps, 1, destinationStamps.length));
            }
        }
        else if (options.isOverwrite())
        {
            return new DeleteResourceChange(destination, false);
        }
        return null;
    }

    private void performAfterChange(IProgressMonitor monitor) throws CoreException
    {
        if (afterChange == null)
            return;

        SubMonitor subMonitor = SubMonitor.convert(monitor, 2);
        RefactoringStatus status = afterChange.isValid(subMonitor.split(1));
        if (!status.hasFatalError())
            afterChange.perform(subMonitor.split(1));
    }
}
