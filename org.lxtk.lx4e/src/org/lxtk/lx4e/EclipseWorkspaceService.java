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

import org.lxtk.DefaultWorkspaceService;
import org.lxtk.WorkspaceService;
import org.lxtk.lx4e.internal.Activator;

/**
 * Default implementation of {@link WorkspaceService} for Eclipse.
 * <p>
 * This implementation is thread-safe.
 * </p>
 */
public class EclipseWorkspaceService
    extends DefaultWorkspaceService
{
    @Override
    protected Consumer<Throwable> getLogger()
    {
        return Activator.LOGGER;
    }
}
