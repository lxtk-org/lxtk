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
import static org.eclipse.handly.context.Contexts.of;
import static org.eclipse.handly.model.Elements.BASE_SNAPSHOT;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.handly.model.ISourceElementInfo;
import org.eclipse.handly.model.impl.support.ISourceElementImplSupport;
import org.eclipse.handly.snapshot.ISnapshot;
import org.lxtk.lx4e.model.ILanguageSourceElement;

/**
 * Root of language source element hierarchy.
 */
public abstract class LanguageSourceElement
    extends LanguageElement
    implements ILanguageSourceElement, ISourceElementImplSupport
{
    /**
     * Constructs a handle for a source element with the given parent element
     * and the given name.
     *
     * @param parent the parent of the element,
     *  or <code>null</code> if the element has no parent
     * @param name the name of the element,
     *  or <code>null</code> if the element has no name
     */
    public LanguageSourceElement(LanguageElement parent, String name)
    {
        super(parent, name);
    }

    @Override
    public final ILanguageSourceElement getSourceElementAt(int position, ISnapshot base,
        IProgressMonitor monitor) throws CoreException
    {
        return (ILanguageSourceElement)getSourceElementAt_(position, of(BASE_SNAPSHOT, base),
            monitor);
    }

    @Override
    public final ISourceElementInfo getSourceElementInfo(IProgressMonitor monitor)
        throws CoreException
    {
        return getSourceElementInfo_(EMPTY_CONTEXT, monitor);
    }
}
