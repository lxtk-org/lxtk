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
package org.lxtk.lx4e.ui.completion;

import java.text.MessageFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.RewriteSessionEditProcessor;
import org.eclipse.jface.text.contentassist.BoldStylerProvider;
import org.eclipse.jface.text.contentassist.ContextInformation;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension2;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension3;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension5;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension6;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension7;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.jface.text.link.LinkedModeUI;
import org.eclipse.jface.text.link.LinkedPosition;
import org.eclipse.jface.text.link.LinkedPositionGroup;
import org.eclipse.jface.text.link.ProposalPosition;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.lsp4j.InsertReplaceEdit;
import org.eclipse.lsp4j.InsertTextFormat;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.MarkupKind;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.RangeMarker;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.undo.DocumentUndoManagerRegistry;
import org.eclipse.text.undo.IDocumentUndoManager;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.statushandlers.StatusManager;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;
import org.lxtk.CommandHandler;
import org.lxtk.CommandService;
import org.lxtk.CompletionProvider;
import org.lxtk.ProgressService;
import org.lxtk.WorkDoneProgress;
import org.lxtk.lx4e.DocumentUtil;
import org.lxtk.lx4e.internal.ui.Activator;
import org.lxtk.lx4e.internal.ui.FocusableInformationControlCreator;
import org.lxtk.lx4e.internal.ui.LSPImages;
import org.lxtk.lx4e.internal.ui.StyledBrowserInformationControlInput;
import org.lxtk.lx4e.requests.CompletionResolveRequest;
import org.lxtk.lx4e.ui.WorkDoneProgressFactory;
import org.lxtk.lx4e.util.Markdown;
import org.lxtk.util.completion.DefaultMatcher;
import org.lxtk.util.completion.MatchResult;
import org.lxtk.util.completion.snippet.Snippet;
import org.lxtk.util.completion.snippet.SnippetException;
import org.lxtk.util.completion.snippet.TabStop;

/**
 * Basic implementation of a completion proposal.
 */
