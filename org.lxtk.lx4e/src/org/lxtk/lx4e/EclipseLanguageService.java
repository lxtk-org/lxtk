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
package org.lxtk.lx4e;

import org.eclipse.lsp4j.CompletionCapabilities;
import org.lxtk.DefaultLanguageService;
import org.lxtk.LanguageService;

/**
 * Default implementation of {@link LanguageService} for Eclipse.
 * <p>
 * This implementation is thread-safe.
 * </p>
 */
public class EclipseLanguageService
    extends DefaultLanguageService
{
    @Override
    public CompletionCapabilities getCompletionCapabilities()
    {
        CompletionCapabilities capabilities = super.getCompletionCapabilities();
        capabilities.setContextSupport(false);
        return capabilities;
    }
}
