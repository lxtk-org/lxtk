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
package org.lxtk.lx4e.requests;

import java.text.MessageFormat;
import java.util.concurrent.Future;

import org.eclipse.lsp4j.SignatureHelp;
import org.eclipse.lsp4j.TextDocumentPositionParams;
import org.lxtk.SignatureHelpProvider;

/**
 * A request for computing signature help.
 */
public class SignatureHelpRequest
    extends LanguageFeatureRequest<SignatureHelpProvider, TextDocumentPositionParams, SignatureHelp>
{
    @Override
    protected Future<SignatureHelp> send(SignatureHelpProvider provider,
        TextDocumentPositionParams params)
    {
        setTitle(MessageFormat.format(Messages.SignatureHelpRequest_title, params));
        return provider.getSignatureHelp(params);
    }
}
