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
 * Default implementation of a pattern matcher intended to be used for matching completion proposals.
 */
public class DefaultMatcher
{
    private static final int S_CASE = 100;
    private static final int S_CAMELCASE = 50;
    private static final int S_EXACT = 40;
    private static final int S_PREFIX = 30;
    private static final int S_SUBSTRING = 20;
    private static final int S_SUBWORD = 10;

    /**
     * Matches the given string against the given pattern.
     *
     * @param pattern not <code>null</code>
     * @param string not <code>null</code>
     * @return the match result if there is a match, or <code>null</code> if there is no match
     */
    public MatchResult match(String pattern, String string)
    {
        int patternLength = pattern.length();
        if (patternLength == 0 || patternLength > string.length())
            return null;

        if (string.equals(pattern))
        {
            return new DefaultMatchResult(new int[] { 0, patternLength }, S_EXACT + S_CASE);
        }
        if (string.equalsIgnoreCase(pattern))
        {
            return new DefaultMatchResult(new int[] { 0, patternLength }, S_EXACT);
        }
        if (string.substring(0, patternLength).equals(pattern))
        {
            return new DefaultMatchResult(new int[] { 0, patternLength }, S_PREFIX + S_CASE);
        }
        if (string.substring(0, patternLength).equalsIgnoreCase(pattern))
        {
            return new DefaultMatchResult(new int[] { 0, patternLength }, S_PREFIX);
        }
        if (isCamelCaseMatching())
        {
            int[] regions = CharOperation.getCamelCaseMatchingRegions(pattern, 0, patternLength,
                string, 0, string.length(), false);
            if (regions != null)
                return new DefaultMatchResult(regions, S_CAMELCASE);
        }
        if (isSubstringMatching())
        {
            int index = CharOperation.indexOf(pattern.toCharArray(), string.toCharArray(), false);
            if (index >= 0)
                return new DefaultMatchResult(new int[] { index, patternLength }, S_SUBSTRING);
        }
        if (isSubwordMatching())
        {
            int[] regions = CharOperation.getSubWordMatchingRegions(pattern, string);
            if (regions != null)
                return new DefaultMatchResult(regions, S_SUBWORD);
        }
        return null;
    }

    /**
     * Returns whether camel case matching is enabled.
     *
     * @return <code>true</code> if camel case matching is enabled,
     *  and <code>false</code> otherwise
     */
    protected boolean isCamelCaseMatching()
    {
        return true;
    }

    /**
     * Returns whether substring matching is enabled.
     *
     * @return <code>true</code> if substring matching is enabled,
     *  and <code>false</code> otherwise
     */
    protected boolean isSubstringMatching()
    {
        return true;
    }

    /**
     * Returns whether subword matching is enabled.
     *
     * @return <code>true</code> if subword matching is enabled,
     *  and <code>false</code> otherwise
     */
    protected boolean isSubwordMatching()
    {
        return true;
    }
}
