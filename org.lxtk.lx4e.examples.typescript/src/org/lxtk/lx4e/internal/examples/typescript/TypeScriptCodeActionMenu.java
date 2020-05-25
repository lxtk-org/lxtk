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
package org.lxtk.lx4e.internal.examples.typescript;

import java.util.List;

import org.eclipse.ui.IEditorPart;
import org.lxtk.LanguageOperationTarget;
import org.lxtk.lx4e.refactoring.WorkspaceEditChangeFactory;
import org.lxtk.lx4e.ui.codeaction.AbstractCodeActionMenu;

/**
 * TypeScript-specific extension of {@link AbstractCodeActionMenu}.
 */
public class TypeScriptCodeActionMenu
    extends AbstractCodeActionMenu
{
    @Override
    protected LanguageOperationTarget getLanguageOperationTarget()
    {
        return TypeScriptOperationTargetProvider.getOperationTarget((IEditorPart)getActivePart());
    }

    @Override
    protected WorkspaceEditChangeFactory getWorkspaceEditChangeFactory()
    {
        return TypeScriptWorkspaceEditChangeFactory.INSTANCE;
    }

    @Override
    protected List<String> getCodeActionKinds()
    {
        return null;
    }
}
