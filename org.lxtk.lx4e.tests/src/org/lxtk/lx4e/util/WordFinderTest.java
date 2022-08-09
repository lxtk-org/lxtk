/*******************************************************************************
 * Copyright (c) 2022 1C-Soft LLC.
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
package org.lxtk.lx4e.util;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Region;

import junit.framework.TestCase;

public class WordFinderTest
    extends TestCase
{
    public void test()
    {
        IDocument document = new Document();
        WordFinder wordFinder = DefaultWordFinder.INSTANCE;

        assertNull(wordFinder.findWord(document, 0));
        assertNull(wordFinder.findWord(document, -1));
        assertNull(wordFinder.findWord(document, 1));

        document.set("a");
        assertEquals(new Region(0, 1), wordFinder.findWord(document, 0));
        assertEquals(new Region(0, 1), wordFinder.findWord(document, 1));
        assertNull(wordFinder.findWord(document, 1, true));

        document.set(" a ");
        assertNull(wordFinder.findWord(document, 0));
        assertEquals(new Region(1, 1), wordFinder.findWord(document, 1));
        assertEquals(new Region(1, 1), wordFinder.findWord(document, 2));
        assertNull(wordFinder.findWord(document, 2, true));

        document.set("ab");
        assertEquals(new Region(0, 2), wordFinder.findWord(document, 0));
        assertEquals(new Region(0, 2), wordFinder.findWord(document, 1));
        assertEquals(new Region(0, 2), wordFinder.findWord(document, 2));
        assertNull(wordFinder.findWord(document, 2, true));

        document.set(" ab ");
        assertNull(wordFinder.findWord(document, 0));
        assertEquals(new Region(1, 2), wordFinder.findWord(document, 1));
        assertEquals(new Region(1, 2), wordFinder.findWord(document, 2));
        assertEquals(new Region(1, 2), wordFinder.findWord(document, 3));
        assertNull(wordFinder.findWord(document, 3, true));

        document.set("𐐀");
        assertEquals(new Region(0, 2), wordFinder.findWord(document, 0));
        assertEquals(new Region(0, 2), wordFinder.findWord(document, 1));
        assertEquals(new Region(0, 2), wordFinder.findWord(document, 2));
        assertNull(wordFinder.findWord(document, 2, true));

        document.set(" 𐐀 ");
        assertNull(wordFinder.findWord(document, 0));
        assertEquals(new Region(1, 2), wordFinder.findWord(document, 1));
        assertEquals(new Region(1, 2), wordFinder.findWord(document, 2));
        assertEquals(new Region(1, 2), wordFinder.findWord(document, 3));
        assertNull(wordFinder.findWord(document, 3, true));

        document.set("a𐐀");
        assertEquals(new Region(0, 3), wordFinder.findWord(document, 0));
        assertEquals(new Region(0, 3), wordFinder.findWord(document, 1));
        assertEquals(new Region(0, 3), wordFinder.findWord(document, 2));
        assertEquals(new Region(0, 3), wordFinder.findWord(document, 3));
        assertNull(wordFinder.findWord(document, 3, true));

        document.set(" a𐐀 ");
        assertNull(wordFinder.findWord(document, 0));
        assertEquals(new Region(1, 3), wordFinder.findWord(document, 1));
        assertEquals(new Region(1, 3), wordFinder.findWord(document, 2));
        assertEquals(new Region(1, 3), wordFinder.findWord(document, 3));
        assertEquals(new Region(1, 3), wordFinder.findWord(document, 4));
        assertNull(wordFinder.findWord(document, 4, true));

        document.set("𐐀b");
        assertEquals(new Region(0, 3), wordFinder.findWord(document, 0));
        assertEquals(new Region(0, 3), wordFinder.findWord(document, 1));
        assertEquals(new Region(0, 3), wordFinder.findWord(document, 2));
        assertEquals(new Region(0, 3), wordFinder.findWord(document, 3));
        assertNull(wordFinder.findWord(document, 3, true));

        document.set(" 𐐀b ");
        assertNull(wordFinder.findWord(document, 0));
        assertEquals(new Region(1, 3), wordFinder.findWord(document, 1));
        assertEquals(new Region(1, 3), wordFinder.findWord(document, 2));
        assertEquals(new Region(1, 3), wordFinder.findWord(document, 3));
        assertEquals(new Region(1, 3), wordFinder.findWord(document, 4));
        assertNull(wordFinder.findWord(document, 4, true));

        document.set("a𐐀b");
        assertEquals(new Region(0, 4), wordFinder.findWord(document, 0));
        assertEquals(new Region(0, 4), wordFinder.findWord(document, 1));
        assertEquals(new Region(0, 4), wordFinder.findWord(document, 2));
        assertEquals(new Region(0, 4), wordFinder.findWord(document, 3));
        assertEquals(new Region(0, 4), wordFinder.findWord(document, 4));
        assertNull(wordFinder.findWord(document, 4, true));

        document.set(" a𐐀b ");
        assertNull(wordFinder.findWord(document, 0));
        assertEquals(new Region(1, 4), wordFinder.findWord(document, 1));
        assertEquals(new Region(1, 4), wordFinder.findWord(document, 2));
        assertEquals(new Region(1, 4), wordFinder.findWord(document, 3));
        assertEquals(new Region(1, 4), wordFinder.findWord(document, 4));
        assertEquals(new Region(1, 4), wordFinder.findWord(document, 5));
        assertNull(wordFinder.findWord(document, 5, true));
    }
}
