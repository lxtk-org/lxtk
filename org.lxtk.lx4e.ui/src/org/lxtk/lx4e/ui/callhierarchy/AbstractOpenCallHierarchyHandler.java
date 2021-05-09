/*******************************************************************************
 * Copyright (c) 2021 1C-Soft LLC.
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
package org.lxtk.lx4e.ui.callhierarchy;

import java.time.Duration;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.handly.ui.callhierarchy.CallHierarchyViewOpener;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.lsp4j.CallHierarchyItem;
import org.eclipse.lsp4j.CallHierarchyPrepareParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.texteditor.ITextEditor;
import org.lxtk.CallHierarchyProvider;
import org.lxtk.DocumentUri;
import org.lxtk.LanguageOperationTarget;
import org.lxtk.LanguageService;
import org.lxtk.lx4e.DocumentUtil;
import org.lxtk.lx4e.internal.ui.Activator;
import org.lxtk.lx4e.requests.PrepareCallHierarchyRequest;
import org.lxtk.lx4e.ui.DefaultEditorHelper;
import org.lxtk.lx4e.ui.EditorHelper;
import org.lxtk.lx4e.ui.WorkDoneProgressFactory;

/**
 * Partial implementation of a handler that opens LXTK-based call hierarchy for the current
 * text selection in the active editor.
 */
public abstract class AbstractOpenCallHierarchyHandler
    extends AbstractHandler
{
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        EditorHelper editorHelper = getEditorHelper();

        ITextEditor editor = editorHelper.getTextEditor(HandlerUtil.getActiveEditor(event));
        if (editor == null)
            return null;

        IWorkbenchPage page = editor.getSite().getWorkbenchWindow().getActivePage();
        if (page == null)
            return null;

        ITextSelection selection = editorHelper.getTextSelection(editor);
        if (selection == null)
            return null;

        IDocument document = editorHelper.getDocument(editor);
        if (document == null)
            return null;

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

        LanguageService languageService = target.getLanguageService();
        CallHierarchyProvider provider = languageService.getDocumentMatcher().getBestMatch(
            languageService.getCallHierarchyProviders(), CallHierarchyProvider::getDocumentSelector,
            target.getDocumentUri(), target.getLanguageId());
        if (provider == null)
            return null;

        CallHierarchyPrepareParams params = new CallHierarchyPrepareParams();
        params.setTextDocument(DocumentUri.toTextDocumentIdentifier(target.getDocumentUri()));
        params.setPosition(position);

        PrepareCallHierarchyRequest request = newPrepareCallHierarchyRequest();
        request.setProvider(provider);
        request.setParams(params);
        request.setTimeout(getPrepareCallHierarchyTimeout());
        request.setMayThrow(false);
        request.setUpWorkDoneProgress(
            () -> WorkDoneProgressFactory.newWorkDoneProgressWithJob(true));

        List<CallHierarchyItem> items = request.sendAndReceive();
        if (items == null || items.isEmpty())
            return null;

        int size = items.size();
        CallHierarchyElement[] inputElements = new CallHierarchyElement[size];
        CallHierarchyUtility utility = getCallHierarchyUtility(provider);
        for (int i = 0; i < size; i++)
        {
            inputElements[i] = new CallHierarchyElement(items.get(i), utility);
        }
        try
        {
            getCallHierarchyViewOpener().openView(page, inputElements);
        }
        catch (PartInitException e)
        {
            Activator.logError(e);
        }
        return null;
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
     * Returns the call hierarchy utility for this handler.
     *
     * @param provider a {@link CallHierarchyProvider} (not <code>null</code>)
     * @return the call hierarchy utility (not <code>null</code>)
     */
    protected abstract CallHierarchyUtility getCallHierarchyUtility(CallHierarchyProvider provider);

    /**
     * Returns the call hierarchy view opener for this handler.
     *
     * @return the call hierarchy view opener (not <code>null</code>)
     */
    protected abstract CallHierarchyViewOpener getCallHierarchyViewOpener();

    /**
     * Returns the timeout for a prepare call hierarchy request.
     *
     * @return a positive duration
     */
    protected Duration getPrepareCallHierarchyTimeout()
    {
        return Duration.ofSeconds(2);
    }

    /**
     * Returns a new instance of {@link PrepareCallHierarchyRequest}.
     *
     * @return the created request object (not <code>null</code>)
     */
    protected PrepareCallHierarchyRequest newPrepareCallHierarchyRequest()
    {
        return new PrepareCallHierarchyRequest();
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
}
