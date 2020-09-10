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

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.swt.graphics.Image;
import org.lxtk.lx4e.internal.ui.LSPImages;

/**
 * Default implementation of a label provider for {@link SymbolInformation} objects.
 */
public class SymbolLabelProvider
    extends LabelProvider
{
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
}
