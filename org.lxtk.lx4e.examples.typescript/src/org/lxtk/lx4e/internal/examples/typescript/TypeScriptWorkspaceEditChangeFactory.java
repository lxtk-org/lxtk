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
package org.lxtk.lx4e.internal.examples.typescript;

import org.lxtk.lx4e.examples.typescript.TypeScriptCore;
import org.lxtk.lx4e.refactoring.WorkspaceEditChangeFactory;

/**
 * TypeScript-specific extension of {@link WorkspaceEditChangeFactory}.
 */
public class TypeScriptWorkspaceEditChangeFactory
    extends WorkspaceEditChangeFactory
{
    public static final TypeScriptWorkspaceEditChangeFactory INSTANCE =
        new TypeScriptWorkspaceEditChangeFactory();

    private TypeScriptWorkspaceEditChangeFactory()
    {
        super(TypeScriptCore.WORKSPACE);
    }
}
