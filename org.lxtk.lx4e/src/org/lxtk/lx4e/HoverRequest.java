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
package org.lxtk.lx4e;

import java.text.MessageFormat;
import java.util.concurrent.Future;

import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.TextDocumentPositionParams;
import org.lxtk.HoverProvider;

/**
 * A request for computing hover information.
 */
public class HoverRequest
    extends LanguageFeatureRequest<HoverProvider, TextDocumentPositionParams, Hover>
{
    @Override
    protected Future<Hover> send(HoverProvider provider, TextDocumentPositionParams params)
    {
        setTitle(MessageFormat.format(Messages.HoverRequest_title, params));
        return provider.getHover(params);
    }
}
