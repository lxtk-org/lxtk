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
import java.util.List;
import java.util.concurrent.Future;

import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.ReferenceParams;
import org.lxtk.ReferenceProvider;

/**
 * A request for computing references.
 */
public class ReferencesRequest
    extends LanguageFeatureRequest<ReferenceProvider, ReferenceParams,
        List<? extends Location>>
{
    @Override
    protected Future<List<? extends Location>> send(ReferenceProvider provider,
        ReferenceParams params)
    {
        setTitle(
            MessageFormat.format(Messages.ReferencesRequest_title, params));
        return provider.getReferences(params);
    }
}
