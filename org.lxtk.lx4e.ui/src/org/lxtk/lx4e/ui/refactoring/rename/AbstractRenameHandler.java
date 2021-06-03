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
package org.lxtk.lx4e.ui.refactoring.rename;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISources;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.texteditor.ITextEditor;
import org.lxtk.LanguageOperationTarget;
import org.lxtk.lx4e.IWorkspaceEditChangeFactory;
import org.lxtk.lx4e.refactoring.rename.RenameRefactoring;
import org.lxtk.lx4e.ui.DefaultEditorHelper;
import org.lxtk.lx4e.ui.EditorHelper;

/**
 * Partial implementation of a handler that starts a {@link RenameRefactoring}.
 */
public abstract class AbstractRenameHandler
    extends AbstractHandler
{
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        IEditorPart activeEditor = HandlerUtil.getActiveEditor(event);
        if (activeEditor == null)
            return null;

        RenameRefactoring refactoring = createRefactoring(activeEditor);
        if (refactoring == null)
            return null;

        RenameRefactoringStarter starter = getRefactoringStarter(refactoring);
        ITextEditor textEditor = getEditorHelper().getTextEditor(activeEditor);
        if (textEditor != null && linkedModePreferred())
            starter.startLinkedEditing(textEditor);
        else
            starter.startRefactoringWithDialog(HandlerUtil.getActiveShell(event), true);
        return null;
    }

    @Override
    public void setEnabled(Object evaluationContext)
    {
        boolean enabled = false;
        if (evaluationContext instanceof IEvaluationContext)
        {
            Object editor =
                ((IEvaluationContext)evaluationContext).getVariable(ISources.ACTIVE_EDITOR_NAME);
            RenameRefactoring refactoring = createRefactoring(editor);
            if (refactoring != null && refactoring.isApplicable())
                enabled = true;
        }
        setBaseEnabled(enabled);
    }

    /**
     * Given a context object, creates and returns the corresponding {@link RenameRefactoring}.
     *
     * @param context may be <code>null</code>
     * @return the created refactoring object, or <code>null</code>
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
     * Creates and returns a {@link RenameRefactoring} with the given parameters.
     *
     * @param target the {@link LanguageOperationTarget} for the refactoring
     *  (not <code>null</code>)
     * @param document the target {@link IDocument} for the refactoring
     *  (not <code>null</code>)
     * @param offset the target document offset for the refactoring (0-based)
     * @return the created refactoring object, or <code>null</code>
     *  if the refactoring is not available
     */
    protected RenameRefactoring createRefactoring(LanguageOperationTarget target,
        IDocument document, int offset)
    {
        return new RenameRefactoring(Messages.AbstractRenameHandler_Refactoring_name, target,
            document, offset, getWorkspaceEditChangeFactory());
    }

    /**
     * Returns the corresponding {@link LanguageOperationTarget} for the given editor.
     *
     * @param editor never <code>null</code>
     * @return the corresponding <code>LanguageOperationTarget</code>,
     *  or <code>null</code> if none
     */
    protected abstract LanguageOperationTarget getLanguageOperationTarget(IEditorPart editor);

    /**
     * Returns the workspace edit change factory to be used by the rename refactoring.
     *
     * @return the workspace edit change factory (not <code>null</code>)
     */
    protected abstract IWorkspaceEditChangeFactory getWorkspaceEditChangeFactory();

    /**
     * Returns the {@link EditorHelper} for this handler.
     *
     * @return the editor helper (not <code>null</code>)
     */
    protected EditorHelper getEditorHelper()
    {
        return DefaultEditorHelper.INSTANCE;
    }

    /**
     * Returns a starter for the given refactoring.
     *
     * @param refactoring never <code>null</code>
     * @return the refactoring starter (not <code>null</code>)
     */
    protected RenameRefactoringStarter getRefactoringStarter(RenameRefactoring refactoring)
    {
        return new RenameRefactoringStarter(refactoring);
    }

    /**
     * Returns whether this handler should start refactoring in linked editing mode,
     * if available.
     *
     * @return <code>true</code> if linked mode should be used, and <code>false</code> otherwise
     */
    protected boolean linkedModePreferred()
    {
        return true;
    }
}
