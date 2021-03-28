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
import java.util.Objects;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.NullChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.resource.ResourceChange;

/**
 * Deletes a resource.
 */
public class DeleteResourceChange
    extends Change
{
    private final IPath resourcePath;
    private final DeleteResourceOptions options;
    private final Change delegate;

    /**
     * Constructor.
     *
     * @param resourcePath not <code>null</code>
     * @param options not <code>null</code>
     */
    public DeleteResourceChange(IPath resourcePath, DeleteResourceOptions options)
    {
        this(resourcePath, options, ResourceChange.SAVE_IF_DIRTY);
    }

    /**
     * Constructor.
     *
     * @param resourcePath not <code>null</code>
     * @param options not <code>null</code>
     * @param validationMethod
     */
    public DeleteResourceChange(IPath resourcePath, DeleteResourceOptions options,
        int validationMethod)
    {
        this.resourcePath = Objects.requireNonNull(resourcePath);
        this.options = Objects.requireNonNull(options);
        org.eclipse.ltk.core.refactoring.resource.DeleteResourceChange delegate =
            new org.eclipse.ltk.core.refactoring.resource.DeleteResourceChange(resourcePath, false);
        delegate.setValidationMethod(validationMethod);
        this.delegate = delegate;
    }

    @Override
    public String getName()
    {
        return delegate.getName();
    }

    @Override
    public void dispose()
    {
        delegate.dispose();
    }

    @Override
    public void initializeValidationData(IProgressMonitor pm)
    {
        delegate.initializeValidationData(pm);
    }

    @Override
    public RefactoringStatus isValid(IProgressMonitor pm)
        throws CoreException, OperationCanceledException
    {
        IResource resource = getResource();

        if (resource == null)
        {
            if (options.isIgnoreIfNotExists())
                return new RefactoringStatus();

            return RefactoringStatus.createFatalErrorStatus(MessageFormat.format(
                Messages.DeleteResourceChange_Resource_does_not_exist, resourcePath));
        }

        if (!options.isRecursive() && resource instanceof IContainer && resource.isAccessible())
        {
            if (((IContainer)resource).members().length > 0)
                return RefactoringStatus.createFatalErrorStatus(MessageFormat.format(
                    Messages.DeleteResourceChange_Directory_is_not_empty, resourcePath));
        }

        return delegate.isValid(pm);
    }

    @Override
    public Change perform(IProgressMonitor pm) throws CoreException
    {
        IResource resource = getResource();

        if (resource == null && options.isIgnoreIfNotExists())
            return new UndoChange(resourcePath, options, new NullChange());

        Change undo = delegate.perform(pm);
        if (undo == null)
            return null;

        return new UndoChange(resourcePath, options, undo);
    }

    @Override
    public Object getModifiedElement()
    {
        return delegate.getModifiedElement();
    }

    private IResource getResource()
    {
        return ResourcesPlugin.getWorkspace().getRoot().findMember(resourcePath);
    }

    private static class UndoChange
        extends Change
    {
        private final IPath resourcePath;
        private final DeleteResourceOptions options;
        private final Change delegate;

        UndoChange(IPath resourcePath, DeleteResourceOptions options, Change delegate)
        {
            this.resourcePath = Objects.requireNonNull(resourcePath);
            this.options = Objects.requireNonNull(options);
            this.delegate = Objects.requireNonNull(delegate);
        }

        @Override
        public String getName()
        {
            return delegate.getName();
        }

        @Override
        public void dispose()
        {
            delegate.dispose();
        }

        @Override
        public void initializeValidationData(IProgressMonitor pm)
        {
            delegate.initializeValidationData(pm);
        }

        @Override
        public RefactoringStatus isValid(IProgressMonitor pm)
            throws CoreException, OperationCanceledException
        {
            return delegate.isValid(pm);
        }

        @Override
        public Change perform(IProgressMonitor pm) throws CoreException
        {
            delegate.perform(pm);
            return new DeleteResourceChange(resourcePath, options,
                ResourceChange.VALIDATE_NOT_READ_ONLY | ResourceChange.VALIDATE_NOT_DIRTY);
        }

        @Override
        public Object getModifiedElement()
        {
            return delegate.getModifiedElement();
        }
    }
}
