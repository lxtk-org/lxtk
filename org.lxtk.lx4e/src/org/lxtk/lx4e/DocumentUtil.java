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
package org.lxtk.lx4e;

import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.RewriteSessionEditProcessor;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.undo.DocumentUndoManagerRegistry;
import org.eclipse.text.undo.IDocumentUndoManager;

/**
 * TODO JavaDoc
 */
public class DocumentUtil
{
    /**
     * TODO JavaDoc
     *
     * @param document not <code>null</code>
     * @param offset a zero-based offset in the document
     * @return the corresponding {@link Position} (never <code>null</code>)
     * @throws BadLocationException if the specified offset is invalid
     *  in the document
     */
    public static Position toPosition(IDocument document, int offset)
        throws BadLocationException
    {
        int line = document.getLineOfOffset(offset);
        int column = offset - document.getLineInformationOfOffset(
            offset).getOffset();
        return new Position(line, column);
    }

    /**
     * TODO JavaDoc
     *
     * @param document not <code>null</code>
     * @param position a position in the document (not <code>null</code>)
     * @return the corresponding offset (zero-based)
     * @throws BadLocationException if the line number of the specified position
     *  is invalid in the document
     */
    public static int toOffset(IDocument document, Position position)
        throws BadLocationException
    {
        IRegion line = document.getLineInformation(position.getLine());
        int offsetInLine = position.getCharacter();
        if (offsetInLine > line.getLength())
            offsetInLine = line.getLength();
        return line.getOffset() + offsetInLine;
    }

    /**
     * TODO JavaDoc
     *
     * @param document not <code>null</code>
     * @param offset the offset of the range
     * @param length the length of the range
     * @return the corresponding {@link Range} (never <code>null</code>)
     * @throws BadLocationException if the specified range is invalid
     *  in the document
     */
    public static Range toRange(IDocument document, int offset, int length)
        throws BadLocationException
    {
        Position start = toPosition(document, offset);
        Position end = toPosition(document, offset + length);
        return new Range(start, end);
    }

    /**
     * TODO JavaDoc
     *
     * @param document not <code>null</code>
     * @param range a range in the document (not <code>null</code>)
     * @return the corresponding {@link IRegion} (never <code>null</code>)
     * @throws BadLocationException if the specified range is invalid
     *  in the document
     */
    public static IRegion toRegion(IDocument document, Range range)
        throws BadLocationException
    {
        int offset = toOffset(document, range.getStart());
        int length = toOffset(document, range.getEnd()) - offset;
        return new Region(offset, length);
    }

    /**
     * TODO JavaDoc
     *
     * @param document not <code>null</code>
     * @param edit a text edit to apply to the document (not <code>null</code>)
     * @throws BadLocationException if the specified edit range is invalid
     *  in the document
     */
    public static void applyEdit(IDocument document, TextEdit edit)
        throws BadLocationException
    {
        IRegion r = toRegion(document, edit.getRange());
        document.replace(r.getOffset(), r.getLength(), edit.getNewText());
    }

    /**
     * Applies the given sequence of text edits as a single document modification.
     *
     * @param document not <code>null</code>
     * @param edits text edits to apply to the document (not <code>null</code>,
     *  may be empty, must not contain <code>null</code>s)
     * @throws MalformedTreeException if an edit overlaps with one of its siblings
     * @throws BadLocationException if an edit's range is invalid in the document
     */
    public static void applyEdits(IDocument document, List<TextEdit> edits)
        throws MalformedTreeException, BadLocationException
    {
        if (edits.isEmpty())
            return;

        MultiTextEdit rootEdit = new MultiTextEdit();
        for (TextEdit edit : edits)
        {
            IRegion r = toRegion(document, edit.getRange());
            rootEdit.addChild(new ReplaceEdit(r.getOffset(), r.getLength(),
                edit.getNewText()));
        }

        RewriteSessionEditProcessor editProcessor =
            new RewriteSessionEditProcessor(document, rootEdit, 0);
        IDocumentUndoManager undoManager =
            DocumentUndoManagerRegistry.getDocumentUndoManager(document);
        if (undoManager != null)
            undoManager.beginCompoundChange();
        try
        {
            editProcessor.performEdits();
        }
        finally
        {
            if (undoManager != null)
                undoManager.endCompoundChange();
        }
    }

    private DocumentUtil()
    {
    }
}
