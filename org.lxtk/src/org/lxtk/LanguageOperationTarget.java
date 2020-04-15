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
import java.util.Objects;

/**
 * A triple (3-tuple) of a document URI, a language identifier,
 * and a {@link LanguageService}.
 */
public final class LanguageOperationTarget
{
    private final URI documentUri;
    private final String languageId;
    private final LanguageService languageService;

    /**
     * Constructor.
     *
     * @param documentUri not <code>null</code>
     * @param languageId not <code>null</code>
     * @param languageService not <code>null</code>
     */
    public LanguageOperationTarget(URI documentUri, String languageId,
        LanguageService languageService)
    {
        this.documentUri = Objects.requireNonNull(documentUri);
        this.languageId = Objects.requireNonNull(languageId);
        this.languageService = Objects.requireNonNull(languageService);
    }

    /**
     * Returns the document URI.
     *
     * @return the document URI (never <code>null</code>)
     */
    public URI getDocumentUri()
    {
        return documentUri;
    }

    /**
     * Returns the language identifier.
     *
     * @return the language identifier (never <code>null</code>)
     */
    public String getLanguageId()
    {
        return languageId;
    }

    /**
     * Returns the language service.
     *
     * @return the language service (never <code>null</code>)
     */
    public LanguageService getLanguageService()
    {
        return languageService;
    }
}
