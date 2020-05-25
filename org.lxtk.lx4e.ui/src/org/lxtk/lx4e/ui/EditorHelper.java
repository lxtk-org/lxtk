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
 * Provides utility methods related to editors.
 * <p>
 * The implementations of the methods in this class strive to provide a
 * reasonable default behavior and work fine for most cases. Clients can use
 * the {@link DefaultEditorHelper#INSTANCE default} instance of the editor
 * helper or may subclass this class if they need to specialize the default
 * behavior.
 * </p>
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public class EditorHelper
{
    /**
     * Constructor.
     */
    protected EditorHelper()
    {
    }

    /**
     * Opens an editor for the given URI.
     * <p>
     * If the given page already has an open editor for the given URI,
     * that editor is brought to front; otherwise, a new editor is opened.
     * </p>
     *
     * @param page not <code>null</code>
     * @param uri not <code>null</code>
     * @return an open editor or <code>null</code> if an external editor
     *  was opened
     * @throws PartInitException if the editor could not be initialized
     */
    public IEditorPart openEditor(IWorkbenchPage page, URI uri) throws PartInitException
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
     * Selects the given text range in the given editor on a best effort basis.
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
     * Returns the text range selected in the given editor.
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
     * Returns the current selection in the given text editor.
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
     * Returns the {@link IDocument} for the given text editor.
     *
     * @param editor may be <code>null</code>
     * @return the editor's document, or <code>null</code> if none
     */
    public IDocument getDocument(ITextEditor editor)
    {
        if (editor == null)
            return null;
        return editor.getDocumentProvider().getDocument(editor.getEditorInput());
    }

    /**
     * Returns a text editor corresponding to the given object.
     * <p>
     * Default implementation uses the Platform's adapter mechanism.
     * </p>
     *
     * @param context may be <code>null</code>
     * @return the corresponding text editor, or <code>null</code> if none
     */
    public ITextEditor getTextEditor(Object context)
    {
        return Adapters.adapt(context, ITextEditor.class);
    }
}
