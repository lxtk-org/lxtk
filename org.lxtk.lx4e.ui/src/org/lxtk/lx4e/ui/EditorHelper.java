/*******************************************************************************
 * Copyright (c) 2019 1C-Soft LLC.
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
package org.lxtk.lx4e.ui;

import java.net.URI;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.lsp4j.Range;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.ITextEditor;
import org.lxtk.lx4e.DocumentUtil;

/**
 * TODO JavaDoc
 */
public class EditorHelper
{
    /**
     * TODO JavaDoc
     */
    protected EditorHelper()
    {
    }

    /**
     * TODO JavaDoc
     *
     * @param page not <code>null</code>
     * @param uri not <code>null</code>
     * @param activate
     * @return an open editor or <code>null</code> if an external editor
     *  was opened
     * @throws PartInitException
     */
    public IEditorPart openEditor(IWorkbenchPage page, URI uri,
        boolean activate) throws PartInitException
    {
        if (page == null)
            throw new IllegalArgumentException();
        if (uri == null)
            throw new IllegalArgumentException();

        IFileStore fileStore;
        try
        {
            fileStore = EFS.getStore(uri);
        }
        catch (CoreException e)
        {
            throw new PartInitException(e.getMessage(), e);
        }
        return IDE.openEditorOnFileStore(page, fileStore);
    }

    /**
     * TODO JavaDoc
     *
     * @param editor not <code>null</code>
     * @param range not <code>null</code>
     */
    public void selectTextRange(IEditorPart editor, Range range)
    {
        if (editor == null)
            throw new IllegalArgumentException();
        if (range == null)
            throw new IllegalArgumentException();

        ITextEditor textEditor = getTextEditor(editor);
        if (textEditor != null)
        {
            IDocument document = getDocument(textEditor);
            if (document != null)
            {
                IRegion r;
                try
                {
                    r = DocumentUtil.toRegion(document, range);
                }
                catch (BadLocationException e)
                {
                    return;
                }
                textEditor.selectAndReveal(r.getOffset(), r.getLength());
            }
        }
    }

    /**
     * TODO JavaDoc
     *
     * @param editor may be <code>null</code>
     * @return the selected text range, or <code>null</code> if none
     */
    public Range getSelectedTextRange(IEditorPart editor)
    {
        ITextEditor textEditor = getTextEditor(editor);
        if (textEditor == null)
            return null;

        ITextSelection s = getTextSelection(textEditor);
        if (s == null)
            return null;

        IDocument document = getDocument(textEditor);
        if (document == null)
            return null;

        try
        {
            return DocumentUtil.toRange(document, s.getOffset(), s.getLength());
        }
        catch (BadLocationException e)
        {
            return null;
        }
    }

    /**
     * TODO JavaDoc
     *
     * @param editor may be <code>null</code>
     * @return the current text selection, or <code>null</code> if none or empty
     */
    public ITextSelection getTextSelection(ITextEditor editor)
    {
        if (editor == null)
            return null;

        ISelectionProvider selectionProvider = editor.getSelectionProvider();
        if (selectionProvider == null)
            return null;

        ISelection selection = selectionProvider.getSelection();
        if (!(selection instanceof ITextSelection))
            return null;

        ITextSelection textSelection = (ITextSelection)selection;
        int offset = textSelection.getOffset();
        int length = textSelection.getLength();
        if (offset < 0 || length < 0)
            return null;

        return textSelection;
    }

    /**
     * TODO JavaDoc
     *
     * @param editor may be <code>null</code>
     * @return the edited document, or <code>null</code> if none
     */
    public IDocument getDocument(ITextEditor editor)
    {
        if (editor == null)
            return null;
        return editor.getDocumentProvider().getDocument(
            editor.getEditorInput());
    }

    /**
     * TODO JavaDoc
     *
     * @param context may be <code>null</code>
     * @return the corresponding text editor, or <code>null</code> if none
     */
    public ITextEditor getTextEditor(Object context)
    {
        return Adapters.adapt(context, ITextEditor.class);
    }
}
