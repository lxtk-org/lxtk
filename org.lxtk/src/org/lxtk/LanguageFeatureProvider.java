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

import java.util.List;

import org.eclipse.lsp4j.DocumentFilter;
import org.eclipse.lsp4j.TextDocumentRegistrationOptions;

/**
 * TODO JavaDoc
 */
public interface LanguageFeatureProvider
{
    /**
     * TODO JavaDoc
     *
     * @return registration options (never <code>null</code>).
     *  Clients <b>must not</b> modify the returned object.
     */
    TextDocumentRegistrationOptions getRegistrationOptions();

    /**
     * TODO JavaDoc
     *
     * @return the document selector (may be <code>null</code>).
     *  Clients <b>must not</b> modify the returned list or any of its elements.
     */
    default List<DocumentFilter> getDocumentSelector()
    {
        return getRegistrationOptions().getDocumentSelector();
    }
}
