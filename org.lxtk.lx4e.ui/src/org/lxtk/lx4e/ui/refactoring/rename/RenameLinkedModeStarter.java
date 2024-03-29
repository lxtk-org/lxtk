/*******************************************************************************
 * Copyright (c) 2021, 2022 1C-Soft LLC and others.
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
import org.eclipse.jface.text.link.ILinkedModeListener;
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
import org.lxtk.jsonrpc.JsonUtil;
import org.lxtk.lx4e.DocumentUtil;
import org.lxtk.lx4e.internal.ui.LinkedEditingPubSub;
import org.lxtk.lx4e.internal.ui.TaskExecutor;
import org.lxtk.lx4e.requests.LinkedEditingRangeRequest;

/**
 * Starts linked editing mode for a local rename (does not change references in other files).
 * Uses {@link LinkedEditingRangeProvider}s to compute the linked editing ranges.
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

        Point originalSelection = viewer.getSelectedRange();
        int invocationOffset = originalSelection.x;

        LinkedEditingRangeParams params = new LinkedEditingRangeParams();
        params.setTextDocument(DocumentUri.toTextDocumentIdentifier(target.getDocumentUri()));
        params.setPosition(DocumentUtil.toPosition(document, invocationOffset));

        LinkedEditingRanges result = getLinkedEditingRanges(params);
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

        LinkedEditingPubSub.INSTANCE.fireLinkedEditingStarted(viewer);
        model.addLinkingListener(new ILinkedModeListener()
        {
            @Override
            public void left(LinkedModeModel model, int flags)
            {
                LinkedEditingPubSub.INSTANCE.fireLinkedEditingStopped(viewer);
            }

            @Override
            public void suspend(LinkedModeModel model)
            {
            }

            @Override
            public void resume(LinkedModeModel model, int flags)
            {
            }
        });

        return new RenameLinkedMode(viewer, model, group);
    }

    /**
     * Returns the linked editing ranges for the given {@link LinkedEditingRangeParams}.
     *
     * @param params never <code>null</code>
     * @return the corresponding {@link LinkedEditingRanges}, or <code>null</code> if none
     */
    protected LinkedEditingRanges getLinkedEditingRanges(LinkedEditingRangeParams params)
    {
        LinkedEditingRanges[] ranges = new LinkedEditingRanges[1];

        TaskExecutor.sequentialExecute(getLinkedEditingRangeProviders(target),
            (provider, timeout) ->
            {
                LinkedEditingRangeRequest request = newLinkedEditingRangeRequest();
                request.setProvider(provider);
                request.setParams(JsonUtil.deepCopy(params));
                request.setTimeout(timeout);
                request.setMayThrow(false);

                LinkedEditingRanges result = request.sendAndReceive();
                if (result == null || result.getRanges().isEmpty())
                    return false;

                ranges[0] = result;
                return true;

            }, getLinkedEditingRangeTimeout());

        return ranges[0];
    }

    /**
     * Returns the linked editing range providers for the given target.
     *
     * @param target never <code>null</code>
     * @return an array of linked editing range providers (not <code>null</code>,
     *  does not contain <code>null</code>s)
     */
    protected LinkedEditingRangeProvider[] getLinkedEditingRangeProviders(
        LanguageOperationTarget target)
    {
        LanguageService languageService = target.getLanguageService();
        return languageService.getDocumentMatcher().getSortedMatches(
            languageService.getLinkedEditingRangeProviders(),
            LinkedEditingRangeProvider::getDocumentSelector, target.getDocumentUri(),
            target.getLanguageId()).toArray(LinkedEditingRangeProvider[]::new);
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
