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

import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.handly.model.impl.support.ElementDelta;
import org.lxtk.lx4e.model.ILanguageElement;
import org.lxtk.lx4e.model.ILanguageElementDelta;

/**
 * Default implementation of {@link ILanguageElementDelta}.
 */
public class LanguageElementDelta
    extends ElementDelta
    implements ILanguageElementDelta
{
    private static final LanguageElementDelta[] NO_CHILDREN =
        new LanguageElementDelta[0];

    /**
     * Constructs an initially empty delta for the given element.
     *
     * @param element the element that this delta describes a change to
     *  (not <code>null</code>)
     */
    public LanguageElementDelta(ILanguageElement element)
    {
        super(element);
        setAffectedChildren_(NO_CHILDREN); // ensure that runtime type of affectedChildren is LanguageElementDelta[]
    }

    /**
     * Sets the kind of this delta.
     * <p>
     * This is a low-level mutator method. In particular, it is the caller's
     * responsibility to ensure validity of the given value.
     * </p>
     *
     * @param kind the delta kind
     * @see #getKind_()
     */
    public final void setKind(int kind)
    {
        setKind_(kind);
    }

    /**
     * Sets the flags for this delta.
     * <p>
     * This is a low-level mutator method. In particular, it is the caller's
     * responsibility to ensure validity of the given value.
     * </p>
     *
     * @param flags the delta flags
     * @see #getFlags_()
     */
    public final void setFlags(long flags)
    {
        setFlags_(flags);
    }

    /**
     * Sets an element describing this delta's element before it was moved
     * to its current location.
     * <p>
     * This is a low-level mutator method. In particular, it is the caller's
     * responsibility to set appropriate flags.
     * </p>
     *
     * @param movedFromElement an element describing this delta's element
     *  before it was moved to its current location
     * @see #getMovedFromElement_()
     */
    public final void setMovedFromElement(ILanguageElement movedFromElement)
    {
        setMovedFromElement_(movedFromElement);
    }

    /**
     * Sets an element describing this delta's element in its new location.
     * <p>
     * This is a low-level mutator method. In particular, it is the caller's
     * responsibility to set appropriate flags.
     * </p>
     *
     * @param movedToElement an element describing this delta's element
     *  in its new location
     * @see #getMovedToElement_()
     */
    public final void setMovedToElement(ILanguageElement movedToElement)
    {
        setMovedToElement_(movedToElement);
    }

    /**
     * Sets the marker deltas. Clients <b>must not</b> modify
     * the given array afterwards.
     * <p>
     * This is a low-level mutator method. In particular, it is the caller's
     * responsibility to set appropriate flags.
     * </p>
     *
     * @param markerDeltas the marker deltas
     * @see #getMarkerDeltas_()
     */
    public final void setMarkerDeltas(IMarkerDelta[] markerDeltas)
    {
        setMarkerDeltas_(markerDeltas);
    }

    /**
     * Sets the resource deltas for this delta. Clients <b>must not</b> modify
     * the given array afterwards.
     * <p>
     * This is a low-level mutator method. In particular, it is the caller's
     * responsibility to set appropriate kind and flags for this delta.
     * </p>
     *
     * @param resourceDeltas the resource deltas
     * @see #getResourceDeltas_()
     */
    public final void setResourceDeltas(IResourceDelta[] resourceDeltas)
    {
        setResourceDeltas_(resourceDeltas);
    }

    @Override
    public ILanguageElement getElement()
    {
        return (ILanguageElement)getElement_();
    }

    @Override
    public LanguageElementDelta[] getAffectedChildren()
    {
        return (LanguageElementDelta[])getAffectedChildren_();
    }

    @Override
    public LanguageElementDelta[] getAddedChildren()
    {
        return (LanguageElementDelta[])getAddedChildren_();
    }

    @Override
    public LanguageElementDelta[] getRemovedChildren()
    {
        return (LanguageElementDelta[])getRemovedChildren_();
    }

    @Override
    public LanguageElementDelta[] getChangedChildren()
    {
        return (LanguageElementDelta[])getChangedChildren_();
    }

    @Override
    public ILanguageElement getMovedFromElement()
    {
        return (ILanguageElement)getMovedFromElement_();
    }

    @Override
    public ILanguageElement getMovedToElement()
    {
        return (ILanguageElement)getMovedToElement_();
    }

    @Override
    public LanguageElementDelta findDelta(ILanguageElement element)
    {
        return (LanguageElementDelta)findDelta_(element);
    }
}
