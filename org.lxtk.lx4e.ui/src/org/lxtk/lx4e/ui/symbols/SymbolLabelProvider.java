/*******************************************************************************
 * Copyright (c) 2020 1C-Soft LLC.
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
package org.lxtk.lx4e.ui.symbols;

import java.util.List;

import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.SymbolTag;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.TextStyle;
import org.lxtk.lx4e.internal.ui.LSPImages;

/**
 * Default implementation of a label provider for {@link SymbolInformation} objects.
 */
public class SymbolLabelProvider
    extends LabelProvider
    implements IStyledLabelProvider
{
    private static final Styler DEPRECATED_STYLER = new Styler()
    {
        @Override
        public void applyStyles(TextStyle textStyle)
        {
            textStyle.strikeout = true;
        };
    };

    @Override
    public StyledString getStyledText(Object element)
    {
        if (element instanceof SymbolInformation)
        {
            SymbolInformation symbol = (SymbolInformation)element;

            if (isDeprecated(symbol))
                return new StyledString(symbol.getName(), DEPRECATED_STYLER);

            return new StyledString(symbol.getName());
        }
        return new StyledString();
    }

    @Override
    public String getText(Object element)
    {
        if (element instanceof SymbolInformation)
            return ((SymbolInformation)element).getName();
        return super.getText(element);
    }

    @Override
    public Image getImage(Object element)
    {
        if (element instanceof SymbolInformation)
            return LSPImages.imageFromSymbolKind(((SymbolInformation)element).getKind());
        return super.getImage(element);
    }

    private static boolean isDeprecated(SymbolInformation symbol)
    {
        List<SymbolTag> tags = symbol.getTags();
        if (tags != null && tags.contains(SymbolTag.Deprecated))
            return true;

        @SuppressWarnings("deprecation")
        boolean deprecated = Boolean.TRUE.equals(symbol.getDeprecated());
        return deprecated;
    }
}
