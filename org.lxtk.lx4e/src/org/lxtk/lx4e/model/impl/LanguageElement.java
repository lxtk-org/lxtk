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

import static org.eclipse.handly.context.Contexts.EMPTY_CONTEXT;

import java.net.URI;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.handly.model.impl.support.Element;
import org.lxtk.lx4e.model.ILanguageElement;

/**
 * Root of language element handle hierarchy.
 */
public abstract class LanguageElement
    extends Element
    implements ILanguageElement
{
    /**
     * Constructs a handle for an element with the given parent element
     * and the given name.
     *
     * @param parent the parent of the element,
     *  or <code>null</code> if the element has no parent
     * @param name the name of the element,
     *  or <code>null</code> if the element has no name
     */
    public LanguageElement(LanguageElement parent, String name)
    {
        super(parent, name);
    }

    @Override
    public final String getName()
    {
        return getName_();
    }

    @Override
    public final ILanguageElement getParent()
    {
        return (ILanguageElement)getParent_();
    }

    @Override
    public final IResource getResource()
    {
        return getResource_();
    }

    @Override
    public final URI getLocationUri()
    {
        return getLocationUri_();
    }

    @Override
    public final boolean exists()
    {
        return exists_();
    }

    @Override
    public final ILanguageElement[] getChildren(IProgressMonitor monitor)
        throws CoreException
    {
        return (ILanguageElement[])getChildren_(EMPTY_CONTEXT, monitor);
    }
}
