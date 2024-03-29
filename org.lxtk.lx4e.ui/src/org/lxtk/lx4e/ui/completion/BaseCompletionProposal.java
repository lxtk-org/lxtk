/*******************************************************************************
 * Copyright (c) 2021, 2022 1C-Soft LLC.
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
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.RewriteSessionEditProcessor;
import org.eclipse.jface.text.contentassist.BoldStylerProvider;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension2;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension3;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension5;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension6;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension7;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemLabelDetails;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.lsp4j.InsertReplaceEdit;
import org.eclipse.lsp4j.InsertTextFormat;
import org.eclipse.lsp4j.InsertTextMode;
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
import org.lxtk.CompletionItemUtil;
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
 * Basic implementation of a {@link CompletionItem}-based completion proposal.
 */
public class BaseCompletionProposal
    implements IScoredCompletionProposal, ICompletionProposalExtension3,
    ICompletionProposalExtension5, ICompletionProposalExtension6, ICompletionProposalExtension7
{
    private static final TextEdit[] NO_EDITS = new TextEdit[0];
    private static final DefaultMatcher MATCHER = new DefaultMatcher();

    /** The given completion item (never <code>null</code>) */
    protected final CompletionItem completionItem;
    /** The given completion list (never <code>null</code>) */
    protected final CompletionList completionList;
    /** The given completion provider (never <code>null</code>) */
    protected final CompletionProvider completionProvider;
    /** The given completion context (never <code>null</code>) */
    protected final CompletionContext completionContext;

    private IRegion selectedRegion;
    private IInformationControlCreator informationControlCreator;
    private CompletionItem resolvedCompletionItem;
    private MatchResultCache matchResultCache;

    /**
     * Constructor.
     *
     * @param completionItem not <code>null</code>
     * @param completionList not <code>null</code>
     * @param completionProvider not <code>null</code>
     * @param completionContext not <code>null</code>
     */
    public BaseCompletionProposal(CompletionItem completionItem, CompletionList completionList,
        CompletionProvider completionProvider, CompletionContext completionContext)
    {
        this.completionItem = Objects.requireNonNull(completionItem);
        this.completionList = Objects.requireNonNull(completionList);
        this.completionProvider = Objects.requireNonNull(completionProvider);
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
        return getStyledDisplayString().getString();
    }

    @Override
    public StyledString getStyledDisplayString()
    {
        StyledString styledDisplayString = new StyledString(completionItem.getLabel());
        CompletionItemLabelDetails labelDetails = completionItem.getLabelDetails();
        if (labelDetails != null)
        {
            String detail = labelDetails.getDetail();
            if (detail != null)
                styledDisplayString.append(detail, StyledString.DECORATIONS_STYLER);

            String description = labelDetails.getDescription();
            if (description != null)
                styledDisplayString.append(
                    MessageFormat.format(Messages.Qualifier_string, description),
                    StyledString.QUALIFIER_STYLER);
        }
        return styledDisplayString;
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
        IContextInformation[] result = computeContextInformation(getContextInformationPosition());
        return result != null && result.length == 1 ? result[0] : null;
    }

    public int getContextInformationPosition()
    {
        Point selection = completionContext.getTextViewer().getSelectedRange();
        return selection.x + selection.y;
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
        if (InsertTextMode.AdjustIndentation.equals(
            CompletionItemUtil.getInsertTextMode(completionItem, completionList.getItemDefaults())))
        {
            edit = adjustIndentation(viewer, offset, edit);
        }
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
        if (!edit.tabStops.isEmpty())
            startLinkedMode(viewer, textEdit.getOffset(), edit.cursorPosition, edit.tabStops);
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

    /**
     * Computes context information for the given offset.
     *
     * @param offset an offset for which context information should be computed
     * @return an array of context information objects, or <code>null</code>
     *  if no context could be found
     */
    protected final IContextInformation[] computeContextInformation(int offset)
    {
        return completionContext.getContentAssistProcessor().computeContextInformation(
            completionContext.getTextViewer(), offset);
    }

    private CompletionItem resolveCompletionItem(IProgressMonitor monitor)
    {
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
            CompletionItemUtil.getTextEdit(completionItem, completionList.getItemDefaults());
        if (textEditOrInsertReplaceEdit == null)
        {
            replacementString = completionItem.getInsertText();
            if (replacementString == null)
                replacementString = completionItem.getLabel();
            IRegion wordRegion = completionContext.getCurrentWordRegion();
            replacementOffset = guessReplacementOffset(document, replacementString,
                wordRegion.getOffset(), completionContext.getInvocationOffset());
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
        if (InsertTextFormat.Snippet.equals(CompletionItemUtil.getInsertTextFormat(completionItem,
            completionList.getItemDefaults())))
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

    private ProposedEdit adjustIndentation(ITextViewer viewer, int offset, ProposedEdit edit)
    {
        if (edit.replacementString.indexOf('\n') < 0 && edit.replacementString.indexOf('\r') < 0)
            return edit;

        String lineLeadingWhitespace = getLineLeadingWhitespace(viewer.getDocument(), offset);
        int lineLeadingWhitespaceLength = lineLeadingWhitespace.length();
        if (lineLeadingWhitespaceLength == 0)
            return edit;

        int cursorPosition = edit.cursorPosition;
        List<TabStop> tabStops = edit.tabStops;

        Document document = new Document(edit.replacementString);
        int numberOfLines = document.getNumberOfLines();
        for (int line = 1; line < numberOfLines; line++)
        {
            try
            {
                int lineOffset = document.getLineOffset(line);

                document.replace(lineOffset, 0, lineLeadingWhitespace);

                if (cursorPosition >= lineOffset)
                    cursorPosition += lineLeadingWhitespaceLength;

                for (TabStop tabStop : tabStops)
                {
                    int[] offsets = tabStop.getOffsets();
                    // break encapsulation and update offsets in place
                    for (int j = 0; j < offsets.length; j++)
                    {
                        if (offsets[j] >= lineOffset)
                            offsets[j] += lineLeadingWhitespaceLength;
                    }
                }
            }
            catch (BadLocationException e)
            {
                throw new AssertionError(e); // should never happen
            }
        }
        return new ProposedEdit(edit.replacementOffset, edit.replacementLength, document.get(),
            cursorPosition, tabStops);
    }

    private static String getLineLeadingWhitespace(IDocument document, int offset)
    {
        try
        {
            int start = document.getLineInformationOfOffset(offset).getOffset();
            int end = findEndOfWhitespace(document, start, offset);
            return document.get(start, end - start);
        }
        catch (BadLocationException e)
        {
            return ""; //$NON-NLS-1$
        }
    }

    private static int findEndOfWhitespace(IDocument document, int offset, int end)
        throws BadLocationException
    {
        while (offset < end)
        {
            char c = document.getChar(offset);
            if (c != ' ' && c != '\t')
                return offset;

            offset++;
        }
        return end;
    }

    private int guessReplacementOffset(IDocument document, String replacementString, int wordOffset,
        int invocationOffset)
    {
        int result = wordOffset;
        if (invocationOffset > wordOffset)
        {
            try
            {
                String prefix = document.get(wordOffset, invocationOffset - wordOffset);
                if (getFilterString().startsWith(prefix + replacementString)) // case of `con<invocation offset>`, filterText `console` and insertText `sole()`
                    result = invocationOffset;
            }
            catch (BadLocationException e)
            {
            }
        }
        return result;
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

        CommandService commandService = completionProvider.getCommandService();
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

    private void startLinkedMode(ITextViewer viewer, int start, int exitOffset,
        List<TabStop> tabStops) throws BadLocationException
    {
        selectedRegion =
            TabStopLinkedMode.start(viewer, start, exitOffset, tabStops).getSelectedRegion();
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
            this.replacementString = Objects.requireNonNull(replacementString);
            this.cursorPosition = cursorPosition;
            this.tabStops = Objects.requireNonNull(tabStops);
        }
    }
}
