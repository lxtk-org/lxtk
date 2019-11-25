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

        ITextEditor textEditor = Adapters.adapt(editor, ITextEditor.class);
        if (textEditor != null)
        {
            IDocument document = textEditor.getDocumentProvider().getDocument(
                textEditor.getEditorInput());
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
}
