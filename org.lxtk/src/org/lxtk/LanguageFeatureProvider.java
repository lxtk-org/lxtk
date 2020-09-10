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

import java.util.List;

import org.eclipse.lsp4j.DocumentFilter;
import org.eclipse.lsp4j.TextDocumentRegistrationOptions;

/**
 * Common interface for language feature providers.
 *
 * @param <RO> registration options type
 * @see LanguageService
 */
public interface LanguageFeatureProvider<RO>
{
    /**
     * Returns registration options for this provider.
     *
     * @return registration options. Clients <b>must not</b> modify the returned object
     */
    RO getRegistrationOptions();

    /**
     * Returns the document selector for this provider.
     *
     * @return the document selector, or <code>null</code> if none.
     *  Clients <b>must not</b> modify the returned list or any of its elements
     */
    default List<DocumentFilter> getDocumentSelector()
    {
        RO registrationOptions = getRegistrationOptions();
        if (registrationOptions instanceof TextDocumentRegistrationOptions)
            return ((TextDocumentRegistrationOptions)registrationOptions).getDocumentSelector();
        return null;
    }
}
