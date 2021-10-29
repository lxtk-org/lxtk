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
package org.lxtk.lx4e.refactoring;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.DeleteArguments;
import org.eclipse.ltk.core.refactoring.participants.DeleteParticipant;
import org.eclipse.ltk.core.refactoring.participants.ISharableParticipant;
import org.eclipse.ltk.core.refactoring.participants.RefactoringArguments;
import org.lxtk.FileDelete;
import org.lxtk.lx4e.util.ResourceUtil;

/**
 * Participates in refactorings that delete elements adaptable to resources.
 * Delegates change creation to an instance of {@link IFileOperationParticipantSupport}.
 */
public abstract class DeleteResourceParticipant
    extends DeleteParticipant
    implements ISharableParticipant
{
    private final Map<IResource, DeleteArguments> resources = new HashMap<>();

    @Override
    protected boolean initialize(Object element)
    {
        IResource resource = ResourceUtil.getResource(element);
        if (resource == null)
            return false;
        resources.put(resource, getArguments());
        return true;
    }

    @Override
    public void addElement(Object element, RefactoringArguments arguments)
    {
        IResource resource = ResourceUtil.getResource(element);
        if (resource != null)
            resources.put(resource, ((DeleteArguments)arguments));
    }

    @Override
    public RefactoringStatus checkConditions(IProgressMonitor pm, CheckConditionsContext context)
        throws OperationCanceledException
    {
        return new RefactoringStatus();
    }

    @Override
    public Change createPreChange(IProgressMonitor pm)
        throws CoreException, OperationCanceledException
    {
        return getFileOperationParticipantSupport().computePreDeleteChange(getFileDeletes(), pm);
    }

    @Override
    public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException
    {
        return getFileOperationParticipantSupport().computePostDeleteChange(getFileDeletes(), pm);
    }

    /**
     * Returns the file operation participant support.
     *
     * @return the file operation participant support (not <code>null</code>)
     */
    protected abstract IFileOperationParticipantSupport getFileOperationParticipantSupport();

    private List<FileDelete> getFileDeletes()
    {
        List<FileDelete> result = new ArrayList<>();
        for (Map.Entry<IResource, DeleteArguments> entry : resources.entrySet())
        {
            if (entry.getKey().getType() == IResource.PROJECT
                && !entry.getValue().getDeleteProjectContents())
                continue;
            URI uri = entry.getKey().getLocationURI();
            if (uri == null)
                continue;
            result.add(new FileDelete(uri));
        }
        return result;
    }
}
