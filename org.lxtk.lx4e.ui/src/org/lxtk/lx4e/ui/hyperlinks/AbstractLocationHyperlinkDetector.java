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
package org.lxtk.lx4e.ui.hyperlinks;

import java.text.MessageFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

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
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.lxtk.LanguageOperationTarget;
import org.lxtk.lx4e.DocumentUtil;
import org.lxtk.lx4e.internal.ui.Activator;
import org.lxtk.lx4e.requests.Request;
import org.lxtk.lx4e.util.DefaultWordFinder;

/**
 * Partial implementation of a hyperlink detector that computes a list
 * of {@link Location}s corresponding to a given document range and creates
 * hyperlinks for the computed locations.
 */
public abstract class AbstractLocationHyperlinkDetector
    extends AbstractHyperlinkDetector
{
    @Override
    public IHyperlink[] detectHyperlinks(ITextViewer textViewer, IRegion region,
        boolean canShowMultipleHyperlinks)
    {
        LanguageOperationTarget target = getAdapter(LanguageOperationTarget.class);
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

        Request<Either<List<? extends Location>, List<? extends LocationLink>>> request =
            createHyperlinkRequest(target, position);
        if (request == null)
            return null;

        request.setTimeout(getHyperlinkTimeout());
        request.setMayThrow(false);

        Either<List<? extends Location>, List<? extends LocationLink>> result =
            request.sendAndReceive();
        if (result == null)
            return null;

        return createHyperlinks(result, textViewer, region, canShowMultipleHyperlinks);
    }

    private IHyperlink[] createHyperlinks(
        Either<List<? extends Location>, List<? extends LocationLink>> locationsOrLinks,
        ITextViewer textViewer, IRegion region, boolean canShowMultipleHyperlinks)
    {
        IDocument document = textViewer.getDocument();
        IRegion hyperlinkRegion = null;
        List<? extends Location> locations;
        if (locationsOrLinks.isLeft())
        {
            locations = locationsOrLinks.getLeft();
            if (locations.isEmpty())
                return null;
        }
        else if (locationsOrLinks.isRight())
        {
            List<? extends LocationLink> links = locationsOrLinks.getRight();
            if (links.isEmpty())
                return null;
            List<Location> locationList = new ArrayList<>(links.size());
            for (LocationLink link : links)
            {
                if (hyperlinkRegion == null)
                {
                    Range originSelectionRange = link.getOriginSelectionRange();
                    if (originSelectionRange != null)
                    {
                        try
                        {
                            hyperlinkRegion = DocumentUtil.toRegion(document, originSelectionRange);
                        }
                        catch (BadLocationException e)
                        {
                            Activator.logWarning(e);
                        }
                    }
                }
                locationList.add(new Location(link.getTargetUri(), link.getTargetSelectionRange()));
            }
            locations = locationList;
        }
        else
            return null;

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
     */
    protected abstract Request<
        Either<List<? extends Location>, List<? extends LocationLink>>> createHyperlinkRequest(
            LanguageOperationTarget target, Position position);

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
}
