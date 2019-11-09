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
package org.lxtk.lx4e.model.impl;

import java.util.List;
import java.util.Map;

import org.eclipse.handly.model.IElement;
import org.eclipse.handly.model.impl.ISourceConstructImplExtension;
import org.eclipse.handly.model.impl.support.SourceElementBody;
import org.eclipse.handly.model.impl.support.StructureHelper;
import org.eclipse.handly.snapshot.ISnapshot;
import org.eclipse.handly.util.TextRange;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.lsp4j.DocumentSymbol;
import org.lxtk.lx4e.DocumentUtil;
import org.lxtk.lx4e.model.ILanguageSourceElement;
import org.lxtk.lx4e.model.ILanguageSourceFile;
import org.lxtk.lx4e.model.ILanguageSymbol;

class StructureBuilder
{
    private static final LanguageSymbol[] NO_SYMBOLS = new LanguageSymbol[0];

    private final Map<IElement, Object> newElements;
    private final IDocument source;
    private final ISnapshot snapshot;
    private final StructureHelper helper = new StructureHelper();

    StructureBuilder(Map<IElement, Object> newElements, IDocument source,
        ISnapshot snapshot)
    {
        this.newElements = newElements;
        this.source = source;
        this.snapshot = snapshot;
    }

    void buildStructure(ILanguageSourceFile handle,
        List<DocumentSymbol> symbols)
    {
        SourceElementBody body = new SourceElementBody();
        body.setFullRange(new TextRange(0, source.getLength()));
        body.setSnapshot(snapshot);

        for (DocumentSymbol symbol : symbols)
            process(handle, body, handle.getSymbol(symbol.getName(),
                symbol.getKind()), symbol);

        body.setChildren(helper.popChildren(body).toArray(NO_SYMBOLS));
        newElements.put(handle, body);
    }

    private void process(ILanguageSourceElement parent, Object parentBody,
        ILanguageSymbol handle, DocumentSymbol symbol)
    {
        helper.resolveDuplicates((ISourceConstructImplExtension)handle);
        SourceElementBody body = new SourceElementBody();
        try
        {
            body.setFullRange(toTextRange(DocumentUtil.toRegion(source,
                symbol.getRange())));
            body.setIdentifyingRange(toTextRange(DocumentUtil.toRegion(source,
                symbol.getSelectionRange())));
        }
        catch (BadLocationException e)
        {
            // ignore
        }
        body.setSnapshot(snapshot);
        body.set(ILanguageSymbol.DETAIL, symbol.getDetail());
        body.set(ILanguageSymbol.DEPRECATED, symbol.getDeprecated());

        for (DocumentSymbol child : symbol.getChildren())
            process(handle, body, handle.getSymbol(child.getName(),
                child.getKind()), child);

        body.setChildren(helper.popChildren(body).toArray(NO_SYMBOLS));
        newElements.put(handle, body);
        helper.pushChild(parentBody, handle);
    }

    private static TextRange toTextRange(IRegion region)
    {
        return new TextRange(region.getOffset(), region.getLength());
    }
}
