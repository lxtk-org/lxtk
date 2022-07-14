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

import java.util.Objects;

import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.lsp4j.TypeHierarchyItem;
import org.eclipse.swt.graphics.Image;
import org.lxtk.DocumentUri;
import org.lxtk.lx4e.internal.ui.LSPImages;

/**
 * Represents a model element of LXTK-based type hierarchy view.
 */
public final class TypeHierarchyElement
    implements IAdaptable
{
    private final TypeHierarchyItem item;
    private final TypeHierarchyUtility utility;

    /**
     * Constructor.
     *
     * @param item not <code>null</code>
     * @param utility not <code>null</code>
     */
    public TypeHierarchyElement(TypeHierarchyItem item, TypeHierarchyUtility utility)
    {
        this.item = Objects.requireNonNull(item);
        this.utility = Objects.requireNonNull(utility);
    }

    /**
     * Returns the type hierarchy item associated with this element.
     *
     * @return the type hierarchy item of this element (never <code>null</code>)
     */
    public TypeHierarchyItem getTypeHierarchyItem()
    {
        return item;
    }

    /**
     * Returns the type hierarchy utility associated with this element.
     *
     * @return the type hierarchy utility of this element (never <code>null</code>)
     */
    public TypeHierarchyUtility getTypeHierarchyUtility()
    {
        return utility;
    }

    /**
     * Returns the text label for this element.
     *
     * @return the text label for this element (never <code>null</code>)
     */
    public String getLabel()
    {
        String detail = item.getDetail();
        if (detail != null)
            return detail;

        return item.getName();
    }

    /**
     * Returns the image for this element.
     *
     * @return the image for this element, or <code>null</code> if none
     */
    public Image getImage()
    {
        return LSPImages.imageFromSymbolKind(item.getKind());
    }

    /**
     * Returns the image descriptor for this element.
     *
     * @return the image descriptor for this element, or <code>null</code> if none
     */
    public ImageDescriptor getImageDescriptor()
    {
        return LSPImages.imageDescriptorFromSymbolKind(item.getKind());
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        TypeHierarchyElement other = (TypeHierarchyElement)obj;
        return item.equals(other.item);
    }

    @Override
    public int hashCode()
    {
        return item.hashCode();
    }

    @Override
    public String toString()
    {
        return item.toString();
    }

    @Override
    public <T> T getAdapter(Class<T> adapter)
    {
        return Adapters.adapt(
            utility.uriHandler.getCorrespondingElement(DocumentUri.convert(item.getUri())),
            adapter);
    }
}
