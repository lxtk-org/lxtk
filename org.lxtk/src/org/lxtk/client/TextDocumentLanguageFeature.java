/*******************************************************************************
 * Copyright (c) 2020 1C-Soft LLC.
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
package org.lxtk.client;

import org.eclipse.lsp4j.ClientCapabilities;
import org.eclipse.lsp4j.TextDocumentClientCapabilities;
import org.eclipse.lsp4j.TextDocumentRegistrationOptions;
import org.lxtk.LanguageService;

abstract class TextDocumentLanguageFeature<RO extends TextDocumentRegistrationOptions>
    extends LanguageFeature<RO>
{
    /**
     * Constructor.
     *
     * @param languageService not <code>null</code>
     */
    public TextDocumentLanguageFeature(LanguageService languageService)
    {
        super(languageService);
    }

    @Override
    public final void fillClientCapabilities(ClientCapabilities capabilities)
    {
        fillClientCapabilities(ClientCapabilitiesUtil.getOrCreateTextDocument(capabilities));
    }

    /**
     * Fills the text document client capabilities this feature implements.
     *
     * @param capabilities never <code>null</code>
     */
    abstract void fillClientCapabilities(TextDocumentClientCapabilities capabilities);

    @Override
    boolean checkRegistrationOptions(RO options)
    {
        return options != null && options.getDocumentSelector() != null;
    }
}
