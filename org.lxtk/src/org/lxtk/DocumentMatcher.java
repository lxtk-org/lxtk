/*******************************************************************************
 * Copyright (c) 2019, 2022 1C-Soft LLC.
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
package org.lxtk;

import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.function.Function;

import org.eclipse.lsp4j.DocumentFilter;

/**
 * Computes the match between a document selector and a document. The document
 * is represented by URI and language identifier. The document selector is
 * represented by a collection of {@link DocumentFilter}s.
 */
public interface DocumentMatcher
{
    /**
     * Computes the match between the given {@link DocumentFilter}
     * and a document with the given URI and language identifier.
     *
     * @param filter not <code>null</code>
     * @param documentUri not <code>null</code>
     * @param documentLanguageId not <code>null</code>
     * @return the computed match score; positive if the document matches,
     *  and 0 if the document does not match
     */
    int match(DocumentFilter filter, URI documentUri, String documentLanguageId);

    /**
     * Computes the match between the given document selector and a document
     * with the given URI and language identifier.
     *
     * @param selector may be <code>null</code> or empty,
     *  in which case 0 is returned
     * @param documentUri not <code>null</code>
     * @param documentLanguageId not <code>null</code>
     * @return the computed match score; positive if the document matches,
     *  and 0 if the document does not match
     */
    default int match(Iterable<DocumentFilter> selector, URI documentUri, String documentLanguageId)
    {
        int result = 0;
        if (selector != null)
        {
            for (DocumentFilter filter : selector)
            {
                int score = match(filter, documentUri, documentLanguageId);
                if (score > result)
                    result = score;
            }
        }
        return result;
    }

    /**
     * Determines whether there is a match between the given {@link DocumentFilter}
     * and a document with the given URI and language identifier.
     *
     * @param filter not <code>null</code>
     * @param documentUri not <code>null</code>
     * @param documentLanguageId not <code>null</code>
     * @return <code>true</code> if the document matches,
     *  and <code>false</code> if the document does not match
     */
    default boolean isMatch(DocumentFilter filter, URI documentUri, String documentLanguageId)
    {
        return match(filter, documentUri, documentLanguageId) > 0;
    }

    /**
     * Determines whether there is a match between the given document selector
     * and a document with the given URI and language identifier.
     *
     * @param selector may be <code>null</code> or empty,
     *  in which case <code>false</code> is returned
     * @param documentUri not <code>null</code>
     * @param documentLanguageId not <code>null</code>
     * @return <code>true</code> if the document matches,
     *  and <code>false</code> if the document does not match
     */
    default boolean isMatch(Iterable<DocumentFilter> selector, URI documentUri,
        String documentLanguageId)
    {
        if (selector != null)
        {
            for (DocumentFilter filter : selector)
            {
                if (isMatch(filter, documentUri, documentLanguageId))
                    return true;
            }
        }
        return false;
    }

    /**
     * Given a collection of candidate elements and a function for computing
     * the corresponding document selectors, returns the first element that
     * matches a document with the given URI and language identifier.
     *
     * @param <T> element type
     * @param candidates not <code>null</code>
     * @param selectorExtractor not  <code>null</code>
     * @param documentUri not <code>null</code>
     * @param documentLanguageId not <code>null</code>
     * @return the first matching element, or <code>null</code> if there are
     *  no matches
     */
    default <T> T getFirstMatch(Iterable<T> candidates,
        Function<T, Iterable<DocumentFilter>> selectorExtractor, URI documentUri,
        String documentLanguageId)
    {
        for (T candidate : candidates)
        {
            if (isMatch(selectorExtractor.apply(candidate), documentUri, documentLanguageId))
            {
                return candidate;
            }
        }
        return null;
    }

    /**
     * Given a collection of candidate elements and a function for computing
     * the corresponding document selectors, returns the element that has the
     * best match with a document with the given URI and language identifier.
     * If two or more elements are the best match, the first of them is returned.
     *
     * @param <T> element type
     * @param candidates not <code>null</code>
     * @param selectorExtractor not  <code>null</code>
     * @param documentUri not <code>null</code>
     * @param documentLanguageId not <code>null</code>
     * @return a matching element with the maximum match score, or <code>null</code>
     *  if there are no matches
     */
    default <T> T getBestMatch(Iterable<T> candidates,
        Function<T, Iterable<DocumentFilter>> selectorExtractor, URI documentUri,
        String documentLanguageId)
    {
        T result = null;
        int max = 0;
        for (T candidate : candidates)
        {
            int score = match(selectorExtractor.apply(candidate), documentUri, documentLanguageId);
            if (score > max)
            {
                max = score;
                result = candidate;
            }
        }
        return result;
    }

    /**
     * Given a collection of candidate elements and a function for computing
     * the corresponding document selectors, returns all elements that match
     * a document with the given URI and language identifier. The elements
     * are returned in their original order.
     *
     * @param <T> element type
     * @param candidates not <code>null</code>
     * @param selectorExtractor not  <code>null</code>
     * @param documentUri not <code>null</code>
     * @param documentLanguageId not <code>null</code>
     * @return the matching elements, in the original order (never <code>null</code>)
     */
    default <T> List<T> getMatches(Iterable<T> candidates,
        Function<T, Iterable<DocumentFilter>> selectorExtractor, URI documentUri,
        String documentLanguageId)
    {
        List<T> result = new ArrayList<>();
        for (T candidate : candidates)
        {
            if (isMatch(selectorExtractor.apply(candidate), documentUri, documentLanguageId))
            {
                result.add(candidate);
            }
        }
        return result;
    }

    /**
     * Given a collection of candidate elements and a function for computing
     * the corresponding document selectors, returns all elements that match
     * a document with the given URI and language identifier, sorted by
     * their match score, in descending order.
     *
     * @param <T> element type
     * @param candidates not <code>null</code>
     * @param selectorExtractor not  <code>null</code>
     * @param documentUri not <code>null</code>
     * @param documentLanguageId not <code>null</code>
     * @return the matching elements, sorted by their match score,
     *  in descending order (never <code>null</code>)
     */
    default <T> List<T> getSortedMatches(Iterable<T> candidates,
        Function<T, Iterable<DocumentFilter>> selectorExtractor, URI documentUri,
        String documentLanguageId)
    {
        List<T> result = new ArrayList<>();
        NavigableMap<Integer, List<T>> groupedMatches =
            getGroupedMatches(candidates, selectorExtractor, documentUri, documentLanguageId);
        for (List<T> group : groupedMatches.values())
        {
            result.addAll(group);
        }
        return result;
    }

    /**
     * Given a collection of candidate elements and a function for computing
     * the corresponding document selectors, returns all elements that match
     * a document with the given URI and language identifier, grouped by
     * their match score, in descending order.
     *
     * @param <T> element type
     * @param candidates not <code>null</code>
     * @param selectorExtractor not  <code>null</code>
     * @param documentUri not <code>null</code>
     * @param documentLanguageId not <code>null</code>
     * @return the matching elements, grouped by their match score,
     *  in descending order (never <code>null</code>)
     */
    default <T> NavigableMap<Integer, List<T>> getGroupedMatches(Iterable<T> candidates,
        Function<T, Iterable<DocumentFilter>> selectorExtractor, URI documentUri,
        String documentLanguageId)
    {
        NavigableMap<Integer, List<T>> result = new TreeMap<>(Comparator.reverseOrder());
        for (T candidate : candidates)
        {
            int score = match(selectorExtractor.apply(candidate), documentUri, documentLanguageId);
            if (score > 0)
                result.computeIfAbsent(score, k -> new ArrayList<>()).add(candidate);
        }
        return result;
    }
}
