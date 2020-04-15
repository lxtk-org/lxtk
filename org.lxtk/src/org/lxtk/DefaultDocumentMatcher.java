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
package org.lxtk;

import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;

import org.eclipse.lsp4j.DocumentFilter;

/**
 * Default implementation of the {@link DocumentMatcher} interface. Thread-safe.
 *
 * @noinstantiate This class is not intended to be instantiated by clients.
 *  Use the provided {@link #INSTANCE}.
 */
public final class DefaultDocumentMatcher
    implements DocumentMatcher
{
    /**
     * The default instance of the document matcher.
     */
    public static final DocumentMatcher INSTANCE = new DefaultDocumentMatcher();

    private static final String ASTERISK = "*"; //$NON-NLS-1$
    private static final FileSystem FS = FileSystems.getDefault();

    @Override
    public int match(DocumentFilter filter, URI documentUri,
        String documentLanguage)
    {
        String language = filter.getLanguage();
        String scheme = filter.getScheme();
        String pattern = filter.getPattern();

        int result = 0;

        if (scheme != null)
        {
            if (scheme.equals(documentUri.getScheme()))
                result = 10;
            else if (scheme.equals(ASTERISK))
                result = 5;
            else
                return 0;
        }

        if (language != null)
        {
            if (language.equals(documentLanguage))
                result = 10;
            else if (language.equals(ASTERISK))
                result = Math.max(result, 5);
            else
                return 0;
        }

        if (pattern != null)
        {
            if (matchGlobPattern(pattern, documentUri.getPath()))
                result = 10;
            else
                return 0;
        }

        return result;
    }

    private static boolean matchGlobPattern(String pattern, String path)
    {
        return FS.getPathMatcher("glob:" + pattern).matches( //$NON-NLS-1$
            FS.getPath(path));
    }

    private DefaultDocumentMatcher()
    {
    }
}
