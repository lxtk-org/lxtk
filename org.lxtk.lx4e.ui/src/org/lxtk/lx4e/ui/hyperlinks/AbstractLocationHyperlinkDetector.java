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
package org.lxtk.lx4e.ui.hyperlinks;

import java.text.MessageFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.hyperlink.AbstractHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.lxtk.DocumentUri;
import org.lxtk.LanguageOperationTarget;
import org.lxtk.lx4e.DocumentUtil;
import org.lxtk.lx4e.internal.ui.Activator;
import org.lxtk.lx4e.internal.ui.TaskExecutor;
import org.lxtk.lx4e.requests.Request;
import org.lxtk.lx4e.util.DefaultWordFinder;

/**
 * Partial implementation of a hyperlink detector that computes a list
 * of {@link Location}s corresponding to a given document range and creates
 * hyperlinks for the computed locations.
 *
 * @param <LocationLinkProvider> the type of a location link provider
 */
public abstract class AbstractLocationHyperlinkDetector<LocationLinkProvider>
    extends AbstractHyperlinkDetector
{
    @Override
    public IHyperlink[] detectHyperlinks(ITextViewer textViewer, IRegion region,
        boolean canShowMultipleHyperlinks)
    {
        LanguageOperationTarget target = getLanguageOperationTarget();
        if (target == null)
            return null;

        Position position;
        try
        {
            position = DocumentUtil.toPosition(textViewer.getDocument(), region.getOffset());
        }
        catch (BadLocationException e)
        {
            Activator.logError(e);
            return null;
        }

        LocationLinkResults<LocationLinkProvider> results =
            computeLocationLinkResults(getLocationLinkProviders(target), new LocationLinkContext(
                DocumentUri.toTextDocumentIdentifier(target.getDocumentUri()), position));
        if (results == null)
            return null;

        return createHyperlinks(results, textViewer, region, canShowMultipleHyperlinks);
    }

    /**
     * Returns the current {@link LanguageOperationTarget}.
     *
     * @return the language operation target, or <code>null</code> if none
     */
    public LanguageOperationTarget getLanguageOperationTarget()
    {
        return getAdapter(LanguageOperationTarget.class);
    }

    /**
     * Returns location link providers that match the given target.
     *
     * @param target never <code>null</code>
     * @return the matching location link providers (not <code>null</code>)
     */
    protected abstract LocationLinkProvider[] getLocationLinkProviders(
        LanguageOperationTarget target);

    /**
     * Asks each of the given location link providers to compute a result for the given context
     * and returns the computed results.
     *
     * @param providers never <code>null</code>
     * @param context never <code>null</code>
     * @return the computed location link results, or <code>null</code> if none
     */
    protected LocationLinkResults<LocationLinkProvider> computeLocationLinkResults(
        LocationLinkProvider[] providers, LocationLinkContext context)
    {
        if (providers.length == 0)
            return null;

        return new LocationLinkResults<>(TaskExecutor.parallelCompute(providers,
            (provider, monitor) -> computeLocationLinkResult(provider, context, monitor),
            Messages.Computing_links, getHyperlinkTimeout(), null));
    }

    /**
     * Asks the given location link provider to compute a result for the given context
     * and returns the computed result.
     *
     * @param provider never <code>null</code>
     * @param context never <code>null</code>
     * @param monitor a progress monitor, or <code>null</code>
     *  if progress reporting is not desired. The caller must not rely on
     *  {@link IProgressMonitor#done()} having been called by the receiver
     * @return the computed location link result, or <code>null</code> if none
     */
    protected abstract LocationLinkResult computeLocationLinkResult(LocationLinkProvider provider,
        LocationLinkContext context, IProgressMonitor monitor);

    /**
     * Creates and returns hyperlinks for the given location link results that have been computed
     * for the given region in the given text viewer.
     *
     * @param results never <code>null</code>
     * @param textViewer the text viewer on which the hover popup should be shown
     *  (never <code>null</code>)
     * @param region the hyperlink region (never <code>null</code>)
     * @param canShowMultipleHyperlinks tells whether the caller is able to show multiple hyperlinks
     * @return the created hyperlinks, or <code>null</code> if none
     */
    protected IHyperlink[] createHyperlinks(LocationLinkResults<LocationLinkProvider> results,
        ITextViewer textViewer, IRegion region, boolean canShowMultipleHyperlinks)
    {
        IDocument document = textViewer.getDocument();
        IRegion hyperlinkRegion = null;
        List<Location> locations = new ArrayList<>();

        for (Map.Entry<LocationLinkProvider, LocationLinkResult> entry : results.asMap().entrySet())
        {
            LocationLinkResult result = entry.getValue();
            if (result != null)
            {
                Either<List<? extends Location>, List<? extends LocationLink>> locationsOrLinks =
                    result.getLocationsOrLinks();
                if (locationsOrLinks != null)
                {
                    if (locationsOrLinks.isLeft())
                    {
                        locations.addAll(locationsOrLinks.getLeft());
                    }
                    else if (locationsOrLinks.isRight())
                    {
                        for (LocationLink link : locationsOrLinks.getRight())
                        {
                            if (hyperlinkRegion == null)
                            {
                                Range originSelectionRange = link.getOriginSelectionRange();
                                if (originSelectionRange != null)
                                {
                                    try
                                    {
                                        hyperlinkRegion =
                                            DocumentUtil.toRegion(document, originSelectionRange);
                                    }
                                    catch (BadLocationException e)
                                    {
                                        Activator.logWarning(e);
                                    }
                                }
                            }
                            locations.add(
                                new Location(link.getTargetUri(), link.getTargetSelectionRange()));
                        }
                    }
                }
            }
        }

        if (hyperlinkRegion == null)
            hyperlinkRegion = findWord(document, region.getOffset());
        if (hyperlinkRegion == null)
            hyperlinkRegion = region;

        return createHyperlinks(textViewer, hyperlinkRegion, canShowMultipleHyperlinks, locations);
    }

    /**
     * Creates and returns hyperlinks for the given locations that have been computed
     * for the given region in the given text viewer.
     *
     * @param textViewer the text viewer on which the hover popup should be shown
     *  (never <code>null</code>)
     * @param region the hyperlink region (never <code>null</code>)
     * @param canShowMultipleHyperlinks tells whether the caller is able to show multiple hyperlinks
     * @param locations the computed locations (never <code>null</code>)
     * @return the created hyperlinks, or <code>null</code> if none
     */
    protected IHyperlink[] createHyperlinks(ITextViewer textViewer, IRegion region,
        boolean canShowMultipleHyperlinks, List<? extends Location> locations)
    {
        if (locations.isEmpty())
            return null;

        int size = locations.size();
        if (size == 1)
            return new IHyperlink[] { createHyperlink(region, locations.get(0), -1) };

        IHyperlink hyperlink = createHyperlink(textViewer, region, locations);
        if (hyperlink != null)
            return new IHyperlink[] { hyperlink };

        if (!canShowMultipleHyperlinks)
            return null;

        IHyperlink[] hyperlinks = new IHyperlink[size];
        int index = 0;
        for (Location location : locations)
        {
            hyperlinks[index] = createHyperlink(region, location, index);
            index++;
        }
        return hyperlinks;
    }

    /**
     * Creates and returns a hyperlink request for the given document position.
     *
     * @param target never <code>null</code>
     * @param position never <code>null</code>
     * @return the created request object, or <code>null</code> if none
     * @deprecated This formerly abstract method is no longer used by the framework
     *  and will be removed in a next release.
     */
    @Deprecated
    protected Request<
        Either<List<? extends Location>, List<? extends LocationLink>>> createHyperlinkRequest(
            LanguageOperationTarget target, Position position)
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Creates and returns a hyperlink that covers the given region and
     * opens the given location.
     *
     * @param region the hyperlink region (never <code>null</code>)
     * @param location the target location for the hyperlink
     *  (never <code>null</code>)
     * @param index 0-based index in the list of two or more hyperlinks,
     *  or -1 if there is only one hyperlink
     * @return the created hyperlink (not <code>null</code>)
     */
    protected abstract IHyperlink createHyperlink(IRegion region, Location location, int index);

    /**
     * Creates and returns a hyperlink that covers the given region in the given text viewer and
     * can show additional UI that allows the user to select from the given list of locations.
     *
     * @param textViewer the text viewer on which the hover popup should be shown
     *  (never <code>null</code>)
     * @param region the hyperlink region (never <code>null</code>)
     * @param locations a list of locations (never <code>null</code>)
     * @return the created hyperlink, or <code>null</code> if none
     */
    protected IHyperlink createHyperlink(ITextViewer textViewer, IRegion region,
        List<? extends Location> locations)
    {
        return null;
    }

    /**
     * Returns a human-readable string that describes as a whole the list of locations computed
     * for the given region in the given text viewer.
     *
     * @param textViewer the text viewer on which the hover popup should be shown
     *  (never <code>null</code>)
     * @param region the hyperlink region (never <code>null</code>)
     * @param locations a list of locations (never <code>null</code>)
     * @return the search result label (not <code>null</code>)
     */
    protected String getResultLabel(ITextViewer textViewer, IRegion region,
        List<? extends Location> locations)
    {
        IDocument document = textViewer.getDocument();
        String textAtRegion;
        try
        {
            textAtRegion = document.get(region.getOffset(), region.getLength());
        }
        catch (BadLocationException e)
        {
            Activator.logError(e);
            return textAtRegion = ""; //$NON-NLS-1$
        }

        LanguageOperationTarget target = getAdapter(LanguageOperationTarget.class);
        if (target == null) // should never happen
            return MessageFormat.format("''{0}''", textAtRegion); //$NON-NLS-1$

        String fileName = new Path(target.getDocumentUri().getPath()).lastSegment();
        int line = -1, column = -1;
        try
        {
            Position position = DocumentUtil.toPosition(document, region.getOffset());
            line = position.getLine();
            column =
                DocumentUtil.getColumn(document, position, textViewer.getTextWidget().getTabs());
        }
        catch (BadLocationException e)
        {
            Activator.logError(e);
        }
        return MessageFormat.format(Messages.AbstractLocationHyperlinkDetector_Result_label,
            textAtRegion, fileName, line + 1, column + 1);
    }

    /**
     * Returns the region of the word enclosing the given document offset.
     *
     * @param document never <code>null</code>
     * @param offset 0-based
     * @return the corresponding word region, or <code>null</code> if none
     */
    protected IRegion findWord(IDocument document, int offset)
    {
        return DefaultWordFinder.INSTANCE.findWord(document, offset);
    }

    /**
     * Returns the timeout for a hyperlink request.
     *
     * @return a positive duration
     */
    protected Duration getHyperlinkTimeout()
    {
        return Duration.ofSeconds(1);
    }

    /**
     * Represents the context of a location link request.
     */
    protected static class LocationLinkContext
    {
        private final TextDocumentIdentifier textDocument;
        private final Position position;

        /**
         * Constructor.
         *
         * @param textDocument not <code>null</code>
         * @param position not <code>null</code>
         */
        public LocationLinkContext(TextDocumentIdentifier textDocument, Position position)
        {
            this.textDocument = Objects.requireNonNull(textDocument);
            this.position = Objects.requireNonNull(position);
        }

        /**
         * Returns the text document.
         *
         * @return the text document (never <code>null</code>)
         */
        public TextDocumentIdentifier getTextDocument()
        {
            return textDocument;
        }

        /**
         * Returns the position in the text document.
         *
         * @return the text document position (never <code>null</code>)
         */
        public Position getPosition()
        {
            return position;
        }
    }

    /**
     * Represents a group of {@link LocationLinkResult}s.
     *
     * @param <LocationLinkProvider> the type of a location link provider
     */
    protected static class LocationLinkResults<LocationLinkProvider>
    {
        private final Map<LocationLinkProvider, LocationLinkResult> results;

        /**
         * Constructor.
         *
         * @param results not <code>null</code>
         */
        public LocationLinkResults(Map<LocationLinkProvider, LocationLinkResult> results)
        {
            this.results = Objects.requireNonNull(results);
        }

        /**
         * Returns the location link results as a map.
         *
         * @return the location link results as a map (never <code>null</code>)
         */
        public Map<LocationLinkProvider, LocationLinkResult> asMap()
        {
            return results;
        }
    }

    /**
     * Represents the result of a location link request.
     */
    protected static class LocationLinkResult
    {
        private final Either<List<? extends Location>,
            List<? extends LocationLink>> locationsOrLinks;

        /**
         * Constructor.
         *
         * @param locationsOrLinks may be <code>null</code>
         */
        public LocationLinkResult(
            Either<List<? extends Location>, List<? extends LocationLink>> locationsOrLinks)
        {
            this.locationsOrLinks = locationsOrLinks;
        }

        /**
         * Returns either the location list or the location link list.
         *
         * @return either the location list or the location link list, or <code>null</code> if none
         */
        public Either<List<? extends Location>, List<? extends LocationLink>> getLocationsOrLinks()
        {
            return locationsOrLinks;
        }
    }
}
