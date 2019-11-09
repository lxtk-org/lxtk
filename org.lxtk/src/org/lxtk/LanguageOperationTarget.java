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
import java.util.Objects;

/**
 * TODO JavaDoc
 */
public final class LanguageOperationTarget
{
    private final URI documentUri;
    private final String languageId;
    private final LanguageService languageService;

    /**
     * TODO JavaDoc
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
     * TODO JavaDoc
     *
     * @return the document URI (never <code>null</code>)
     */
    public URI getDocumentUri()
    {
        return documentUri;
    }

    /**
     * TODO JavaDoc
     *
     * @return the language id (never <code>null</code>)
     */
    public String getLanguageId()
    {
        return languageId;
    }

    /**
     * TODO JavaDoc
     *
     * @return the language service (never <code>null</code>)
     */
    public LanguageService getLanguageService()
    {
        return languageService;
    }
}
