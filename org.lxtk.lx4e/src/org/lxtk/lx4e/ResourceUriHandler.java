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
package org.lxtk.lx4e;

import java.net.URI;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.handly.buffer.IBuffer;
import org.eclipse.handly.buffer.TextFileBuffer;
import org.lxtk.lx4e.internal.Activator;

/**
 * TODO JavaDoc
 */
public class ResourceUriHandler
    implements IUriHandler
{
    @Override
    public IResource getCorrespondingElement(URI uri)
    {
        IFile file = getFile(uri);
        if (file != null)
        {
            ensureSynchronized(file);
            if (file.exists())
                return file;
        }
        IContainer container = getContainer(uri);
        if (container != null)
        {
            ensureSynchronized(container);
            if (container.exists())
                return container;
        }
        return null; // avoid any ambiguity, potential or real
    }

    @Override
    public Boolean exists(URI uri)
    {
        IResource resource = getCorrespondingElement(uri);
        if (resource != null)
            return resource.exists();
        return null;
    }

    @Override
    public IBuffer getBuffer(URI uri) throws CoreException
    {
        IResource resource = getCorrespondingElement(uri);
        if (resource instanceof IFile)
            return TextFileBuffer.forFile((IFile)resource);
        return null;
    }

    @Override
    public String toDisplayString(URI uri)
    {
        IResource resource = getCorrespondingElement(uri);
        if (resource != null)
            return resource.getFullPath().makeRelative().toString();
        return null;
    }

    protected IFile getFile(URI uri)
    {
        return getNonLinkedOne(
            ResourcesPlugin.getWorkspace().getRoot().findFilesForLocationURI(
                uri));
    }

    protected IContainer getContainer(URI uri)
    {
        return getNonLinkedOne(
            ResourcesPlugin.getWorkspace().getRoot().findContainersForLocationURI(
                uri));
    }

    private static <T extends IResource> T getNonLinkedOne(T[] resources)
    {
        for (T resource : resources)
        {
            if (!resource.isLinked())
                return resource;
        }
        return null;
    }

    private static void ensureSynchronized(IResource resource)
    {
        if (!resource.isSynchronized(IResource.DEPTH_ZERO))
        {
            try
            {
                resource.refreshLocal(IResource.DEPTH_ZERO, null);
            }
            catch (CoreException e)
            {
                Activator.logError(e);
            }
        }
    }
}
