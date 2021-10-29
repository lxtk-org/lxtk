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

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.ISharableParticipant;
import org.eclipse.ltk.core.refactoring.participants.RefactoringArguments;
import org.eclipse.ltk.core.refactoring.participants.RenameArguments;
import org.eclipse.ltk.core.refactoring.participants.RenameParticipant;
import org.lxtk.FileRename;
import org.lxtk.lx4e.util.ResourceUtil;

/**
 * Participates in refactorings that rename elements adaptable to resources.
 * Delegates change creation to an instance of {@link IFileOperationParticipantSupport}.
 */
public abstract class RenameResourceParticipant
    extends RenameParticipant
    implements ISharableParticipant
{
    private final Map<IResource, RenameArguments> resources = new HashMap<>();

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
            resources.put(resource, ((RenameArguments)arguments));
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
        return getFileOperationParticipantSupport().computePreRenameChange(getFileRenames(), pm);
    }

    @Override
    public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException
    {
        return getFileOperationParticipantSupport().computePostRenameChange(getFileRenames(), pm);
    }

    /**
     * Returns the file operation participant support.
     *
     * @return the file operation participant support (not <code>null</code>)
     */
    protected abstract IFileOperationParticipantSupport getFileOperationParticipantSupport();

    private List<FileRename> getFileRenames()
    {
        List<FileRename> result = new ArrayList<>();
        for (Map.Entry<IResource, RenameArguments> entry : resources.entrySet())
        {
            URI oldUri = entry.getKey().getLocationURI();
            if (oldUri == null)
                continue;
            IContainer parent = entry.getKey().getParent();
            if (parent == null)
                continue;
            URI parentUri = parent.getLocationURI();
            if (parentUri == null)
                continue;
            URI newUri = URIUtil.append(parentUri, entry.getValue().getNewName());
            result.add(new FileRename(oldUri, newUri));
        }
        return result;
    }
}
