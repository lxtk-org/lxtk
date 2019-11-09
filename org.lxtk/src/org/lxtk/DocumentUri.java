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
package org.lxtk;

import java.net.URI;

import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.lxtk.util.UriUtil;

/**
 * TODO JavaDoc
 */
public class DocumentUri
{
    /**
     * TODO JavaDoc
     *
     * @param uri a document URI (not <code>null</code>)
     * @return a string representation of the given URI in a form suitable for
     *  transferring to a language server (never <code>null</code>)
     */
    public static String convert(URI uri)
    {
        return UriUtil.toWireString(uri);
    }

    /**
     * TODO JavaDoc
     *
     * @param uri a string representation of a document URI as transferred from
     *  a language server (not <code>null</code>)
     * @return the created <code>URI</code> (never <code>null</code>)
     * @throws IllegalArgumentException if the string violates RFC 3986
     */
    public static URI convert(String uri)
    {
        return UriUtil.fromWireString(uri);
    }

    /**
     * TODO JavaDoc
     *
     * @param uri a document URI (not <code>null</code>)
     * @return the created <code>TextDocumentIdentifier</code> (never <code>null</code>)
     */
    public static TextDocumentIdentifier toTextDocumentIdentifier(URI uri)
    {
        return new TextDocumentIdentifier(convert(uri));
    }

    private DocumentUri()
    {
    }
}
