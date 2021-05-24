/*******************************************************************************
 * Copyright (c) 2021 1C-Soft LLC and others.
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
package org.lxtk.lx4e.ui.refactoring.rename;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.jface.text.link.LinkedModeUI;
import org.eclipse.jface.text.link.LinkedModeUI.ExitFlags;
import org.eclipse.jface.text.link.LinkedModeUI.IExitPolicy;
import org.eclipse.jface.text.link.LinkedPosition;
import org.eclipse.jface.text.link.LinkedPositionGroup;
import org.eclipse.lsp4j.LinkedEditingRangeParams;
import org.eclipse.lsp4j.LinkedEditingRanges;
import org.eclipse.lsp4j.Range;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.texteditor.link.EditorLinkedModeUI;
import org.lxtk.DocumentUri;
import org.lxtk.LanguageOperationTarget;
import org.lxtk.LanguageService;
import org.lxtk.LinkedEditingRangeProvider;
import org.lxtk.lx4e.DocumentUtil;
import org.lxtk.lx4e.requests.LinkedEditingRangeRequest;

/**
 * Starts linked editing mode for a local rename (does not change references in other files).
 * Uses {@link LinkedEditingRangeProvider} to compute the linked editing ranges.
 */
public class RenameLinkedModeStarter
{
    protected final LanguageOperationTarget target;

    /**
     * Constructor.
     *
     * @param target a {@link LanguageOperationTarget} (not <code>null</code>)
     */
    public RenameLinkedModeStarter(LanguageOperationTarget target)
    {
        this.target = Objects.requireNonNull(target);
    }

    /**
     * Starts linked editing mode for renaming the currently selected symbol
     * in the given text viewer.
     *
     * @param viewer not <code>null</code>
     * @param exitPolicy may be <code>null</code>
     * @return the started {@link RenameLinkedMode}, or <code>null</code> if not available
     * @throws BadLocationException if some of the linked editing ranges were not valid
     *  in the viewer's document
     */
    public RenameLinkedMode start(ITextViewer viewer, IExitPolicy exitPolicy)
        throws BadLocationException
    {
        IDocument document = viewer.getDocument();
        if (document == null)
            return null;

        LinkedEditingRangeProvider provider = getLinkedEditingRangeProvider();
        if (provider == null)
            return null;

        Point originalSelection = viewer.getSelectedRange();
        int invocationOffset = originalSelection.x;

        LinkedEditingRangeParams params = new LinkedEditingRangeParams();
        params.setTextDocument(DocumentUri.toTextDocumentIdentifier(target.getDocumentUri()));
        params.setPosition(DocumentUtil.toPosition(document, invocationOffset));

        LinkedEditingRangeRequest request = newLinkedEditingRangeRequest();
        request.setProvider(provider);
        request.setParams(params);
        request.setTimeout(getLinkedEditingRangeTimeout());
        request.setMayThrow(false);

        LinkedEditingRanges result = request.sendAndReceive();
        if (result == null || result.getRanges().isEmpty())
            return null;

        LinkedPositionGroup group = new LinkedPositionGroup();
        List<IRegion> regions = toRegions(document, result.getRanges());
        regions.sort(new Comparator<IRegion>()
        {
            @Override
            public int compare(IRegion o1, IRegion o2)
            {
                return rank(o1) - rank(o2);
            }

            private int rank(IRegion region)
            {
                int relativeRank = region.getOffset() + region.getLength() - invocationOffset;
                return relativeRank < 0 ? Integer.MAX_VALUE + relativeRank : relativeRank;
            }
        });
        for (int i = 0, size = regions.size(); i < size; i++)
        {
            IRegion r = regions.get(i);
            group.addPosition(new LinkedPosition(document, r.getOffset(), r.getLength(), i));
        }

        LinkedModeModel model = new LinkedModeModel();
        model.addGroup(group);
        model.forceInstall();

        LinkedModeUI ui = new EditorLinkedModeUI(model, viewer);
        ui.setExitPosition(viewer, invocationOffset, 0, Integer.MAX_VALUE);
        if (exitPolicy != null)
            ui.setExitPolicy(exitPolicy);
        ui.enter();
        viewer.setSelectedRange(originalSelection.x, originalSelection.y); // by default, full word is selected; restore original selection

        return new RenameLinkedMode(viewer, model, group);
    }

    /**
     * Returns the timeout for a linked editing range request.
     *
     * @return a positive duration
     */
    protected Duration getLinkedEditingRangeTimeout()
    {
        return Duration.ofSeconds(2);
    }

    /**
     * Returns a new instance of {@link LinkedEditingRangeRequest}.
     *
     * @return the created request object (not <code>null</code>)
     */
    protected LinkedEditingRangeRequest newLinkedEditingRangeRequest()
    {
        return new LinkedEditingRangeRequest();
    }

    /**
     * Returns the linked editing range provider.
     *
     * @return the linked editing range provider, or <code>null</code> if none is available
     */
    protected LinkedEditingRangeProvider getLinkedEditingRangeProvider()
    {
        LanguageService languageService = target.getLanguageService();
        return languageService.getDocumentMatcher().getBestMatch(
            languageService.getLinkedEditingRangeProviders(),
            LinkedEditingRangeProvider::getDocumentSelector, target.getDocumentUri(),
            target.getLanguageId());
    }

    private static List<IRegion> toRegions(IDocument document, List<Range> ranges)
        throws BadLocationException
    {
        List<IRegion> result = new ArrayList<>(ranges.size());
        for (Range range : ranges)
        {
            result.add(DocumentUtil.toRegion(document, range));
        }
        return result;
    }

    /**
     * An exit policy that skips Backspace and Delete at the beginning and at the end
     * of a linked position, respectively.
     * <p>
     * See https://bugs.eclipse.org/bugs/show_bug.cgi?id=183925.
     */
    public static class DeleteBlockingExitPolicy // Copied from org.eclipse.jdt.internal.ui.text.correction.proposals.LinkedNamesAssistProposal.DeleteBlockingExitPolicy
        implements IExitPolicy
    {
        protected final IDocument document;

        /**
         * Constructor.
         *
         * @param document not <code>null</code>
         */
        public DeleteBlockingExitPolicy(IDocument document)
        {
            this.document = Objects.requireNonNull(document);
        }

        @Override
        public ExitFlags doExit(LinkedModeModel model, VerifyEvent event, int offset, int length)
        {
            if (length == 0 && (event.character == SWT.BS || event.character == SWT.DEL))
            {
                LinkedPosition position = model.findPosition(
                    new LinkedPosition(document, offset, 0, LinkedPositionGroup.NO_STOP));
                if (position != null)
                {
                    if (event.character == SWT.BS)
                    {
                        if (offset - 1 < position.getOffset())
                        {
                            // skip backspace at beginning of linked position
                            event.doit = false;
                        }
                    }
                    else // event.character == SWT.DEL
                    {
                        if (offset + 1 > position.getOffset() + position.getLength())
                        {
                            // skip delete at end of linked position
                            event.doit = false;
                        }
                    }
                }
            }
            return null; // don't change behavior
        }
    }
}
