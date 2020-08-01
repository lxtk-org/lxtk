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
package org.lxtk.lx4e.ui.references;

import java.text.MessageFormat;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.lsp4j.Position;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISources;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.texteditor.ITextEditor;
import org.lxtk.DocumentService;
import org.lxtk.LanguageOperationTarget;
import org.lxtk.lx4e.DocumentUtil;
import org.lxtk.lx4e.internal.ui.Activator;
import org.lxtk.lx4e.ui.DefaultEditorHelper;
import org.lxtk.lx4e.ui.EditorHelper;
import org.lxtk.lx4e.util.DefaultWordFinder;

/**
 * Partial implementation of a handler that creates and runs a {@link ReferenceSearchQuery}.
 */
public abstract class AbstractFindReferencesHandler
    extends AbstractHandler
{
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        ReferenceSearchQuery query = createSearchQuery(HandlerUtil.getActiveEditor(event));
        if (query != null)
            NewSearchUI.runQueryInBackground(query);
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
            ReferenceSearchQuery query = createSearchQuery(editor);
            if (query != null && query.canRun())
                enabled = true;
        }
        setBaseEnabled(enabled);
    }

    /**
     * Creates and returns a {@link ReferenceSearchQuery} for the given context.
     *
     * @param context may be <code>null</code>
     * @return the created search query, or <code>null</code> if none
     */
    protected ReferenceSearchQuery createSearchQuery(Object context)
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
        IRegion wordRegion = findWord(document, selection.getOffset());
        if (wordRegion == null)
            return null;
        String wordAtPosition;
        try
        {
            wordAtPosition = document.get(wordRegion.getOffset(), wordRegion.getLength());
        }
        catch (BadLocationException e)
        {
            Activator.logError(e);
            return null;
        }
        Position position;
        try
        {
            position = DocumentUtil.toPosition(document, selection.getOffset());
        }
        catch (BadLocationException e)
        {
            Activator.logError(e);
            return null;
        }
        LanguageOperationTarget target = getLanguageOperationTarget(editor);
        if (target == null)
            return null;
        String fileName = new Path(target.getDocumentUri().getPath()).lastSegment();
        int column = getColumn(editor, document, wordRegion.getOffset());
        String resultLabelPattern =
            MessageFormat.format(Messages.AbstractFindReferencesHandler_Result_label_pattern,
                wordAtPosition, fileName, position.getLine() + 1, column + 1);
        return createSearchQuery(target, position, resultLabelPattern);
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
     * Returns the {@link DocumentService} for this handler.
     *
     * @return the document service (not <code>null</code>)
     */
    protected abstract DocumentService getDocumentService();

    /**
     * Creates and returns a {@link ReferenceSearchQuery} for the given parameters.
     *
     * @param target the {@link LanguageOperationTarget} for the search query
     *  (never <code>null</code>)
     * @param position the target text document position (never <code>null</code>)
     * @param resultLabelPattern a MessageFormat pattern for the search result label
     *  (may be <code>null</code> or empty)
     * @return the created search query (not <code>null</code>)
     */
    protected ReferenceSearchQuery createSearchQuery(LanguageOperationTarget target,
        Position position, String resultLabelPattern)
    {
        return new ReferenceSearchQuery(target, position, getDocumentService(), true)
        {
            @Override
            public String getResultLabel(int nMatches)
            {
                return resultLabelPattern == null || resultLabelPattern.isEmpty()
                    ? super.getResultLabel(nMatches)
                    : MessageFormat.format(resultLabelPattern, super.getResultLabel(nMatches));
            }
        };
    }

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
     * Returns the region of the word enclosing the given document offset.
     *
     * @param document never <code>null</code>
     * @param offset 0-based
     * @return the corresponding word region, or <code>null</code> if none
     */
    protected IRegion findWord(IDocument document, int offset)
    {
        return DefaultWordFinder.INSTANCE.findWord(document, offset);
    }

    private int getColumn(ITextEditor editor, IDocument document, int offset)
    {
        int column = -1;
        Integer tabWidth = getEditorHelper().getTabWidth(editor);
        if (tabWidth != null)
        {
            try
            {
                Position position = DocumentUtil.toPosition(document, offset);
                column = DocumentUtil.getColumn(document, position, tabWidth);
            }
            catch (BadLocationException e)
            {
                Activator.logError(e);
            }
        }
        return column;
    }
}
