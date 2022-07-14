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
package org.lxtk.lx4e.ui.typehierarchy;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

/**
 * Default implementation of a label provider for {@link TypeHierarchyElement}s.
 */
public class TypeHierarchyElementLabelProvider
    extends LabelProvider
{
    @Override
    public String getText(Object element)
    {
        if (element instanceof TypeHierarchyElement)
            return ((TypeHierarchyElement)element).getLabel();

        return super.getText(element);
    }

    @Override
    public Image getImage(Object element)
    {
        if (element instanceof TypeHierarchyElement)
            return ((TypeHierarchyElement)element).getImage();

        return super.getImage(element);
    }
}
