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
package org.lxtk.lx4e.ui.refactoring.rename;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISources;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.texteditor.ITextEditor;
import org.lxtk.LanguageOperationTarget;
import org.lxtk.lx4e.refactoring.WorkspaceEditChangeFactory;
import org.lxtk.lx4e.refactoring.rename.RenameRefactoring;
import org.lxtk.lx4e.ui.DefaultEditorHelper;
import org.lxtk.lx4e.ui.EditorHelper;

/**
 * TODO JavaDoc
 */
public abstract class AbstractRenameHandler
    extends AbstractHandler
{
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        RenameRefactoring refactoring = createRefactoring(
            HandlerUtil.getActiveEditor(event));
        if (refactoring != null)
        {
            RefactoringWizardOpenOperation op =
                new RefactoringWizardOpenOperation(new RenameRefactoringWizard(
                    refactoring));
            try
            {
                op.run(HandlerUtil.getActiveShell(event),
                    refactoring.getName());
            }
            catch (InterruptedException e)
            {
                // do nothing: got canceled by the user
            }
        }
        return null;
    }

    @Override
    public void setEnabled(Object evaluationContext)
    {
        boolean enabled = false;
        if (evaluationContext instanceof IEvaluationContext)
        {
            Object editor = ((IEvaluationContext)evaluationContext).getVariable(
                ISources.ACTIVE_EDITOR_NAME);
            RenameRefactoring refactoring = createRefactoring(editor);
            if (refactoring != null && refactoring.isApplicable())
                enabled = true;
        }
        setBaseEnabled(enabled);
    }

    /**
     * TODO JavaDoc
     *
     * @param context may be <code>null</code>
     * @return a {@link RenameRefactoring}, or <code>null</code>
     *  if the refactoring is not available
     */
    protected RenameRefactoring createRefactoring(Object context)
    {
        EditorHelper editorHelper = getEditorHelper();
        ITextEditor editor = editorHelper.getTextEditor(context);
        if (editor == null)
            return null;
        ITextSelection selection = editorHelper.getTextSelection(editor);
        if (selection == null)
            return null;
        IDocument document = editorHelper.getDocument(editor);
        if (document == null)
            return null;
        LanguageOperationTarget target = getLanguageOperationTarget(editor);
        if (target == null)
            return null;
        return createRefactoring(target, document, selection.getOffset());
    }

    /**
     * TODO JavaDoc
     *
     * @param target never <code>null</code>
     * @param document never <code>null</code>
     * @param offset 0-based
     * @return a {@link RenameRefactoring}, or <code>null</code>
     *  if the refactoring is not available
     */
    protected RenameRefactoring createRefactoring(
        LanguageOperationTarget target, IDocument document, int offset)
    {
        return new RenameRefactoring(
            Messages.AbstractRenameHandler_Refactoring_name, target, document,
            offset, getWorkspaceEditChangeFactory());
    }

    /**
     * TODO JavaDoc
     *
     * @param editor never <code>null</code>
     * @return the corresponding {@link LanguageOperationTarget},
     *  or <code>null</code> if none
     */
    protected abstract LanguageOperationTarget getLanguageOperationTarget(
        IEditorPart editor);

    /**
     * TODO JavaDoc
     *
     * @return a {@link WorkspaceEditChangeFactory} (not <code>null</code>)
     */
    protected abstract WorkspaceEditChangeFactory getWorkspaceEditChangeFactory();

    /**
     * TODO JavaDoc
     *
     * @return an editor helper (not <code>null</code>)
     */
    protected EditorHelper getEditorHelper()
    {
        return DefaultEditorHelper.INSTANCE;
    }
}
