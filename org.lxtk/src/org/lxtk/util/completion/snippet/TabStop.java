/*******************************************************************************
 * Copyright (c) 2021 1C-Soft LLC.
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
package org.lxtk.util.completion.snippet;

import java.util.Arrays;
import java.util.Objects;

/**
 * Represents a tab stop in a snippet.
 *
 * @see Snippet
 */
public final class TabStop
{
    private final String id;
    private final int[] offsets;
    private final String[] values;
    private Integer ordinal;

    TabStop(String id, int[] offsets, String... values)
    {
        if (id.isEmpty())
            throw new IllegalArgumentException();
        this.id = id;
        if (offsets.length == 0)
            throw new IllegalArgumentException();
        this.offsets = offsets;
        this.values = Objects.requireNonNull(values);
    }

    /**
     * Returns the identifier of this tab stop.
     *
     * @return the tab stop identifier (never <code>null</code>, never empty)
     */
    public String getId()
    {
        return id;
    }

    /**
     * Returns the offsets of this tab stop relative to the beginning of the snippet text.
     * <p>
     * The first offset marks the definition of the tab stop.
     * The other offsets (if any) mark its mirrors.
     * The offsets are zero-based.
     * </p>
     *
     * @return the tab stop offsets relative to the start of the snippet
     *  (never <code>null</code>, never empty)
     */
    public int[] getOffsets()
    {
        return offsets;
    }

    /**
     * Returns the values of this tab stop, if any.
     * <p>
     * Placeholders always have a single value.
     * Choices can have multiple values.
     * Ordinary tab stops have no values.
     * </p>
     *
     * @return the tab stop values (never <code>null</code>, may be empty)
     */
    public String[] getValues()
    {
        return values;
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
        TabStop other = (TabStop)obj;
        if (!id.equals(other.id))
            return false;
        if (!Arrays.equals(offsets, other.offsets))
            return false;
        if (!Arrays.equals(values, other.values))
            return false;
        return true;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + id.hashCode();
        result = prime * result + Arrays.hashCode(offsets);
        result = prime * result + Arrays.hashCode(values);
        return result;
    }

    @Override
    public String toString()
    {
        return "{id=" + id + ", offsets=" + Arrays.toString(offsets) //$NON-NLS-1$ //$NON-NLS-2$
            + ", values=" + Arrays.toString(values) + '}'; //$NON-NLS-1$
    }

    int getOrdinal()
    {
        if (ordinal == null)
        {
            try
            {
                ordinal = Integer.valueOf(id);
            }
            catch (NumberFormatException e)
            {
                ordinal = Integer.MAX_VALUE;
            }
        }
        return ordinal;
    }

    // Note: the ordering is inconsistent with equals
    int compareTo(TabStop other)
    {
        int ordinal = getOrdinal();
        int diff = ordinal - other.getOrdinal();
        if (diff != 0)
            return diff;
        return offsets[0] - other.offsets[0];
    }
}
