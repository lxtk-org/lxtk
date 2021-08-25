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
package org.lxtk.lx4e.ui.completion;

import static org.lxtk.util.completion.snippet.StandardVariableNames.TM_CURRENT_LINE;
import static org.lxtk.util.completion.snippet.StandardVariableNames.TM_CURRENT_WORD;
import static org.lxtk.util.completion.snippet.StandardVariableNames.TM_DIRECTORY;
import static org.lxtk.util.completion.snippet.StandardVariableNames.TM_FILENAME;
import static org.lxtk.util.completion.snippet.StandardVariableNames.TM_FILENAME_BASE;
import static org.lxtk.util.completion.snippet.StandardVariableNames.TM_FILEPATH;
import static org.lxtk.util.completion.snippet.StandardVariableNames.TM_LINE_INDEX;
import static org.lxtk.util.completion.snippet.StandardVariableNames.TM_LINE_NUMBER;
import static org.lxtk.util.completion.snippet.StandardVariableNames.TM_SELECTED_TEXT;

import java.net.URI;

import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.swt.graphics.Point;
import org.lxtk.CompletionProvider;
import org.lxtk.lx4e.util.DefaultWordFinder;

/**
 * A shared context for completion proposals.
 */
public class CompletionContext
{
    private ITextViewer textViewer;
    private URI documentUri;
    private int invocationOffset;
    private CompletionProvider completionProvider;
    private IContentAssistProcessor contentAssistProcessor;

    /**
     * Returns the text viewer.
     *
     * @return the text viewer (never <code>null</code>)
     */
    public final ITextViewer getTextViewer()
    {
        if (textViewer == null)
            throw new IllegalStateException();
        return textViewer;
    }

    /**
     * Returns the document.
     *
     * @return the document (never <code>null</code>)
     */
    public final IDocument getDocument()
    {
        return getTextViewer().getDocument();
    }

    /**
     * Returns the document URI.
     *
     * @return the document URI (never <code>null</code>)
     */
    public final URI getDocumentUri()
    {
        if (documentUri == null)
            throw new IllegalStateException();
        return documentUri;
    }

    /**
     * Returns the invocation offset.
     *
     * @return the invocation offset (zero-based)
     */
    public final int getInvocationOffset()
    {
        return invocationOffset;
    }

    /**
     * Returns the current word region.
     *
     * @return the current word region (never <code>null</code>, may be empty)
     */
    public IRegion getCurrentWordRegion()
    {
        IRegion wordRegion = DefaultWordFinder.INSTANCE.findWord(getDocument(), invocationOffset);
        if (wordRegion == null)
            return new Region(invocationOffset, 0);
        return wordRegion;
    }

    /**
     * Return the completion provider.
     *
     * @return the completion provider (never <code>null</code>)
     */
    public final CompletionProvider getCompletionProvider()
    {
        if (completionProvider == null)
            throw new IllegalStateException();
        return completionProvider;
    }

    /**
     * Returns whether the overwrite mode is on.
     *
     * @return <code>true</code> if the overwrite mode is on,
     *  and <code>false</code> if the overwrite mode is off
     */
    public boolean isOverwriteMode()
    {
        return false;
    }

    /**
     * Returns the value of the given variable set in this context.
     *
     * @param name the variable name (not <code>null</code>)
     * @return the value of the variable, or <code>null</code> if the variable is not set
     */
    public String resolveVariable(String name)
    {
        switch (name)
        {
        case TM_SELECTED_TEXT:
            return getSelectedText();
        case TM_CURRENT_LINE:
            return getCurrentLine();
        case TM_CURRENT_WORD:
            return getCurrentWord();
        case TM_LINE_INDEX:
            return getLineIndex();
        case TM_LINE_NUMBER:
            return getLineNumber();
        case TM_FILENAME:
            return getFileName();
        case TM_FILENAME_BASE:
            return getFileNameBase();
        case TM_DIRECTORY:
            return getDirectory();
        case TM_FILEPATH:
            return getFilePath();
        default:
            return null;
        }
    }

    void setTextViewer(ITextViewer textViewer)
    {
        this.textViewer = textViewer;
    }

    void setDocumentUri(URI documentUri)
    {
        this.documentUri = documentUri;
    }

    void setInvocationOffset(int invocationOffset)
    {
        this.invocationOffset = invocationOffset;
    }

    void setCompletionProvider(CompletionProvider completionProvider)
    {
        this.completionProvider = completionProvider;
    }

    void setContentAssistProcessor(IContentAssistProcessor contentAssistProcessor)
    {
        this.contentAssistProcessor = contentAssistProcessor;
    }

    IContentAssistProcessor getContentAssistProcessor()
    {
        if (contentAssistProcessor == null)
            throw new IllegalStateException();
        return contentAssistProcessor;
    }

    private String getSelectedText()
    {
        Point selection = getTextViewer().getSelectedRange();
        if (selection.y == 0)
            return null;
        try
        {
            return getDocument().get(selection.x, selection.y);
        }
        catch (BadLocationException e)
        {
            return null;
        }
    }

    private String getCurrentLine()
    {
        Point selection = getTextViewer().getSelectedRange();
        IDocument document = getDocument();
        try
        {
            IRegion region = document.getLineInformationOfOffset(selection.x);
            return document.get(region.getOffset(), region.getLength());
        }
        catch (BadLocationException e)
        {
            return null;
        }
    }

    private String getCurrentWord()
    {
        IRegion region = getCurrentWordRegion();
        if (region.getLength() == 0)
            return null;
        try
        {
            return getDocument().get(region.getOffset(), region.getLength());
        }
        catch (BadLocationException e)
        {
            return null;
        }
    }

    private String getLineIndex()
    {
        Point selection = getTextViewer().getSelectedRange();
        try
        {
            return String.valueOf(getDocument().getLineOfOffset(selection.x));
        }
        catch (BadLocationException e)
        {
            return null;
        }
    }

    private String getLineNumber()
    {
        Point selection = getTextViewer().getSelectedRange();
        try
        {
            return String.valueOf(getDocument().getLineOfOffset(selection.x) + 1);
        }
        catch (BadLocationException e)
        {
            return null;
        }
    }

    private String getFileName()
    {
        String path = getDocumentUri().getPath();
        if (path == null)
            return null;
        return new Path(path).lastSegment();
    }

    private String getFileNameBase()
    {
        String path = getDocumentUri().getPath();
        if (path == null)
            return null;
        return new Path(path).removeFileExtension().lastSegment();
    }

    private String getDirectory()
    {
        String path = getDocumentUri().getPath();
        if (path == null)
            return null;
        return new Path(path).removeLastSegments(1).toOSString();
    }

    private String getFilePath()
    {
        return getDocumentUri().getPath();
    }
}
