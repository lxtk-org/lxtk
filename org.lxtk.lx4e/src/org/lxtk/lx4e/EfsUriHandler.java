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

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.handly.buffer.IBuffer;
import org.eclipse.handly.buffer.TextFileBuffer;

/**
 * TODO JavaDoc
 */
public class EfsUriHandler
    implements IUriHandler
{
    @Override
    public IFileStore getCorrespondingElement(URI uri)
    {
        try
        {
            return EFS.getStore(uri);
        }
        catch (CoreException e)
        {
            return null;
        }
    }

    @Override
    public Boolean exists(URI uri)
    {
        IFileStore fileStore = getCorrespondingElement(uri);
        if (fileStore != null)
            return fileStore.fetchInfo().exists();
        return null;
    }

    @Override
    public IBuffer getBuffer(URI uri) throws CoreException
    {
        IFileStore fileStore = getCorrespondingElement(uri);
        if (fileStore != null)
            return TextFileBuffer.forFileStore(fileStore);
        return null;
    }

    @Override
    public String toDisplayString(URI uri)
    {
        IFileStore fileStore = getCorrespondingElement(uri);
        if (fileStore != null)
            return fileStore.toString();
        return null;
    }
}
