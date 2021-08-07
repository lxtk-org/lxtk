/*******************************************************************************
 * Copyright (c) 2000, 2021 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Luiz-Otavio Zorzella <zorzella at gmail dot com> - Improve CamelCase algorithm
 *     Gábor Kövesdán - Contribution for Bug 350000 - [content assist] Include non-prefix matches in auto-complete suggestions
 *     Vladimir Piskarev (1C) - adaptation
 *          (adapted from org.eclipse.jdt.core.compiler.CharOperation)
 *******************************************************************************/
package org.lxtk.util.completion;

/**
 * This class is a collection of helper methods to manipulate char arrays.
 */
//@formatter:off
class CharOperation {

	private static final int[] EMPTY_REGIONS = new int[0];

/**
 * Answers all the regions in a given name matching a given camel case pattern.
 * </p><p>
 * Each of these regions is made of its starting index and its length in the given
 * name. They are all concatenated in a single array of <code>int</code>
 * which therefore always has an even length.
 * </p><p>
 * Note that each region is disjointed from the following one.<br>
 * E.g. if the regions are <code>{ start1, length1, start2, length2 }</code>,
 * then <code>start1+length1</code> will always be smaller than
 * <code>start2</code>.
 * </p><p>
 * <pre>
 * Examples:
 * <ol><li>  pattern = "NPE"
 *  name = NullPointerException / NoPermissionException
 *  result:  { 0, 1, 4, 1, 11, 1 } / { 0, 1, 2, 1, 12, 1 } </li>
 * <li>  pattern = "NuPoEx"
 *  name = NullPointerException
 *  result:  { 0, 2, 4, 2, 11, 2 }</li>
 * <li>  pattern = "IPL3"
 *  name = "IPerspectiveListener3"
 *  result:  { 0, 2, 12, 1, 20, 1 }</li>
 * <li>  pattern = "HashME"
 *  name = "HashMapEntry"
 *  result:  { 0, 5, 7, 1 }</li>
 * </ol></pre>
 *</p>
 *
 * @param pattern the given pattern
 * @param patternStart the start index of the pattern, inclusive
 * @param patternEnd the end index of the pattern, exclusive
 * @param name the given name
 * @param nameStart the start index of the name, inclusive
 * @param nameEnd the end index of the name, exclusive
 * @param samePartCount flag telling whether the pattern and the name should
 *  have the same count of parts or not.<br>
 *  &nbsp;&nbsp;For example:
 *  <ul>
 *      <li>'HM' type string pattern will match 'HashMap' and 'HtmlMapper' types,
 *              but not 'HashMapEntry'</li>
 *      <li>'HMap' type string pattern will still match previous 'HashMap' and
 *              'HtmlMapper' types, but not 'HighMagnitude'</li>
 *  </ul>
 * @return an array of <code>int</code> having two slots per returned
 *  regions (first one is the starting index of the region and the second
 *  one the length of the region).<br>
 *  Note that it may be <code>null</code> if the given name does not match
 *  the pattern
 */
public static final int[] getCamelCaseMatchingRegions(String pattern, int patternStart, int patternEnd, String name, int nameStart, int nameEnd, boolean samePartCount) {

    if (name == null)
        return null; // null name cannot match
    if (pattern == null) {
        // null pattern cannot match any region
        // see bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=264816
        return EMPTY_REGIONS;
    }
    if (patternEnd < 0)     patternEnd = pattern.length();
    if (nameEnd < 0) nameEnd = name.length();

    if (patternEnd <= patternStart) {
        return nameEnd <= nameStart
            ? new int[] { patternStart, patternEnd-patternStart }
            : null;
    }
    if (nameEnd <= nameStart) return null;
    // check first pattern char
    if (name.charAt(nameStart) != pattern.charAt(patternStart)) {
        // first char must strictly match (upper/lower)
        return null;
    }

    char patternChar, nameChar;
    int iPattern = patternStart;
    int iName = nameStart;

    // init segments
    int parts = 1;
    for (int i=patternStart+1; i<patternEnd; i++) {
        final char ch = pattern.charAt(i);
        if (Character.isUnicodeIdentifierPart(ch) && (Character.isUpperCase(ch) || Character.isDigit(ch))) {
            parts++;
        }
    }
    int[] segments = null;
    int count = 0; // count

    // Main loop is on pattern characters
    int segmentStart = iName;
    while (true) {
        iPattern++;
        iName++;

        if (iPattern == patternEnd) { // we have exhausted pattern...
            // it's a match if the name can have additional parts (i.e. uppercase characters) or is also exhausted
            if (!samePartCount || iName == nameEnd) {
                if (segments == null) {
                    segments = new int[2];
                }
                segments[count++] = segmentStart;
                segments[count++] = iName - segmentStart;
                if (count < segments.length) {
                    System.arraycopy(segments, 0, segments = new int[count], 0, count);
                }
                return segments;
            }

            // otherwise it's a match only if the name has no more uppercase characters
            int segmentEnd = iName;
            while (true) {
                if (iName == nameEnd) {
                    // we have exhausted the name, so it's a match
                    if (segments == null) {
                        segments = new int[2];
                    }
                    segments[count++] = segmentStart;
                    segments[count++] = segmentEnd - segmentStart;
                    if (count < segments.length) {
                        System.arraycopy(segments, 0, segments = new int[count], 0, count);
                    }
                    return segments;
                }
                nameChar = name.charAt(iName);
                // test if the name character is uppercase
                if (!Character.isUnicodeIdentifierPart(nameChar) || Character.isUpperCase(nameChar)) {
                    return null;
                }
                iName++;
            }
        }

        if (iName == nameEnd){
            // We have exhausted the name (and not the pattern), so it's not a match
            return null;
        }

        // For as long as we're exactly matching, bring it on (even if it's a lower case character)
        if ((patternChar = pattern.charAt(iPattern)) == name.charAt(iName)) {
            continue;
        }
        int segmentEnd = iName;

        // If characters are not equals, then it's not a match if patternChar is lowercase
        if (Character.isUnicodeIdentifierPart(patternChar) && !Character.isUpperCase(patternChar) && !Character.isDigit(patternChar)) {
            return null;
        }

        // patternChar is uppercase, so let's find the next uppercase in name
        while (true) {
            if (iName == nameEnd){
                //  We have exhausted name (and not pattern), so it's not a match
                return null;
            }

            nameChar = name.charAt(iName);
            if (Character.isUnicodeIdentifierPart(nameChar) && !Character.isUpperCase(nameChar)) {
                iName++;
            } else if (Character.isDigit(nameChar)) {
                if (patternChar == nameChar) break;
                iName++;
            } else  if (patternChar != nameChar) {
                return null;
            } else {
                break;
            }
        }
        // At this point, either name has been exhausted, or it is at an uppercase letter.
        // Since pattern is also at an uppercase letter
        if (segments == null) {
            segments = new int[parts*2];
        }
        segments[count++] = segmentStart;
        segments[count++] = segmentEnd - segmentStart;
        segmentStart = iName;
    }
}

/**
 * Answers all the regions in a given name matching a subword pattern.
 * <p>
 * Each of these regions is made of its starting index and its length in the given
 * name. They are all concatenated in a single array of <code>int</code>
 * which therefore always has an even length.
 * <p>
 * Note that each region is disjointed from the following one.<br>
 * E.g. if the regions are <code>{ start1, length1, start2, length2 }</code>,
 * then <code>start1+length1</code> will always be smaller than
 * <code>start2</code>.
 * <p>
 * Examples:
 * <ol>
 * <li><pre>
 *    pattern = "linkedmap"
 *    name = LinkedHashMap
 *    result:  { 0, 6, 10, 3 }
 * </pre></li>
 * </ol>
 *
 * @param pattern the given pattern
 * @param name the given name
 * @return an array of <code>int</code> having two slots per returned
 * 	regions (first one is the starting index of the region and the second
 * 	one the length of the region).<br>
 * 	Note that it may be <code>null</code> if the given name does not match
 * 	the pattern
 */
public static final int[] getSubWordMatchingRegions(String pattern, String name) {

	if (name == null)
		return null; // null name cannot match
	if (pattern == null) {
		// null pattern cannot match any region
		// see bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=264816
		return EMPTY_REGIONS;
	}

	return new SubwordMatcher(name).getMatchingRegions(pattern);
}

/**
 * Answers the first index in the array for which the toBeFound array is a matching
 * subarray following the case rule. Answers -1 if no match is found.
 * <br>
 * <br>
 * For example:
 * <ol>
 * <li><pre>
 *    toBeFound = { 'c' }
 *    array = { ' a', 'b', 'c', 'd' }
 *    result => 2
 * </pre>
 * </li>
 * <li><pre>
 *    toBeFound = { 'e' }
 *    array = { ' a', 'b', 'c', 'd' }
 *    result => -1
 * </pre>
 * </li>
 * </ol>
 *
 * @param toBeFound the subarray to search
 * @param array the array to be searched
 * @param isCaseSensitive flag to know if the matching should be case sensitive
 * @return the first index in the array for which the toBeFound array is a matching
 * subarray following the case rule, -1 otherwise
 * @throws NullPointerException if array is null or toBeFound is null
 */
public static final int indexOf(char[] toBeFound, char[] array, boolean isCaseSensitive) {
    return indexOf(toBeFound, array, isCaseSensitive, 0);
}

/**
 * Answers the first index in the array for which the toBeFound array is a matching
 * subarray following the case rule starting at the index start. Answers -1 if no match is found.
 * <br>
 * <br>
 * For example:
 * <ol>
 * <li><pre>
 *    toBeFound = { 'c' }
 *    array = { ' a', 'b', 'c', 'd' }
 *    result => 2
 * </pre>
 * </li>
 * <li><pre>
 *    toBeFound = { 'e' }
 *    array = { ' a', 'b', 'c', 'd' }
 *    result => -1
 * </pre>
 * </li>
 * </ol>
 *
 * @param toBeFound the subarray to search
 * @param array the array to be searched
 * @param isCaseSensitive flag to know if the matching should be case sensitive
 * @param start the starting index
 * @return the first index in the array for which the toBeFound array is a matching
 * subarray following the case rule starting at the index start, -1 otherwise
 * @throws NullPointerException if array is null or toBeFound is null
 */
public static final int indexOf(final char[] toBeFound, final char[] array, final boolean isCaseSensitive, final int start) {
    return indexOf(toBeFound, array, isCaseSensitive, start, array.length);
}

/**
 * Answers the first index in the array for which the toBeFound array is a matching
 * subarray following the case rule starting at the index start. Answers -1 if no match is found.
 * <br>
 * <br>
 * For example:
 * <ol>
 * <li><pre>
 *    toBeFound = { 'c' }
 *    array = { ' a', 'b', 'c', 'd' }
 *    result => 2
 * </pre>
 * </li>
 * <li><pre>
 *    toBeFound = { 'e' }
 *    array = { ' a', 'b', 'c', 'd' }
 *    result => -1
 * </pre>
 * </li>
 * </ol>
 *
 * @param toBeFound the subarray to search
 * @param array the array to be searched
 * @param isCaseSensitive flag to know if the matching should be case sensitive
 * @param start the starting index (inclusive)
 * @param end the end index (exclusive)
 * @return the first index in the array for which the toBeFound array is a matching
 * subarray following the case rule starting at the index start, -1 otherwise
 * @throws NullPointerException if array is null or toBeFound is null
 */
public static final int indexOf(final char[] toBeFound, final char[] array, final boolean isCaseSensitive, final int start, final int end) {
    final int arrayLength = end;
    final int toBeFoundLength = toBeFound.length;
    if (toBeFoundLength > arrayLength || start < 0) return -1;
    if (toBeFoundLength == 0) return 0;
    if (toBeFoundLength == arrayLength) {
        if (isCaseSensitive) {
            for (int i = start; i < arrayLength; i++) {
                if (array[i] != toBeFound[i]) return -1;
            }
        } else {
            for (int i = start; i < arrayLength; i++) {
                if (Character.toLowerCase(array[i]) != Character.toLowerCase(toBeFound[i])) return -1;
            }
        }
        return 0;
    }
    if (isCaseSensitive) {
        arrayLoop: for (int i = start, max = arrayLength - toBeFoundLength + 1; i < max; i++) {
            if (array[i] == toBeFound[0]) {
                for (int j = 1; j < toBeFoundLength; j++) {
                    if (array[i + j] != toBeFound[j]) continue arrayLoop;
                }
                return i;
            }
        }
    } else {
        arrayLoop: for (int i = start, max = arrayLength - toBeFoundLength + 1; i < max; i++) {
            if (Character.toLowerCase(array[i]) == Character.toLowerCase(toBeFound[0])) {
                for (int j = 1; j < toBeFoundLength; j++) {
                    if (Character.toLowerCase(array[i + j]) != Character.toLowerCase(toBeFound[j])) continue arrayLoop;
                }
                return i;
            }
        }
    }
    return -1;
}

private CharOperation() {}
}
