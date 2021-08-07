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
package org.lxtk.util.completion;

/**
 * Default implementation of the {@link MatchResult} interface.
 */
public final class DefaultMatchResult
    implements MatchResult
{
    private final int[] matchingRegions;
    private final int score;

    /**
     * Constructor.
     *
     * @param matchingRegions not <code>null</code>, not empty
     * @param score the match result score
     */
    public DefaultMatchResult(int[] matchingRegions, int score)
    {
        if (matchingRegions.length == 0)
            throw new IllegalArgumentException();
        this.matchingRegions = matchingRegions;
        this.score = score;
    }

    @Override
    public int[] getMatchingRegions()
    {
        return matchingRegions;
    }

    @Override
    public int getScore()
    {
        return score;
    }
}
