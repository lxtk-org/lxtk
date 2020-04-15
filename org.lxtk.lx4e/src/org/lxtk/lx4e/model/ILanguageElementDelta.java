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
package org.lxtk.lx4e.model;

import org.eclipse.handly.model.IElementDeltaConstants;
import org.eclipse.handly.model.IElementDeltaExtension;

/**
 * A language element delta describes changes in a language element between two
 * discrete points in time.  Given a delta, clients can access the element that
 * has changed, and any children that have changed.
 */
public interface ILanguageElementDelta
    extends IElementDeltaExtension, IElementDeltaConstants
{
    @Override
    ILanguageElement getElement();

    @Override
    ILanguageElementDelta[] getAffectedChildren();

    @Override
    ILanguageElementDelta[] getAddedChildren();

    @Override
    ILanguageElementDelta[] getRemovedChildren();

    @Override
    ILanguageElementDelta[] getChangedChildren();

    @Override
    ILanguageElement getMovedFromElement();

    @Override
    ILanguageElement getMovedToElement();

    /**
     * Returns the delta for the given element in this delta subtree,
     * or <code>null</code> if no delta is found for the given element.
     * <p>
     * This is a convenience method to avoid manual traversal of the delta tree
     * in cases where the listener is only interested in changes to particular
     * elements. Calling this method will generally be faster than manually
     * traversing the delta to a particular descendant.
     * </p>
     *
     * @param element the element to search the delta for (may be <code>null</code>)
     * @return the delta for the given element, or <code>null</code> if none
     */
    ILanguageElementDelta findDelta(ILanguageElement element);
}
