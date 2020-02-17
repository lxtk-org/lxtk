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
package org.lxtk.lx4e.internal.examples.typescript.editor;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.ui.IEditorPart;
import org.lxtk.LanguageOperationTarget;
import org.lxtk.lx4e.internal.examples.typescript.Activator;
import org.lxtk.lx4e.internal.examples.typescript.TypeScriptOperationTargetProvider;
import org.lxtk.lx4e.internal.examples.typescript.TypeScriptWorkspaceEditChangeFactory;
import org.lxtk.lx4e.refactoring.WorkspaceEditChangeFactory;
import org.lxtk.lx4e.refactoring.rename.RenameRefactoring;
import org.lxtk.lx4e.ui.refactoring.rename.AbstractRenameHandler;
import org.lxtk.lx4e.util.DefaultWordFinder;

/**
 * TODO JavaDoc
 */
public class RenameHandler
    extends AbstractRenameHandler
{
    @Override
    protected RenameRefactoring createRefactoring(
        LanguageOperationTarget target, IDocument document, int offset)
    {
        RenameRefactoring refactoring = super.createRefactoring(target,
            document, offset);
        if (refactoring != null && refactoring.isApplicable())
        {
            IRegion r = DefaultWordFinder.INSTANCE.findWord(document, offset);
            if (r != null && r.getLength() > 0)
            {
                try
                {
                    refactoring.setCurrentName(document.get(r.getOffset(),
                        r.getLength()));
                    return refactoring;
                }
                catch (BadLocationException e)
                {
                    Activator.logError(e);
                }
            }
        }
        return null;
    }

    @Override
    protected LanguageOperationTarget getLanguageOperationTarget(
        IEditorPart editor)
    {
        return TypeScriptOperationTargetProvider.getOperationTarget(editor);
    }

    @Override
    protected WorkspaceEditChangeFactory getWorkspaceEditChangeFactory()
    {
        return TypeScriptWorkspaceEditChangeFactory.INSTANCE;
    }
}
