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
package org.lxtk.util.completion.snippet;

import junit.framework.TestCase;

public class SnippetParserTest
    extends TestCase
{
    public void testText() throws Exception
    {
        assertEquals(new Snippet(""), parse(""));
        assertEquals(new Snippet("a"), parse("a"));
        assertEquals(new Snippet("abc"), parse("abc"));
        assertEquals(new Snippet("1"), parse("1"));
        assertEquals(new Snippet("123"), parse("123"));
        assertEquals(new Snippet("$"), parse("$"));
        assertEquals(new Snippet("$$$"), parse("$$$"));
        assertEquals(new Snippet("\\"), parse("\\"));
        assertEquals(new Snippet("\\"), parse("\\\\"));
        assertEquals(new Snippet("\\a"), parse("\\a"));
        assertEquals(new Snippet("\\1"), parse("\\1"));
        assertEquals(new Snippet("\\,"), parse("\\,"));
        assertEquals(new Snippet("\\|"), parse("\\|"));
        assertEquals(new Snippet("$"), parse("\\$"));
        assertEquals(new Snippet("}"), parse("\\}"));
        assertEquals(new Snippet("$1"), parse("\\$1"));
        assertEquals(new Snippet("\\t\\\\}\\}${1\\|\\,,\\\\,},\\},$,\\$,\\\\$,\\||}"),
            parse("\\t\\\\\\\\}\\\\}${1\\\\|\\\\,,\\\\\\\\,},\\\\},$,\\\\$,\\\\\\\\$,\\\\||}"));
    }

    public void testTabStop() throws Exception
    {
        assertEquals(new Snippet("", new TabStop("1", new int[] { 0 })), parse("$1"));
        assertEquals(new Snippet("", new TabStop("1", new int[] { 0 })), parse("${1}"));
        assertEquals(new Snippet("${1"), parse("${1"));
        assertEquals(new Snippet("${1", new TabStop("1", new int[] { 3 })), parse("${1$1"));
        assertEquals(new Snippet("", new TabStop("0", new int[] { 0 }),
            new TabStop("1", new int[] { 0 }), new TabStop("2", new int[] { 0 })), parse("$0$1$2"));
        assertEquals(
            new Snippet("", new TabStop("2", new int[] { 0 }), new TabStop("11", new int[] { 0 })),
            parse("$11$002"));
        assertEquals(
            new Snippet("12", new TabStop("0", new int[] { 1 }), new TabStop("1", new int[] { 0 })),
            parse("${1}1${00}2"));
    }

    public void testPlaceholder() throws Exception
    {
        assertEquals(new Snippet("a", new TabStop("1", new int[] { 0 }, "a")), parse("${1:a}"));
        assertEquals(new Snippet("${1:a"), parse("${1:a"));
        assertEquals(new Snippet("${1:a", new TabStop("1", new int[] { 5 })), parse("${1:a$1"));
        assertEquals(new Snippet("a a", new TabStop("1", new int[] { 0, 2 }, "a")),
            parse("${1:a} $1"));
        assertEquals(new Snippet("a a", new TabStop("1", new int[] { 2, 0 }, "a"),
            new TabStop("2", new int[] { 2, 0 }, "a")), parse("$1 ${1:${2:a}}"));
        assertEquals(new Snippet("a a a", new TabStop("1", new int[] { 2, 0, 4 }, "a")),
            parse("$1 ${1:a} ${1:b}"));
        assertEquals(new Snippet("a a a", new TabStop("1", new int[] { 2, 0, 4 }, "a"),
            new TabStop("2", new int[] { 2 }, "a")), parse("$1 ${2:${1:a}} ${1:b}"));
        assertEquals(new Snippet("a a a", new TabStop("1", new int[] { 0, 2, 4 }, "a"),
            new TabStop("2", new int[] { 4 }, "a")), parse("${1:a} ${1:$2} ${2:$1}"));
        assertEquals(new Snippet("a b", new TabStop("1", new int[] { 0 }, "a b"),
            new TabStop("2", new int[] { 2 }, "b")), parse("${1:a ${2:b}}"));
        assertEquals(new Snippet("${1:a b", new TabStop("2", new int[] { 6 }, "b")),
            parse("${1:a ${2:b}"));
        assertEquals(new Snippet("a b c", new TabStop("1", new int[] { 0 }, "a"),
            new TabStop("2", new int[] { 4 }, "c")), parse("${1:a} b ${2:c}"));
        assertEquals(new Snippet("a b a", new TabStop("1", new int[] { 4, 0 }, "a")),
            parse("$1 b ${1:a}"));
        assertEquals(new Snippet("a b a", new TabStop("1", new int[] { 0, 4 }, "a")),
            parse("${1:a} b ${1:c}"));
        assertEquals(new Snippet("a b a", new TabStop("1", new int[] { 0, 4 }, "a")),
            parse("${1:a} b ${1:${2:c}}"));
        assertEquals(new Snippet("a b a b", new TabStop("1", new int[] { 0, 4 }, "a b"),
            new TabStop("2", new int[] { 2, 6 }, "b")), parse("${1:a ${2:b}} $1"));
        assertEquals(new Snippet("a b a b", new TabStop("1", new int[] { 4, 0 }, "a b"),
            new TabStop("2", new int[] { 6, 2 }, "b")), parse("$1 ${1:a ${2:b}}"));
        assertEquals(new Snippet("a b c a b c", new TabStop("1", new int[] { 0, 6 }, "a b c"),
            new TabStop("2", new int[] { 2, 8 }, "b c"),
            new TabStop("3", new int[] { 4, 10 }, "c")), parse("${1:a ${2:b ${3:c}}} $1"));
        assertEquals(new Snippet("a b c a b c", new TabStop("1", new int[] { 6, 0 }, "a b c"),
            new TabStop("2", new int[] { 8, 2 }, "b c"),
            new TabStop("3", new int[] { 10, 4 }, "c")), parse("$1 ${1:a ${2:b ${3:c}}}"));
        assertEquals(new Snippet("a b c a b c b", new TabStop("1", new int[] { 6, 0 }, "a b c"),
            new TabStop("2", new int[] { 12, 2, 8 }, "b")), parse("$1 ${1:a $2 c} ${2:b}"));
        assertEquals(new Snippet("a b c a b c b", new TabStop("1", new int[] { 6, 0 }, "a b c"),
            new TabStop("2", new int[] { 12, 2, 8 }, "b"),
            new TabStop("3", new int[] { 8, 2 }, "b c")), parse("$1 ${1:a ${3:$2 c}} ${2:b}"));
        assertEquals(
            new Snippet("a b c a b c b", new TabStop("1", new int[] { 6, 0 }, "a b c"),
                new TabStop("2", new int[] { 12, 2, 8 }, "b"),
                new TabStop("3", new int[] { 12, 2, 8 }, "b")),
            parse("$1 ${1:a $2 c} ${2:${3:b}}"));
    }

    public void testChoice() throws Exception
    {
        assertEquals(new Snippet("", new TabStop("1", new int[] { 0 }, "")), parse("${1||}"));
        assertEquals(new Snippet("${1|}"), parse("${1|}"));
        assertEquals(new Snippet("${1|"), parse("${1|"));
        assertEquals(new Snippet("${1|||}"), parse("${1|||}"));
        assertEquals(new Snippet("a", new TabStop("1", new int[] { 0 }, "a")), parse("${1|a|}"));
        assertEquals(new Snippet("a", new TabStop("1", new int[] { 0 }, "a", "b")),
            parse("${1|a,b|}"));
        assertEquals(new Snippet("$1", new TabStop("1", new int[] { 0 }, "$1")), parse("${1|$1|}"));
        assertEquals(new Snippet("", new TabStop("1", new int[] { 0 }, "", "")), parse("${1|,|}"));
        assertEquals(new Snippet(",", new TabStop("1", new int[] { 0 }, ",")), parse("${1|\\,|}"));
        assertEquals(new Snippet("\\", new TabStop("1", new int[] { 0 }, "\\", "")),
            parse("${1|\\\\,|}"));
        assertEquals(new Snippet("|", new TabStop("1", new int[] { 0 }, "|")), parse("${1|\\||}"));
        assertEquals(new Snippet("}", new TabStop("1", new int[] { 0 }, "}")), parse("${1|}|}"));
        assertEquals(new Snippet("\\$", new TabStop("1", new int[] { 0 }, "\\$")),
            parse("${1|\\$|}"));
        assertEquals(new Snippet("\\}", new TabStop("1", new int[] { 0 }, "\\}")),
            parse("${1|\\}|}"));
    }

    public void testVariable() throws Exception
    {
        assertEquals(new Snippet("foo"), parse("$TM_FILENAME_BASE"));
        assertEquals(new Snippet("foo"), parse("${TM_FILENAME_BASE}"));
        assertEquals(new Snippet("foo"), parse("${TM_FILENAME_BASE:${1:bar}}"));
        assertEquals(new Snippet(""), parse("$TM_SELECTED_TEXT"));
        assertEquals(new Snippet("foo"), parse("${TM_SELECTED_TEXT:foo}"));
        assertEquals(new Snippet("foo foo", new TabStop("1", new int[] { 0, 4 }, "foo")),
            parse("${TM_SELECTED_TEXT:${1:foo} $1}"));
        assertEquals(
            new Snippet("foo bar foo bar", new TabStop("1", new int[] { 0, 8 }, "foo bar"),
                new TabStop("2", new int[] { 4, 12 }, "bar")),
            parse("${TM_SELECTED_TEXT:${1:foo ${2:bar}} $1}"));
        assertEquals(new Snippet("abc abc", new TabStop("abc", new int[] { 0, 4 }, "abc")),
            parse("$abc ${abc}"));
        assertEquals(new Snippet("abc abc", new TabStop("abc", new int[] { 0, 4 }, "abc")),
            parse("${abc} $abc"));
        assertEquals(new Snippet("foo foo", new TabStop("1", new int[] { 0, 4 }, "foo")),
            parse("${abc:${1:foo} $1}"));
        assertEquals(
            new Snippet("foo bar foo bar", new TabStop("1", new int[] { 0, 8 }, "foo bar"),
                new TabStop("2", new int[] { 4, 12 }, "bar")),
            parse("${abc:${1:foo ${2:bar}} $1}"));
    }

    public void testVariableTransform() throws Exception
    {
        assertEquals(new Snippet("foo"), parse("${TM_FILENAME_BASE///}"));
        assertEquals(new Snippet("${TM_FILENAME_BASE//}"), parse("${TM_FILENAME_BASE//}"));
        assertEquals(new Snippet("foo"), parse("${TM_SELECTED_TEXT//foo/}"));
        assertEquals(new Snippet("/foo"), parse("${TM_FILENAME_BASE//\\//}"));
        assertEquals(new Snippet("/f/o/o/"), parse("${TM_FILENAME_BASE//\\//g}"));
        assertEquals(new Snippet("${TM_FILENAME_BASE/// }"), parse("${TM_FILENAME_BASE/// }"));
        assertEquals(new Snippet(""), parse("${TM_FILENAME_BASE/.oo//}"));
        assertEquals(new Snippet("foo"), parse("${TM_FILENAME_BASE/bar//}"));
        assertEquals(new Snippet("foo"), parse("${TM_FILENAME_BASE/\\///}"));
        assertEquals(new Snippet("foo"), parse("${TM_FILENAME_BASE/.oo/$0/}"));
        assertEquals(new Snippet("$0"), parse("${TM_FILENAME_BASE/.oo/\\$0/}"));
        assertEquals(new Snippet("\\foo"), parse("${TM_FILENAME_BASE/.oo/\\\\$0/}"));
        assertEquals(new Snippet("foo bar"), parse("${TM_FILENAME_BASE/(.oo)/$1 bar/}"));
        assertEquals(new Snippet(" bar"), parse("${TM_FILENAME_BASE/.oo/$1 bar/}"));
        assertEquals(new Snippet("FOO"), parse("${TM_FILENAME_BASE/.oo/${0:/upcase}/}"));
        assertEquals(new Snippet("Foo"), parse("${TM_FILENAME_BASE/.oo/${0:/capitalize}/}"));
        assertEquals(new Snippet("foo"), parse("${TM_FILENAME_BASE/.oo/${0:/ignore_123}/}"));
        assertEquals(
            new Snippet("${TM_FILENAME_BASE/.oo//ignore 123/}",
                new TabStop("0", new int[] { 23 }, "/ignore 123")),
            parse("${TM_FILENAME_BASE/.oo/${0:/ignore 123}/}"));
        assertEquals(new Snippet("bar}"), parse("${TM_FILENAME_BASE/(foo)|(.*)/${1:+bar}}/}"));
        assertEquals(new Snippet("}"), parse("${TM_FILENAME_BASE/(abc)|(.*)/${1:+bar}}/}"));
        assertEquals(new Snippet(""), parse("${TM_FILENAME_BASE/(foo)|(.*)/${1:-bar\\}}/}"));
        assertEquals(new Snippet("bar}"), parse("${TM_FILENAME_BASE/(abc)|(.*)/${1:bar\\}}/}"));
        assertEquals(new Snippet("bar:}"),
            parse("${TM_FILENAME_BASE/(foo)|(.*)/${1:?bar\\:}:baz:\\}}/}"));
        assertEquals(new Snippet("baz:}"),
            parse("${TM_FILENAME_BASE/(abc)|(.*)/${1:?bar\\:}:baz:\\}}/}"));
        assertEquals(new Snippet("bar"), parse("${TM_FILENAME_BASE/foo/${1:-bar}/}"));
        assertEquals(new Snippet("foo"), parse("${TM_FILENAME_BASE/abc/${1:-bar}/}")); // same as in TextMate; it would have been "bar" in VSCode
        assertEquals(new Snippet(""),
            parse("${TM_SELECTED_TEXT/(\\w+)\\W*(.+)?/$1${2:?...:!!!}/}")); // same as in TextMate; it would have been "!!!" in VSCode
        assertEquals(new Snippet("FOO"),
            parse("${TM_FILENAME_BASE/(foo)|(FOO)/${1:?${1:/upcase}:${2:/downcase}}/}")); // same as in TextMate; VSCode does not currently support nested format strings
    }

    public void testComplex() throws Exception
    {
        assertEquals(
            new Snippet("$abc $abc", new TabStop("1", new int[] { 7, 2 }, "b"),
                new TabStop("2", new int[] { 6, 1 }, "a"),
                new TabStop("3", new int[] { 6, 1 }, "a", "b"),
                new TabStop("4", new int[] { 3, 8 }, "c")),
            parse("$${2}$1${4:c} \\$${2:${3|a,b|}}${1:b}$4"));
        assertEquals(
            new Snippet("xyz abc abc", new TabStop("0", new int[] { 4, 8 }),
                new TabStop("1", new int[] { 0 }), new TabStop("20", new int[] { 11 }),
                new TabStop("xyz", new int[] { 0 }, "xyz"),
                new TabStop("abc", new int[] { 4, 8 }, "abc")),
            parse("$001${xyz} $0$abc $00${abc}$20"));
    }

    public void testInvalid()
    {
        try
        {
            parse("${1:$1}");
            fail();
        }
        catch (SnippetException e)
        {
        }
        try
        {
            parse("${1:a $1}");
            fail();
        }
        catch (SnippetException e)
        {
        }
        try
        {
            parse("${1:${1:a}}");
            fail();
        }
        catch (SnippetException e)
        {
        }
        try
        {
            parse("${1:$2} ${2:$1}");
            fail();
        }
        catch (SnippetException e)
        {
        }
        try
        {
            parse("${1:$2} ${2:$1} ${1:a}");
            fail();
        }
        catch (SnippetException e)
        {
        }
    }

    private static Snippet parse(String source) throws SnippetException
    {
        return Snippet.parse(source,
            name -> StandardVariableNames.TM_FILENAME_BASE.equals(name) ? "foo" : null);
    }
}
