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
 * Provides static utility methods that bridge the gap between Eclipse documents
 * and document-related structures of LSP.
 */
public class DocumentUtil
{
    /**
     * Returns the LSP position corresponding to the given document offset.
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
     * Returns the document offset corresponding to the given LSP position.
     *
     * @param document not <code>null</code>
     * @param position an LSP position in the document (not <code>null</code>)
     * @return the corresponding offset (zero-based)
     * @throws BadLocationException if the the specified position is invalid
     *  in the document
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
     * Returns the LSP range corresponding to the given document offset and length.
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
     * Returns the document region corresponding to the given LSP range.
     *
     * @param document not <code>null</code>
     * @param range an LSP range in the document (not <code>null</code>)
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
     * Applies the given LSP text edit to the given document.
     *
     * @param document not <code>null</code>
     * @param edit an LSP text edit to apply to the document
     *  (not <code>null</code>)
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
     * Applies the given sequence of LSP text edits to the given document
     * as a single document modification.
     *
     * @param document not <code>null</code>
     * @param edits LSP text edits to apply to the document (not <code>null</code>,
     *  may be empty, must not contain <code>null</code>s)
     * @throws MalformedTreeException if an edit overlaps with one of its siblings
     * @throws BadLocationException if an edit's range is invalid in the document
     */
    public static void applyEdits(IDocument document, List<TextEdit> edits)
        throws MalformedTreeException, BadLocationException
    {
        if (edits.isEmpty())
            return;

        MultiTextEdit edit = toMultiTextEdit(document, edits);

        RewriteSessionEditProcessor editProcessor =
            new RewriteSessionEditProcessor(document, edit, 0);
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

    /**
     * Returns the multi-text edit corresponding to the given sequence
     * of LSP text edits for the given document.
     *
     * @param document not <code>null</code>
     * @param edits LSP text edits for the document (not <code>null</code>,
     *  may be empty, must not contain <code>null</code>s)
     * @return the created {@link MultiTextEdit} (never <code>null</code>)
     * @throws MalformedTreeException if an edit overlaps with one of its siblings
     * @throws BadLocationException if an edit's range is invalid in the document
     */
    public static MultiTextEdit toMultiTextEdit(IDocument document,
        List<TextEdit> edits) throws MalformedTreeException,
        BadLocationException
    {
        MultiTextEdit result = new MultiTextEdit();
        for (TextEdit edit : edits)
        {
            IRegion r = toRegion(document, edit.getRange());
            result.addChild(new ReplaceEdit(r.getOffset(), r.getLength(),
                edit.getNewText()));
        }
        return result;
    }

    private DocumentUtil()
    {
    }
}
