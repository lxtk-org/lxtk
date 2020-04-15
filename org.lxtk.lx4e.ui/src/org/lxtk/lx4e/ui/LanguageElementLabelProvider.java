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
package org.lxtk.lx4e.ui;

import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.lxtk.lx4e.internal.ui.LSPImages;
import org.lxtk.lx4e.model.ILanguageElement;
import org.lxtk.lx4e.model.ILanguageSymbol;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StyledString;

/**
 * Default implementation of a label provider for {@link ILanguageElement}s.
 */
public class LanguageElementLabelProvider
    extends LabelProvider
    implements IStyledLabelProvider
{
    @Override
    public StyledString getStyledText(Object element)
    {
        String text = getText(element);
        if (text == null)
            return new StyledString();
        return new StyledString(text);
    }

    @Override
    public String getText(Object element)
    {
        if (element instanceof ILanguageElement)
            return ((ILanguageElement)element).getName();
        return super.getText(element);
    }

    @Override
    public Image getImage(Object element)
    {
        if (element instanceof ILanguageSymbol)
            return LSPImages.imageFromSymbolKind(
                ((ILanguageSymbol)element).getKind());
        return super.getImage(element);
    }
}
