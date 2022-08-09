/*******************************************************************************
 * Copyright (c) 2000, 2022 IBM Corporation and others.
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Vladimir Piskarev (1C) - adaptation
 *     (adapted from org.eclipse.jdt.internal.ui.text.JavaWordFinder
 *               and org.eclipse.jface.text.DefaultTextHover.findWord)
 *******************************************************************************/
package org.lxtk.lx4e.util;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

/**
 * Finds the <i>word</i> at a given document offset.
 * <p>
 * This class provides a default behavior that searches for <i>Unicode identifiers</i>.
 * Clients can use the {@link DefaultWordFinder#INSTANCE default} instance of the
 * word finder or may subclass this class if they need to specialize the default
 * behavior.
 * </p>
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public class WordFinder
{
    /**
     * Constructor.
     */
    protected WordFinder()
    {
    }

    /**
     * Returns the region of the word enclosing the given document offset.
     *
     * @param document not <code>null</code>
     * @param offset 0-based
     * @return the corresponding word region, or <code>null</code> if none
     */
    public IRegion findWord(IDocument document, int offset)
    {
        return findWord(document, offset, false);
    }

    /**
     * Returns the region of the word enclosing the given document offset,
     * using the given search mode.
     *
     * @param document not <code>null</code>
     * @param offset 0-based
     * @param strict the search mode:
     *  <ul>
     *  <li>in strict mode, the offset must fall strictly within the word region;</li>
     *  <li>in non-strict mode, the offset may immediately follow the word region</li>
     *  </ul>
     * @return the corresponding word region, or <code>null</code> if none
     */
    public IRegion findWord(IDocument document, int offset, boolean strict)
    {
        int start;
        int end;

        try
        {
            int length = document.getLength();

            if (offset > 0 && offset < length && Character.isLowSurrogate(document.getChar(offset))
                && Character.isHighSurrogate(document.getChar(offset - 1)))
                --offset; // normalize offset so that it points to the beginning of the surrogate pair

            if (!strict && offset > 0
                && (offset == length || !isWordPart(codePointAt(document, offset))))
                offset -= Character.charCount(codePointBefore(document, offset));

            int pos = offset;
            do
            {
                int c = codePointAt(document, pos);
                if (!isWordPart(c))
                    break;
                pos += Character.charCount(c);
            }
            while (pos < length);

            if (pos == offset)
                return null;

            end = pos;

            pos = offset;
            while (pos > 0)
            {
                int c = codePointBefore(document, pos);
                if (!isWordPart(c))
                    break;
                pos -= Character.charCount(c);
            }

            start = pos;
        }
        catch (BadLocationException e)
        {
            return null;
        }

        return new Region(start, end - start);
    }

    /**
     * Determines whether the given character may be part of a word.
     *
     * @param c a character (Unicode code point)
     * @return <code>true</code> if the given character may be part of a word,
     *  and <code>false</code> otherwise
     */
    protected boolean isWordPart(int c)
    {
        return Character.isUnicodeIdentifierPart(c);
    }

    private static int codePointAt(IDocument document, int offset) throws BadLocationException
    {
        char ch = document.getChar(offset);
        if (Character.isHighSurrogate(ch) && offset < document.getLength() - 1)
        {
            char ch2 = document.getChar(offset + 1);
            if (Character.isLowSurrogate(ch2))
                return Character.toCodePoint(ch, ch2);
        }
        return ch;
    }

    private static int codePointBefore(IDocument document, int offset) throws BadLocationException
    {
        char ch = document.getChar(offset - 1);
        if (Character.isLowSurrogate(ch) && offset > 1)
        {
            char ch2 = document.getChar(offset - 2);
            if (Character.isHighSurrogate(ch2))
                return Character.toCodePoint(ch2, ch);
        }
        return ch;
    }
}
