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
package org.lxtk.lx4e.ui.format;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.lsp4j.FormattingOptions;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISources;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.statushandlers.StatusManager;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;
import org.eclipse.ui.texteditor.ITextEditor;
import org.lxtk.LanguageOperationTarget;
import org.lxtk.lx4e.internal.ui.Activator;
import org.lxtk.lx4e.ui.DefaultEditorHelper;
import org.lxtk.lx4e.ui.EditorHelper;

/**
 * Partial implementation of a handler that creates and runs a {@link Formatter}.
 */
public abstract class AbstractFormatHandler
    extends AbstractHandler
{
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        Formatter formatter = createFormatter(HandlerUtil.getActiveEditor(event));
        if (formatter != null)
        {
            formatter.setOptions(getFormattingOptions());
            try
            {
                PlatformUI.getWorkbench().getProgressService().busyCursorWhile(formatter);
            }
            catch (InvocationTargetException e)
            {
                StatusManager.getManager().handle(
                    Activator.createErrorStatus(Messages.AbstractFormatHandler_Execution_error,
                        e.getCause()),
                    StatusManager.LOG | StatusManager.SHOW);
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
            Object editor =
                ((IEvaluationContext)evaluationContext).getVariable(ISources.ACTIVE_EDITOR_NAME);
            Formatter formatter = createFormatter(editor);
            if (formatter != null && formatter.isApplicable())
                enabled = true;
        }
        setBaseEnabled(enabled);
    }

    /**
     * Given a context object, creates and returns a {@link Formatter}.
     *
     * @param context may be <code>null</code>
     * @return the created formatter, or <code>null</code>
     *  if no formatter can be created for the given context
     */
    protected Formatter createFormatter(Object context)
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
        return new Formatter(target, document, selection);
    }

    /**
     * Returns the corresponding {@link LanguageOperationTarget}
     * for the given editor.
     *
     * @param editor never <code>null</code>
     * @return the corresponding <code>LanguageOperationTarget</code>,
     *  or <code>null</code> if none
     */
    protected abstract LanguageOperationTarget getLanguageOperationTarget(IEditorPart editor);

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
     * Returns the formatting options.
     *
     * @return the formatting options (not <code>null</code>)
     */
    protected FormattingOptions getFormattingOptions()
    {
        IPreferenceStore store = EditorsUI.getPreferenceStore();
        int tabWidth =
            store.getInt(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_TAB_WIDTH);
        if (tabWidth == 0)
            tabWidth = 4;
        boolean insertSpaces =
            store.getBoolean(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SPACES_FOR_TABS);
        return new FormattingOptions(tabWidth, insertSpaces);
    }
}
