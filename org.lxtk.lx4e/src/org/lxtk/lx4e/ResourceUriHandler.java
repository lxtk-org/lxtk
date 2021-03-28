/*******************************************************************************
 * Copyright (c) 2019, 2021 1C-Soft LLC.
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
import org.eclipse.core.runtime.IPath;
import org.eclipse.handly.buffer.IBuffer;
import org.eclipse.handly.buffer.TextFileBuffer;
import org.eclipse.lsp4j.CreateFileOptions;
import org.eclipse.lsp4j.DeleteFileOptions;
import org.eclipse.lsp4j.RenameFileOptions;
import org.eclipse.ltk.core.refactoring.Change;
import org.lxtk.lx4e.internal.CreateFileChange;
import org.lxtk.lx4e.internal.CreateResourceOptions;
import org.lxtk.lx4e.internal.DeleteResourceChange;
import org.lxtk.lx4e.internal.DeleteResourceOptions;
import org.lxtk.lx4e.internal.MoveResourceChange;
import org.lxtk.lx4e.internal.MoveResourceOptions;

/**
 * Default implementation of a {@link IUriHandler} that maps URIs to
 * Eclipse workspace resources.
 */
public class ResourceUriHandler
    implements IUriHandler
{
    @Override
    public IResource getCorrespondingElement(URI uri)
    {
        IContainer container = getContainer(uri);
        if (container != null && container.exists())
            return container;
        IFile file = getFile(uri);
        if (file != null)
            return file; // note that the returned file may not currently exist
        // note also that inexistent file is preferred to inexistent container
        return container; // note that container here is either null or does not currently exist
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
        IPath path = getResourcePath(uri);
        if (path != null)
            return path.makeRelative().toString();
        return null;
    }

    @Override
    public Change getCreateFileChange(URI uri, CreateFileOptions options) throws CoreException
    {
        IFile file = getFile(uri);
        if (file == null)
            return null;

        boolean overwrite = options != null && Boolean.TRUE.equals(options.getOverwrite());
        boolean ignoreIfExists =
            options != null && Boolean.TRUE.equals(options.getIgnoreIfExists());

        return new CreateFileChange(file, new CreateResourceOptions(overwrite, ignoreIfExists));
    }

    @Override
    public Change getDeleteFileChange(URI uri, DeleteFileOptions options) throws CoreException
    {
        IPath resourcePath = getResourcePath(uri);
        if (resourcePath == null)
            return null;

        boolean recursive = options != null && Boolean.TRUE.equals(options.getRecursive());
        boolean ignoreIfNotExists =
            options != null && Boolean.TRUE.equals(options.getIgnoreIfNotExists());

        return new DeleteResourceChange(resourcePath,
            new DeleteResourceOptions(recursive, ignoreIfNotExists));
    }

    @Override
    public Change getRenameFileChange(URI uri, URI newUri, RenameFileOptions options)
        throws CoreException
    {
        IResource source = getCorrespondingElement(uri);
        if (source == null)
            return null;

        IPath destination = getResourcePath(newUri);
        if (destination == null)
            return null;

        boolean overwrite = options != null && Boolean.TRUE.equals(options.getOverwrite());
        boolean ignoreIfExists =
            options != null && Boolean.TRUE.equals(options.getIgnoreIfExists());

        return new MoveResourceChange(source, destination,
            new MoveResourceOptions(overwrite, ignoreIfExists));
    }

    /**
     * Returns an {@link IFile} corresponding to the given URI.
     *
     * @param uri never <code>null</code>
     * @return the corresponding <code>IFile</code>,
     *  or <code>null</code> if none
     */
    protected IFile getFile(URI uri)
    {
        return getNonLinkedOne(
            ResourcesPlugin.getWorkspace().getRoot().findFilesForLocationURI(uri));
    }

    /**
     * Returns an {@link IContainer} corresponding to the given URI.
     *
     * @param uri never <code>null</code>
     * @return the corresponding <code>IContainer</code>,
     *  or <code>null</code> if none
     */
    protected IContainer getContainer(URI uri)
    {
        return getNonLinkedOne(
            ResourcesPlugin.getWorkspace().getRoot().findContainersForLocationURI(uri));
    }

    private static <T extends IResource> T getNonLinkedOne(T[] resources)
    {
        for (T resource : resources)
        {
            if (!resource.isLinked(IResource.CHECK_ANCESTORS))
                return resource;
        }
        return null;
    }

    private IPath getResourcePath(URI uri)
    {
        IResource resource = getFile(uri);
        if (resource == null)
            resource = getContainer(uri);
        if (resource != null)
            return resource.getFullPath();
        return null;
    }
}
