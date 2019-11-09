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
package org.lxtk.lx4e.model;

import java.net.URI;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.handly.model.IElement;

/**
 * Common interface for language elements.
 * <p>
 * Language elements are exposed to clients as handles to the actual underlying
 * element. The model may hand out any number of handles for each element.
 * Handles that refer to the same element are guaranteed to be equal, but
 * not necessarily identical.
 * </p>
 */
public interface ILanguageElement
    extends IElement, IAdaptable
{
    /**
     * Returns the name of this element, or <code>null</code>
     * if this element has no name. This is a handle-only method.
     *
     * @return the element name, or <code>null</code> if this element has no name
     */
    String getName();

    /**
     * Returns the element directly containing this element,
     * or <code>null</code> if this element has no parent.
     * This is a handle-only method.
     *
     * @return the parent element, or <code>null</code> if this element has
     *  no parent
     */
    ILanguageElement getParent();

    /**
     * Returns the innermost resource enclosing this element, or <code>null</code>
     * if this element is not enclosed in a workspace resource.
     * This is a handle-only method.
     * <p>
     * Note that it is safe to call this method and test the return value
     * for <code>null</code> even when <code>org.eclipse.core.resources</code>
     * bundle is not available.
     * </p>
     *
     * @return the innermost resource enclosing this element, or <code>null</code>
     *  if this element is not enclosed in a workspace resource
     */
    IResource getResource();

    /**
     * Returns a file system location for this element. The resulting URI is
     * suitable to passing to <code>EFS.getStore(URI)</code>. Returns
     * <code>null</code> if no location can be determined.
     *
     * @return a file system location for this element,
     *  or <code>null</code> if no location can be determined
     */
    URI getLocationUri();

    /**
     * Returns whether this element exists in the model.
     * <p>
     * Handles may or may not be backed by an actual element. Handles that are
     * backed by an actual element are said to "exist".
     * </p>
     *
     * @return <code>true</code> if this element exists in the model, and
     *  <code>false</code> if this element does not exist
     */
    boolean exists();

    /**
     * Returns the immediate children of this element.
     *
     * @param monitor a progress monitor, or <code>null</code>
     *  if progress reporting is not desired. The caller must not rely on
     *  {@link IProgressMonitor#done()} having been called by the receiver
     * @return the immediate children of this element (never <code>null</code>).
     *  Clients <b>must not</b> modify the returned array.
     * @throws CoreException if this element does not exist or if an
     *  exception occurs while accessing its corresponding resource
     */
    ILanguageElement[] getChildren(IProgressMonitor monitor)
        throws CoreException;
}
