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
package org.lxtk.lx4e;

import java.net.URI;
import java.util.Objects;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.handly.buffer.IBuffer;
import org.lxtk.DocumentService;
import org.lxtk.TextDocument;
import org.lxtk.lx4e.util.ResourceUtil;

/**
 * Default implementation of a {@link IUriHandler} that maps URIs to
 * text documents managed by a given {@link DocumentService}.
 */
public class TextDocumentUriHandler
    implements IUriHandler
{
    /**
     * The associated {@link DocumentService} (never <code>null</code>).
     */
    protected final DocumentService documentService;

    /**
     * Constructor.
     *
     * @param documentService not <code>null</code>
     */
    public TextDocumentUriHandler(DocumentService documentService)
    {
        this.documentService = Objects.requireNonNull(documentService);
    }

    @Override
    public Object getCorrespondingElement(URI uri)
    {
        TextDocument textDocument = documentService.getTextDocument(uri);
        if (textDocument instanceof EclipseTextDocument)
            return ((EclipseTextDocument)textDocument).getCorrespondingElement();
        return null;
    }

    @Override
    public Boolean exists(URI uri)
    {
        if (documentService.getTextDocument(uri) != null)
            return true;
        return null;
    }

    @Override
    public IBuffer getBuffer(URI uri) throws CoreException
    {
        TextDocument textDocument = documentService.getTextDocument(uri);
        if (textDocument instanceof EclipseTextDocument)
            return ((EclipseTextDocument)textDocument).getBuffer();
        return null;
    }

    @Override
    public String toDisplayString(URI uri)
    {
        IResource resource = ResourceUtil.getResource(getCorrespondingElement(uri));
        if (resource != null)
            return resource.getFullPath().makeRelative().toString();
        return null;
    }
}
