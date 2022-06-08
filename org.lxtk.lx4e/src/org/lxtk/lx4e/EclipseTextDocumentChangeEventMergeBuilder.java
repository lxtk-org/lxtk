/*******************************************************************************
 * Copyright (c) 2021, 2022 1C-Soft LLC.
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

import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.lxtk.TextDocumentChangeEvent;
import org.lxtk.TextDocumentChangeEventMergeBuilder;
import org.lxtk.TextDocumentSnapshot;

/**
 * LX4E-specific implementation of {@link TextDocumentChangeEventMergeBuilder}.
 */
public final class EclipseTextDocumentChangeEventMergeBuilder
    implements TextDocumentChangeEventMergeBuilder
{
    private final Document document;
    private TextDocumentSnapshot currentSnapshot;
    private DocumentEvent mergedEvent;

    /**
     * Constructor.
     *
     * @param base text before changes (not <code>null</code>)
     */
    public EclipseTextDocumentChangeEventMergeBuilder(String base)
    {
        document = new Document(Objects.requireNonNull(base));
    }

    @Override
    public TextDocumentChangeEventMergeBuilder merge(TextDocumentChangeEvent event)
    {
        if (!(event instanceof EclipseTextDocumentChangeEvent))
            throw new IllegalArgumentException();

        if (mergedEvent == null)
            mergedEvent = ((EclipseTextDocumentChangeEvent)event).originalEvent;
        else
        {
            try
            {
                mergedEvent = TextUtilities.mergeProcessedDocumentEvents(Arrays.asList(mergedEvent,
                    ((EclipseTextDocumentChangeEvent)event).originalEvent));
            }
            catch (BadLocationException e)
            {
                throw new IllegalArgumentException(e);
            }
        }
        currentSnapshot = event.getSnapshot();
        return this;
    }

    @Override
    public TextDocumentChangeEvent getResult()
    {
        if (mergedEvent == null)
            return null;

        Range range;
        try
        {
            range =
                DocumentUtil.toRange(document, mergedEvent.getOffset(), mergedEvent.getLength());
        }
        catch (BadLocationException e)
        {
            throw new AssertionError(e); // must never happen
        }

        return new EclipseTextDocumentChangeEvent(currentSnapshot, Collections.singletonList(
            new TextDocumentContentChangeEvent(range, mergedEvent.getText())), mergedEvent);
    }

    @Override
    public boolean hasResult()
    {
        return mergedEvent != null;
    }
}
