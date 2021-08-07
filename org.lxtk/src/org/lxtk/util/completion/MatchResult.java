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
 * Represents the result of a successful match of a string against a pattern.
 */
public interface MatchResult
{
    /**
     * Returns the regions in the string matching the pattern.
     * <p>
     * The matching regions are returned as an array of <code>int</code>s with two slots per
     * each region: the first one is the region starting index and the second one is the region
     * length. Therefore, the array always has an even length.
     * </p>
     *
     * @return the matching regions (never <code>null</code>)
     */
    int[] getMatchingRegions();

    /**
     * Returns the score of the match result.
     * <p>
     * The score can be used to sort match results produced by the same matcher implementation for
     * the same pattern. Matches with higher score should be listed before matches with lower score.
     * </p>
     *
     * @return the match result score
     */
    int getScore();
}