public class BaseCompletionProposal
    implements IScoredCompletionProposal, ICompletionProposalExtension3,
    ICompletionProposalExtension5, ICompletionProposalExtension6, ICompletionProposalExtension7
{
    private static final TextEdit[] NO_EDITS = new TextEdit[0];
    private static final DefaultMatcher MATCHER = new DefaultMatcher();

    /** The given completion item. */
    protected final CompletionItem completionItem;
    /** The given completion context. */
    protected final CompletionContext completionContext;

    private IRegion selectedRegion;
    private IInformationControlCreator informationControlCreator;
    private CompletionItem resolvedCompletionItem;
    private MatchResultCache matchResultCache;

    /**
     * Constructor.
     *
     * @param completionItem not <code>null</code>
     * @param completionContext not <code>null</code>
     */
    public BaseCompletionProposal(CompletionItem completionItem,
        CompletionContext completionContext)
    {
        this.completionItem = Objects.requireNonNull(completionItem);
        this.completionContext = Objects.requireNonNull(completionContext);
    }

    @Override
    public void apply(IDocument document)
    {
        if (document != completionContext.getDocument())
            throw new IllegalArgumentException();

        try
        {
            apply(completionContext.getTextViewer(), '\0', 0,
                completionContext.getInvocationOffset());
        }
        catch (BadLocationException e)
        {
            Activator.logError(e);
        }
    }

    @Override
    public Point getSelection(IDocument document)
    {
        if (selectedRegion == null)
            return null;
        return new Point(selectedRegion.getOffset(), selectedRegion.getLength());
    }

    @Override
    public String getAdditionalProposalInfo()
    {
        Object info = getAdditionalProposalInfo(new NullProgressMonitor());
        return info == null ? null : info.toString();
    }

    @Override
    public Object getAdditionalProposalInfo(IProgressMonitor monitor)
    {
        Either<String, MarkupContent> documentation = completionItem.getDocumentation();
        if (documentation == null)
            documentation = getResolvedCompletionItem(monitor).getDocumentation();
        if (documentation == null)
            return null;

        MarkupContent markupContent = documentation.isLeft()
            ? new MarkupContent(MarkupKind.PLAINTEXT, documentation.getLeft())
            : documentation.getRight();
        if (markupContent == null)
            return null;
        return toAdditionalProposalInfo(markupContent);
    }

    @Override
    public IInformationControlCreator getInformationControlCreator()
    {
        if (informationControlCreator == null)
            informationControlCreator = newInformationControlCreator();
        return informationControlCreator;
    }

    @Override
    public String getDisplayString()
    {
        return completionItem.getLabel();
    }

    @Override
    public StyledString getStyledDisplayString()
    {
        return new StyledString(getDisplayString());
    }

    @Override
    public StyledString getStyledDisplayString(IDocument document, int offset,
        BoldStylerProvider boldStylerProvider)
    {
        StyledString styledDisplayString = new StyledString();
        styledDisplayString.append(getStyledDisplayString());

        String pattern = getPrefix(document, offset);
        MatchResult matchResult = getMatchResult(pattern, styledDisplayString.getString());
        if (matchResult != null)
            markMatchingRegions(styledDisplayString, 0, matchResult.getMatchingRegions(),
                boldStylerProvider.getBoldStyler());
        return styledDisplayString;
    }

    @Override
    public Image getImage()
    {
        return LSPImages.imageFromCompletionItem(completionItem);
    }

    @Override
    public IContextInformation getContextInformation()
    {
        String detail = completionItem.getDetail();
        if (detail == null)
            detail = getResolvedCompletionItem(null).getDetail();
        if (detail == null)
            return null;
        return new ContextInformation(getImage(), null, detail);
    }

    @Override
    public int getPrefixCompletionStart(IDocument document, int completionOffset)
    {
        return completionContext.getCurrentWordRegion().getOffset();
    }

    @Override
    public CharSequence getPrefixCompletionText(IDocument document, int completionOffset)
    {
        return getFilterString();
    }

    @Override
    public int getScore()
    {
        MatchResult matchResult = getMatchResult(getPrefix(completionContext.getDocument(),
            completionContext.getTextViewer().getSelectedRange().x), getFilterString());
        if (matchResult == null)
            return 0;
        return matchResult.getScore();
    }

    @Override
    public String getSortString()
    {
        String sortText = completionItem.getSortText();
        if (sortText == null)
            sortText = completionItem.getLabel();
        return sortText;
    }

    /**
     * Returns the filter string for this completion proposal.
     *
     * @return the filter string (never <code>null</code>)
     */
    protected String getFilterString()
    {
        String filterText = completionItem.getFilterText();
        if (filterText == null)
            filterText = completionItem.getLabel();
        return filterText;
    }

    /**
     * See {@link ICompletionProposalExtension2#apply(ITextViewer, char, int, int)}.
     *
     * @param viewer the text viewer into which to insert the proposed completion
     * @param trigger the trigger to apply the completion
     * @param stateMask the state mask of the modifiers
     * @param offset the offset at which the trigger has been activated
     * @throws BadLocationException if one of the edits of this proposal cannot be executed
     */
    protected void apply(ITextViewer viewer, char trigger, int stateMask, int offset)
        throws BadLocationException
    {
        IDocument document = viewer.getDocument();
        ProposedEdit edit = computeProposedEdit(viewer, trigger, stateMask, offset);
        MultiTextEdit editTree = new MultiTextEdit();
        TextEdit textEdit = !document.get(edit.replacementOffset, edit.replacementLength).equals(
            edit.replacementString)
                ? new ReplaceEdit(edit.replacementOffset, edit.replacementLength,
                    edit.replacementString)
                : new RangeMarker(edit.replacementOffset, edit.replacementLength);
        editTree.addChild(textEdit);
        editTree.addChildren(computeAdditionalEdits(document));
        applyEditTree(editTree, document);
        selectedRegion = new Region(textEdit.getOffset() + edit.cursorPosition, 0);
        executeCommand();
        setUpLinkedMode(viewer, textEdit.getOffset(), edit.cursorPosition, edit.tabStops);
    }

    /**
     * Returns the delta for adjusting the proposal offset in response to document changes.
     *
     * @return the editing delta
     */
    protected int getEditingDelta()
    {
        return 0;
    }

    /**
     * Returns the prefix in the <code>document</code> for the given <code>offset</code>.
     *
     * @param document never <code>null</code>
     * @param offset a zero-based offset
     * @return the prefix (not <code>null</code>, may be empty)
     */
    protected String getPrefix(IDocument document, int offset)
    {
        IRegion region = completionContext.getCurrentWordRegion();
        try
        {
            if (region.getOffset() < offset)
                return document.get(region.getOffset(), offset - region.getOffset());
        }
        catch (BadLocationException x)
        {
        }
        return ""; //$NON-NLS-1$
    }

    /**
     * Returns the result of matching the given string against the given pattern.
     * <p>
     * Delegates to {@link #computeMatchResult(String, String)} for computing the match result
     * if not already computed.
     * </p>
     *
     * @param pattern not <code>null</code>
     * @param string not <code>null</code>
     * @return the match result if there is a match, or <code>null</code> if there is no match
     */
    protected final MatchResult getMatchResult(String pattern, String string)
    {
        if (pattern.isEmpty() || pattern.length() > string.length())
            return null;
        if (matchResultCache != null && pattern.equals(matchResultCache.pattern)
            && string.equals(matchResultCache.string))
            return matchResultCache.result;
        MatchResult result = computeMatchResult(pattern, string);
        matchResultCache = new MatchResultCache(pattern, string, result);
        return result;
    }

    /**
     * Computes the result of matching the given string against the given pattern.
     *
     * @param pattern not <code>null</code>
     * @param string not <code>null</code>
     * @return the match result if there is a match, or <code>null</code> if there is no match
     */
    protected MatchResult computeMatchResult(String pattern, String string)
    {
        return MATCHER.match(pattern, string);
    }

    /**
     * Sets the given <code>styler</code> to use for <code>matchingRegions</code> (obtained from
     * {@link MatchResult#getMatchingRegions()}) in the <code>styledString</code> starting
     * from the given <code>index</code>.
     *
     * @param styledString the styled string to mark (not <code>null</code>)
     * @param index the index from which to start marking
     * @param matchingRegions the regions to mark (not <code>null</code>)
     * @param styler the styler to use for marking (not <code>null</code>)
     */
    protected static void markMatchingRegions(StyledString styledString, int index,
        int[] matchingRegions, Styler styler)
    {
        int offset = -1;
        int length = 0;
        for (int i = 0; i + 1 < matchingRegions.length; i = i + 2)
        {
            if (offset == -1)
                offset = index + matchingRegions[i];

            // Concatenate adjacent regions
            if (i + 2 < matchingRegions.length
                && matchingRegions[i] + matchingRegions[i + 1] == matchingRegions[i + 2])
            {
                length = length + matchingRegions[i + 1];
            }
            else
            {
                styledString.setStyle(offset, length + matchingRegions[i + 1], styler);
                offset = -1;
                length = 0;
            }
        }
    }

    /**
     * Returns a new instance of the information control creator for this proposal.
     *
     * @return a new information control creator (not <code>null</code>)
     * @see #getInformationControlCreator()
     */
    protected IInformationControlCreator newInformationControlCreator()
    {
        return new FocusableInformationControlCreator()
        {
            @Override
            protected String getTooltipAffordanceString()
            {
                return EditorsUI.getPreferenceStore().getBoolean(
                    AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SHOW_TEXT_HOVER_AFFORDANCE)
                        ? Messages.Tooltip_affordance_string : null;
            }
        };
    }

    /**
     * Given a {@link MarkupContent}, returns additional information about the proposal.
     *
     * @param markupContent never <code>null</code>
     * @return the additional information or <code>null</code>
     */
    protected Object toAdditionalProposalInfo(MarkupContent markupContent)
    {
        String value = markupContent.getValue();
        if (value.isBlank())
            return null;
        String html = Markdown.toHtml(value, false);
        return StyledBrowserInformationControlInput.of(html);
    }

    /**
     * Returns the result of resolving the completion item of this proposal.
     * If resolving is not possible or has failed, returns the completion item itself.
     *
     * @param monitor a progress monitor or <code>null</code>
     * @return the resolved completion item (never <code>null</code>)
     */
    protected final synchronized CompletionItem getResolvedCompletionItem(IProgressMonitor monitor)
    {
        if (resolvedCompletionItem == null)
            resolvedCompletionItem = resolveCompletionItem(monitor);
        return resolvedCompletionItem;
    }

    /**
     * Returns a new instance of {@link CompletionResolveRequest}.
     *
     * @return the created request object (not <code>null</code>)
     */
    protected CompletionResolveRequest newCompletionResolveRequest()
    {
        return new CompletionResolveRequest();
    }

    /**
     * Returns the timeout for a completion resolve request.
     *
     * @return a positive duration
     */
    protected Duration getCompletionResolveTimeout()
    {
        return Duration.ofSeconds(1);
    }

    private CompletionItem resolveCompletionItem(IProgressMonitor monitor)
    {
        CompletionProvider completionProvider = completionContext.getCompletionProvider();
        if (!Boolean.TRUE.equals(completionProvider.getRegistrationOptions().getResolveProvider()))
            return completionItem;

        CompletionResolveRequest request = newCompletionResolveRequest();
        request.setProvider(completionProvider);
        request.setParams(completionItem);
        request.setDefaultResult(completionItem);
        request.setProgressMonitor(monitor);
        request.setTimeout(getCompletionResolveTimeout());
        request.setMayThrow(false);
        return request.sendAndReceive();
    }

    private ProposedEdit computeProposedEdit(ITextViewer viewer, char trigger, int stateMask,
        int offset) throws BadLocationException
    {
        IDocument document = viewer.getDocument();
        Point selectedRange = viewer.getSelectedRange();
        boolean overwrite = completionContext.isOverwriteMode() ^ ((stateMask & SWT.CTRL) != 0);
        String replacementString;
        int replacementOffset, replacementEndOffset;
        Either<org.eclipse.lsp4j.TextEdit, InsertReplaceEdit> textEditOrInsertReplaceEdit =
            completionItem.getTextEdit();
        if (textEditOrInsertReplaceEdit == null)
        {
            replacementString = completionItem.getInsertText();
            if (replacementString == null)
                replacementString = completionItem.getLabel();
            IRegion wordRegion = completionContext.getCurrentWordRegion();
            replacementOffset = replacementString.startsWith(getFilterString())
                ? wordRegion.getOffset() : completionContext.getInvocationOffset();
            replacementEndOffset = overwrite ? wordRegion.getOffset() + wordRegion.getLength()
                : selectedRange.x + selectedRange.y;
        }
        else if (textEditOrInsertReplaceEdit.isLeft())
        {
            org.eclipse.lsp4j.TextEdit textEdit = textEditOrInsertReplaceEdit.getLeft();
            replacementString = textEdit.getNewText();
            replacementOffset = DocumentUtil.toOffset(document, textEdit.getRange().getStart());
            if (!overwrite)
                replacementEndOffset = selectedRange.x + selectedRange.y;
            else
            {
                IRegion wordRegion = completionContext.getCurrentWordRegion();
                replacementEndOffset = wordRegion.getOffset() + wordRegion.getLength();
            }
        }
        else // textEditOrInsertReplaceEdit.isRight()
        {
            InsertReplaceEdit insertReplaceEdit = textEditOrInsertReplaceEdit.getRight();
            replacementString = insertReplaceEdit.getNewText();
            replacementOffset =
                DocumentUtil.toOffset(document, insertReplaceEdit.getInsert().getStart()); // insertReplaceEdit.getInsert().getStart() must be equal to insertReplaceEdit.getReplace().getStart()
            if (!overwrite)
                replacementEndOffset = selectedRange.x + selectedRange.y;
            else
            {
                org.eclipse.lsp4j.Position position = insertReplaceEdit.getReplace().getEnd();
                IRegion line = document.getLineInformation(position.getLine());
                int offsetInLine = position.getCharacter() + getEditingDelta();
                if (offsetInLine < 0)
                    offsetInLine = 0;
                else if (offsetInLine > line.getLength())
                    offsetInLine = line.getLength();
                replacementEndOffset = line.getOffset() + offsetInLine;
            }
        }
        List<TabStop> tabStops = Collections.emptyList();
        if (InsertTextFormat.Snippet.equals(completionItem.getInsertTextFormat()))
        {
            try
            {
                Snippet snippet =
                    Snippet.parse(replacementString, completionContext::resolveVariable);
                replacementString = snippet.getText();
                tabStops = Arrays.asList(snippet.getTabStops());
            }
            catch (SnippetException e)
            {
                Activator.logError(e);
            }
        }
        int cursorPosition = replacementString.length();
        if (!tabStops.isEmpty() && tabStops.get(0).getId().equals("0")) //$NON-NLS-1$
        {
            cursorPosition = tabStops.get(0).getOffsets()[0]; // exit offset
            tabStops = tabStops.subList(1, tabStops.size()); // exclude $0 from tabStops
        }
        if (trigger != '\0' && !replacementString.endsWith(String.valueOf(trigger)))
        {
            if (cursorPosition == replacementString.length())
                ++cursorPosition;

            replacementString += trigger;
        }
        return new ProposedEdit(replacementOffset, replacementEndOffset - replacementOffset,
            replacementString, cursorPosition, tabStops);
    }

    private TextEdit[] computeAdditionalEdits(IDocument document)
    {
        List<org.eclipse.lsp4j.TextEdit> edits = completionItem.getAdditionalTextEdits();
        if (edits == null || edits.isEmpty())
            return NO_EDITS;
        int editingDelta = getEditingDelta();
        List<TextEdit> result = new ArrayList<>(edits.size());
        for (org.eclipse.lsp4j.TextEdit edit : edits)
        {
            try
            {
                IRegion r = DocumentUtil.toRegion(document, edit.getRange());
                int offset = r.getOffset();
                if (offset > completionContext.getInvocationOffset())
                    offset += editingDelta;
                result.add(new ReplaceEdit(offset, r.getLength(), edit.getNewText()));
            }
            catch (BadLocationException e)
            {
                Activator.logError(e);
            }
        }
        return result.toArray(NO_EDITS);
    }

    private static void applyEditTree(MultiTextEdit editTree, IDocument document)
        throws BadLocationException
    {
        RewriteSessionEditProcessor editProcessor =
            new RewriteSessionEditProcessor(document, editTree, TextEdit.UPDATE_REGIONS);
        IDocumentUndoManager undoManager =
            DocumentUndoManagerRegistry.getDocumentUndoManager(document);
        if (undoManager != null)
            undoManager.beginCompoundChange();
        try
        {
            editProcessor.performEdits();
        }
        finally
        {
            if (undoManager != null)
                undoManager.endCompoundChange();
        }
    }

    private void executeCommand()
    {
        Command command = completionItem.getCommand();
        if (command == null)
            return;

        CommandService commandService =
            completionContext.getCompletionProvider().getCommandService();
        if (commandService == null)
            return;

        CommandHandler commandHandler = commandService.getCommandHandler(command.getCommand());
        if (commandHandler == null)
            return;

        ExecuteCommandParams params =
            new ExecuteCommandParams(command.getCommand(), command.getArguments());

        WorkDoneProgress workDoneProgress = null;
        if (Boolean.TRUE.equals(commandHandler.getRegistrationOptions().getWorkDoneProgress()))
        {
            ProgressService progressService = commandHandler.getProgressService();
            if (progressService != null)
            {
                workDoneProgress = WorkDoneProgressFactory.newWorkDoneProgressWithJob(false);
                progressService.attachProgress(workDoneProgress);
                params.setWorkDoneToken(workDoneProgress.getToken());
            }
        }

        CompletableFuture<Object> future = commandHandler.execute(params);

        if (workDoneProgress != null)
            workDoneProgress.connectWith(future);

        future.exceptionally(e ->
        {
            e = Activator.unwrap(e);
            if (!Activator.isCancellation(e))
            {
                StatusManager.getManager().handle(
                    Activator.createErrorStatus(
                        MessageFormat.format(Messages.Execution_error, command.getTitle()), e),
                    StatusManager.LOG);
            }
            return null;
        });
    }

    private void setUpLinkedMode(ITextViewer viewer, int start, int exitOffset,
        List<TabStop> tabStops) throws BadLocationException
    {
        List<LinkedPositionGroup> groups =
            getLinkedPositionGroups(viewer.getDocument(), start, tabStops);
        if (groups.isEmpty())
            return;
        LinkedModeModel model = new LinkedModeModel();
        for (LinkedPositionGroup group : groups)
        {
            try
            {
                model.addGroup(group);
            }
            catch (BadLocationException e)
            {
                // ignore: non-disjoint (e.g. nested) tabstops are currently not supported
            }
        }
        model.forceInstall();
        LinkedModeUI ui = new LinkedModeUI(model, viewer);
        ui.setExitPosition(viewer, start + exitOffset, 0, Integer.MAX_VALUE);
        ui.enter();
        selectedRegion = ui.getSelectedRegion();
    }

    private List<LinkedPositionGroup> getLinkedPositionGroups(IDocument document, int start,
        List<TabStop> tabStops) throws BadLocationException
    {
        List<LinkedPositionGroup> result = new ArrayList<>(tabStops.size());
        int sequenceNumber = 0;
        for (TabStop tabStop : tabStops)
        {
            LinkedPositionGroup group = new LinkedPositionGroup();
            List<LinkedPosition> positions =
                getLinkedPositions(document, start, tabStop, sequenceNumber++);
            for (LinkedPosition position : positions)
            {
                group.addPosition(position);
            }
            result.add(group);
        }
        return result;
    }

    private List<LinkedPosition> getLinkedPositions(IDocument document, int start, TabStop tabStop,
        int sequenceNumber)
    {
        int[] offsets = tabStop.getOffsets();
        int offsetsLength = offsets.length;
        String[] values = tabStop.getValues();
        int valuesLength = values.length;
        int length = valuesLength > 0 ? values[0].length() : 0;
        List<LinkedPosition> result = new ArrayList<>(offsetsLength);
        for (int i = 0; i < offsetsLength; i++)
        {
            result.add(i == 0 && valuesLength > 1
                ? new LazyProposalPosition(document, start + offsets[i], length, sequenceNumber,
                    values)
                : new LinkedPosition(document, start + offsets[i], length, sequenceNumber));
            sequenceNumber = LinkedPositionGroup.NO_STOP;
        }
        return result;
    }

    private static class MatchResultCache
    {
        final String pattern;
        final String string;
        final MatchResult result;

        MatchResultCache(String pattern, String string, MatchResult result)
        {
            this.pattern = pattern;
            this.string = string;
            this.result = result;
        }
    }

    private static class ProposedEdit
    {
        final int replacementOffset;
        final int replacementLength;
        final String replacementString;
        final int cursorPosition;
        final List<TabStop> tabStops;

        ProposedEdit(int replacementOffset, int replacementLength, String replacementString,
            int cursorPosition, List<TabStop> tabStops)
        {
            this.replacementOffset = replacementOffset;
            this.replacementLength = replacementLength;
            this.replacementString = replacementString;
            this.cursorPosition = cursorPosition;
            this.tabStops = Objects.requireNonNull(tabStops);
        }
    }

    private static class LazyProposalPosition
        extends ProposalPosition
    {
        private final String[] values;

        LazyProposalPosition(IDocument document, int offset, int length, int sequenceNumber,
            String[] values)
        {
            super(document, offset, length, sequenceNumber, null);
            this.values = Objects.requireNonNull(values);
        }

        @Override
        public ICompletionProposal[] getChoices()
        {
            int length = values.length;
            ICompletionProposal[] result = new ICompletionProposal[length];
            for (int i = 0; i < length; i++)
            {
                result[i] = new ChoiceProposal(values[i]);
            }
            return result;
        }

        private class ChoiceProposal
            implements ICompletionProposal
        {
            private final String value;

            ChoiceProposal(String value)
            {
                this.value = Objects.requireNonNull(value);
            }

            @Override
            public String getDisplayString()
            {
                return value;
            }

            @Override
            public void apply(IDocument document)
            {
                try
                {
                    replace(document, getOffset(), getLength(), value);
                }
                catch (BadLocationException e)
                {
                    Activator.logError(e); // should never happen
                }
            }

            private void replace(IDocument document, int offset, int length, String string)
                throws BadLocationException
            {
                if (!document.get(offset, length).equals(string))
                    document.replace(offset, length, string);
            }

            @Override
            public Point getSelection(IDocument document)
            {
                return null;
            }

            @Override
            public String getAdditionalProposalInfo()
            {
                return null;
            }

            @Override
            public Image getImage()
            {
                return null;
            }

            @Override
            public IContextInformation getContextInformation()
            {
                return null;
            }
        }
    }
}
