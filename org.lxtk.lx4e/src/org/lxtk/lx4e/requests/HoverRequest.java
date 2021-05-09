/*******************************************************************************
 * Copyright (c) 2020, 2021 1C-Soft LLC.
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
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverParams;
import org.lxtk.HoverProvider;

/**
 * Requests hover information for the given text document position.
 */
public class HoverRequest
    extends LanguageFeatureRequestWithWorkDoneProgress<HoverProvider, HoverParams, Hover>
{
    @Override
    protected CompletableFuture<Hover> send(HoverProvider provider, HoverParams params)
    {
        setTitle(MessageFormat.format(Messages.HoverRequest_title, params));
        return provider.getHover(params);
    }
}
