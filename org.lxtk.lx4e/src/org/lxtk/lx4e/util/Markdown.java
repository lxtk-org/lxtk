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
package org.lxtk.lx4e.util;

import java.io.StringWriter;

import org.eclipse.mylyn.wikitext.markdown.MarkdownLanguage;
import org.eclipse.mylyn.wikitext.parser.MarkupParser;
import org.eclipse.mylyn.wikitext.parser.builder.HtmlDocumentBuilder;

/**
 * TODO JavaDoc
 */
public class Markdown
{
    /**
     * TODO JavaDoc
     *
     * @param markdown the content to parse (not <code>null</code>)
     * @param asDocument indicates if the resulting HTML should be emitted as
     *  a document. If <code>false</code>, the <code>html</code> and <code>body</code>
     *  tags are not included in the output
     * @return the HTML document text (never <code>null</code>)
     */
    public static String toHtml(String markdown, boolean asDocument)
    {
        StringWriter out = new StringWriter();
        MarkupParser parser = new MarkupParser(new MarkdownLanguage(),
            new HtmlDocumentBuilder(out));
        parser.parse(markdown, asDocument);
        return out.toString();
    }

    private Markdown()
    {
    }
}
