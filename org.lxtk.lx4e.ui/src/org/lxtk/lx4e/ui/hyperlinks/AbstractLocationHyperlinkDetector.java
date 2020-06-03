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

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

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
import org.lxtk.lx4e.Request;
import org.lxtk.lx4e.internal.ui.Activator;
import org.lxtk.lx4e.util.DefaultWordFinder;

/**
 * Partial implementation of a hyperlink detector that computes a list
 * of {@link Location}s corresponding to a given document range and creates
 * hyperlinks for each of the computed locations.
 */
public abstract class AbstractLocationHyperlinkDetector
    extends AbstractHyperlinkDetector
{
    private static final IHyperlink[] NO_HYPERLINKS = new IHyperlink[0];

    @Override
    public IHyperlink[] detectHyperlinks(ITextViewer textViewer, IRegion region,
        boolean canShowMultipleHyperlinks)
    {
        LanguageOperationTarget target = getAdapter(LanguageOperationTarget.class);
        if (target == null)
            return null;

        IDocument document = textViewer.getDocument();

        Position position;
        try
        {
            position = DocumentUtil.toPosition(document, region.getOffset());
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

        List<IHyperlink> hyperlinks;
        if (result.isLeft())
        {
            List<? extends Location> locations = result.getLeft();
            if (locations.isEmpty())
                return null;
            IRegion hyperlinkRegion = findWord(document, region.getOffset());
            if (hyperlinkRegion == null)
                hyperlinkRegion = region;
            int size = locations.size();
            int index = size > 1 ? 0 : -1;
            hyperlinks = new ArrayList<>(size);
            for (Location location : locations)
            {
                hyperlinks.add(createHyperlink(hyperlinkRegion, location, index++));
            }
        }
        else if (result.isRight())
        {
            List<? extends LocationLink> links = result.getRight();
            if (links.isEmpty())
                return null;
            int size = links.size();
            int index = size > 1 ? 0 : -1;
            IRegion wordRegion = null;
            hyperlinks = new ArrayList<>(size);
            for (LocationLink link : links)
            {
                IRegion hyperlinkRegion = null;
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
                if (hyperlinkRegion == null)
                {
                    if (wordRegion == null)
                        wordRegion = findWord(document, region.getOffset());
                    if (wordRegion == null)
                        wordRegion = region;
                    hyperlinkRegion = wordRegion;
                }
                hyperlinks.add(createHyperlink(hyperlinkRegion,
                    new Location(link.getTargetUri(), link.getTargetSelectionRange()), index++));
            }
        }
        else
            return null;
        return hyperlinks.toArray(NO_HYPERLINKS);
    }

    /**
     * Creates and returns a request for computing hyperlink locations
     * at the given document position.
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
     * Returns the timeout for computing hyperlink locations.
     *
     * @return a positive duration
     */
    protected Duration getHyperlinkTimeout()
    {
        return Duration.ofSeconds(1);
    }
}
