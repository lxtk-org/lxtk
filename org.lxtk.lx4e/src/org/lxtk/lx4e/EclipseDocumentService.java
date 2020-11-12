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

import java.util.function.Consumer;

import org.lxtk.DefaultDocumentService;
import org.lxtk.DocumentService;
import org.lxtk.lx4e.internal.Activator;

/**
 * Default implementation of {@link DocumentService} for Eclipse.
 * <p>
 * This implementation is thread-safe.
 * </p>
 */
public class EclipseDocumentService
    extends DefaultDocumentService
{
    @Override
    protected Consumer<Throwable> getLogger()
    {
        return Activator.LOGGER;
    }
}
