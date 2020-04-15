/*******************************************************************************
 * Copyright (c) 2019, 2020 1C-Soft LLC.
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
package org.lxtk.lx4e.util;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Adapters;

/**
 * Provides static utility methods related to Eclipse workspace resources.
 */
public class ResourceUtil
{
    /**
     * Returns the resource corresponding to the given model element,
     * or <code>null</code> if there is no corresponding resource.
     *
     * @param element a model element (may be <code>null</code>, in which case
     *  <code>null</code> is returned)
     * @return the corresponding {@link IResource}, or <code>null</code> if none
     */
    public static IResource getResource(Object element)
    {
        return Adapters.adapt(element, IResource.class);
    }

    /**
     * Returns the file corresponding to the given model element,
     * or <code>null</code> if there is no corresponding file.
     *
     * @param element a model element (may be <code>null</code>, in which case
     *  <code>null</code> is returned)
     * @return the corresponding {@link IFile}, or <code>null</code> if none
     */
    public static IFile getFile(Object element)
    {
        if (element == null)
            return null;

        if (element instanceof IFile)
            return (IFile)element;

        IFile file = Adapters.adapt(element, IFile.class);
        if (file != null)
            return file;

        IResource resource = Adapters.adapt(element, IResource.class);
        if (resource instanceof IFile)
            return (IFile)resource;

        return null;
    }

    private ResourceUtil()
    {
    }
}
