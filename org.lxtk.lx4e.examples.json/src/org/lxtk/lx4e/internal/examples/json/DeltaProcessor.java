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
package org.lxtk.lx4e.internal.examples.json;

import static org.eclipse.handly.model.IElementDeltaConstants.ADDED;
import static org.eclipse.handly.model.IElementDeltaConstants.CHANGED;
import static org.eclipse.handly.model.IElementDeltaConstants.F_CONTENT;
import static org.eclipse.handly.model.IElementDeltaConstants.F_MARKERS;
import static org.eclipse.handly.model.IElementDeltaConstants.F_MOVED_FROM;
import static org.eclipse.handly.model.IElementDeltaConstants.F_MOVED_TO;
import static org.eclipse.handly.model.IElementDeltaConstants.F_SYNC;
import static org.eclipse.handly.model.IElementDeltaConstants.F_UNDERLYING_RESOURCE;
import static org.eclipse.handly.model.IElementDeltaConstants.REMOVED;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.handly.model.ElementDeltas;
import org.eclipse.handly.model.IElementDelta;
import org.eclipse.handly.model.impl.IElementImplExtension;
import org.eclipse.handly.model.impl.support.Body;
import org.lxtk.lx4e.examples.json.JsonCore;
import org.lxtk.lx4e.model.ILanguageElement;
import org.lxtk.lx4e.model.ILanguageSourceFile;
import org.lxtk.lx4e.model.impl.LanguageElementDelta;

class DeltaProcessor
    implements IResourceDeltaVisitor
{
    private final List<IElementDelta> deltas = new ArrayList<>();

    IElementDelta[] getDeltas()
    {
        return deltas.toArray(ElementDeltas.EMPTY_ARRAY);
    }

    @Override
    public boolean visit(IResourceDelta delta) throws CoreException
    {
        switch (delta.getResource().getType())
        {
        case IResource.FILE:
            return processFile(delta);

        default:
            return true;
        }
    }

    private boolean processFile(IResourceDelta delta)
    {
        switch (delta.getKind())
        {
        case IResourceDelta.ADDED:
            return processAddedFile(delta);

        case IResourceDelta.REMOVED:
            return processRemovedFile(delta);

        case IResourceDelta.CHANGED:
            return processChangedFile(delta);

        default:
            return false;
        }
    }

    private boolean processAddedFile(IResourceDelta delta)
    {
        IFile file = (IFile)delta.getResource();
        ILanguageElement element = JsonCore.create(file);
        if (element != null)
        {
            long flags = 0;

            if (!isWorkingCopy(element))
                addToModel(element);
            else
                flags = F_UNDERLYING_RESOURCE;

            translateAddedDelta(delta, element, flags);
        }
        return false;
    }

    private boolean processRemovedFile(IResourceDelta delta)
    {
        IFile file = (IFile)delta.getResource();
        ILanguageElement element = JsonCore.create(file);
        if (element != null)
        {
            long flags = 0;

            if (!isWorkingCopy(element))
                removeFromModel(element);
            else
                flags = F_UNDERLYING_RESOURCE;

            translateRemovedDelta(delta, element, flags);
        }
        return false;
    }

    private boolean processChangedFile(IResourceDelta delta)
    {
        IFile file = (IFile)delta.getResource();
        ILanguageElement element = JsonCore.create(file);
        if (element != null)
        {
            LanguageElementDelta result = new LanguageElementDelta(element);
            result.setKind(CHANGED);

            long flags = 0;

            boolean isWorkingCopy = isWorkingCopy(element);

            if (isWorkingCopy)
                flags |= F_UNDERLYING_RESOURCE;

            if ((delta.getFlags() & ~(IResourceDelta.MARKERS | IResourceDelta.SYNC)) != 0)
            {
                flags |= F_CONTENT;
                if (!isWorkingCopy)
                    close(element);
            }

            if ((delta.getFlags() & IResourceDelta.MARKERS) != 0)
            {
                flags |= F_MARKERS;
                result.setMarkerDeltas(delta.getMarkerDeltas());
            }

            if ((delta.getFlags() & IResourceDelta.SYNC) != 0)
                flags |= F_SYNC;

            result.setFlags(flags);

            deltas.add(result);
        }
        return false;
    }

    private void addToModel(ILanguageElement element)
    {
        Body parentBody = findBody(element.getParent());
        if (parentBody != null)
            parentBody.addChild(element);
        close(element);
    }

    private void removeFromModel(ILanguageElement element)
    {
        Body parentBody = findBody(element.getParent());
        if (parentBody != null)
            parentBody.removeChild(element);
        close(element);
    }

    private void translateAddedDelta(IResourceDelta delta, ILanguageElement element, long flags)
    {
        LanguageElementDelta result = new LanguageElementDelta(element);
        result.setKind(ADDED);
        if ((delta.getFlags() & IResourceDelta.MOVED_FROM) != 0)
        {
            ILanguageElement movedFromElement = JsonCore.create(
                getResource(delta.getMovedFromPath(), delta.getResource().getType()));
            if (movedFromElement != null)
            {
                flags |= F_MOVED_FROM;
                result.setMovedFromElement(movedFromElement);
            }
        }
        result.setFlags(flags);
        deltas.add(result);
    }

    private void translateRemovedDelta(IResourceDelta delta, ILanguageElement element, long flags)
    {
        LanguageElementDelta result = new LanguageElementDelta(element);
        result.setKind(REMOVED);
        if ((delta.getFlags() & IResourceDelta.MOVED_TO) != 0)
        {
            ILanguageElement movedToElement =
                JsonCore.create(getResource(delta.getMovedToPath(), delta.getResource().getType()));
            if (movedToElement != null)
            {
                flags |= F_MOVED_TO;
                result.setMovedToElement(movedToElement);
            }
        }
        result.setFlags(flags);
        deltas.add(result);
    }

    private static boolean isWorkingCopy(ILanguageElement element)
    {
        return element instanceof ILanguageSourceFile
            && ((ILanguageSourceFile)element).isWorkingCopy();
    }

    private static Body findBody(ILanguageElement element)
    {
        if (element == null)
            return null;
        return (Body)((IElementImplExtension)element).findBody_();
    }

    private static void close(ILanguageElement element)
    {
        ((IElementImplExtension)element).close_();
    }

    private static IResource getResource(IPath fullPath, int resourceType)
    {
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        switch (resourceType)
        {
        case IResource.ROOT:
            return root;

        case IResource.PROJECT:
            return root.getProject(fullPath.lastSegment());

        case IResource.FOLDER:
            return root.getFolder(fullPath);

        case IResource.FILE:
            return root.getFile(fullPath);

        default:
            return null;
        }
    }
}
