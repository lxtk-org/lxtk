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
package org.lxtk;

import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.SignatureHelp;
import org.eclipse.lsp4j.SignatureHelpRegistrationOptions;
import org.eclipse.lsp4j.TextDocumentPositionParams;

/**
 * TODO JavaDoc
 */
public interface SignatureHelpProvider
    extends LanguageFeatureProvider
{
    @Override
    SignatureHelpRegistrationOptions getRegistrationOptions();

    /**
     * TODO JavaDoc
     *
     * @param params not <code>null</code>
     * @return result future (never <code>null</code>)
     */
    CompletableFuture<SignatureHelp> getSignatureHelp(
        TextDocumentPositionParams params);
}
