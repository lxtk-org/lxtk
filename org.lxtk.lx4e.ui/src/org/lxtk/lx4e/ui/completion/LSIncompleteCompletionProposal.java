/*******************************************************************************
 * Copyright (c) 2016, 2021 Red Hat Inc. and others.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Lucas Bullen (Red Hat Inc.) - initial implementation
 *   Michał Niewrzał (Rogue Wave Software Inc.)
 *   Lucas Bullen (Red Hat Inc.) - Refactored for incomplete completion lists
 *   Vladimir Piskarev (1C) - adaptation
 *******************************************************************************/
package org.lxtk.lx4e.ui.completion;

import java.net.URI;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.contentassist.BoldStylerProvider;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension3;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension4;
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
import org.eclipse.lsp4j.CompletionItemTag;
import org.eclipse.lsp4j.InsertTextFormat;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.MarkupKind;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.TextStyle;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.statushandlers.StatusManager;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;
import org.eclipse.ui.texteditor.link.EditorLinkedModeUI;
import org.lxtk.CommandService;
import org.lxtk.CompletionProvider;
import org.lxtk.lx4e.CompletionResolveRequest;
import org.lxtk.lx4e.DocumentUtil;
import org.lxtk.lx4e.internal.ui.Activator;
import org.lxtk.lx4e.internal.ui.FocusableInformationControlCreator;
import org.lxtk.lx4e.internal.ui.StyledBrowserInformationControlInput;
import org.lxtk.lx4e.util.Markdown;

@SuppressWarnings("javadoc")
//@formatter:off
class LSIncompleteCompletionProposal
        implements ICompletionProposal, ICompletionProposalExtension3, ICompletionProposalExtension4,
        ICompletionProposalExtension5, ICompletionProposalExtension6, ICompletionProposalExtension7,
        IContextInformation {

    // Those variables should be defined in LSP4J and reused here whenever done there
    // See https://github.com/eclipse/lsp4j/issues/149
    /** The currently selected text or the empty string */
    private static final String TM_SELECTED_TEXT = "TM_SELECTED_TEXT"; //$NON-NLS-1$
    /** The contents of the current line */
    private static final String TM_CURRENT_LINE = "TM_CURRENT_LINE"; //$NON-NLS-1$
    /** The contents of the word under cursor or the empty string */
    private static final String TM_CURRENT_WORD = "TM_CURRENT_WORD"; //$NON-NLS-1$
    /** The zero-index based line number */
    private static final String TM_LINE_INDEX = "TM_LINE_INDEX"; //$NON-NLS-1$
    /** The one-index based line number */
    private static final String TM_LINE_NUMBER = "TM_LINE_NUMBER"; //$NON-NLS-1$
    /** The filename of the current document */
    private static final String TM_FILENAME = "TM_FILENAME"; //$NON-NLS-1$
    /** The filename of the current document without its extensions */
    private static final String TM_FILENAME_BASE = "TM_FILENAME_BASE"; //$NON-NLS-1$
    /** The directory of the current document */
    private static final String TM_DIRECTORY = "TM_DIRECTORY"; //$NON-NLS-1$
    /** The full file path of the current document */
    private static final String TM_FILEPATH = "TM_FILEPATH"; //$NON-NLS-1$

    private static final Styler DEPRECATE = new Styler() {
        @Override
        public void applyStyles(TextStyle textStyle) {
            textStyle.strikeout = true;
            textStyle.foreground = PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY);
        };
    };

    protected final CompletionItem item;
    private CompletionItem resolvedItem;
    private final CompletionProvider provider;
    private final Image image;
    private int initialOffset = -1;
    protected int bestOffset = -1;
    protected int currentOffset = -1;
    protected ITextViewer viewer;
    private final URI documentUri;
    private final IDocument document;
    private IRegion selection;
    private Integer rankCategory;
    private Integer rankScore;
    private String documentFilter;
    private String documentFilterAddition = ""; //$NON-NLS-1$
    private IInformationControlCreator informationControlCreator;

    /**
     * Constructor.
     *
     * @param documentUri not <code>null</code>
     * @param document not <code>null</code>
     * @param offset 0-based
     * @param item not <code>null</code>
     * @param provider not <code>null</code>
     * @param image may be <code>null</code>
     */
    public LSIncompleteCompletionProposal(URI documentUri, IDocument document, int offset, CompletionItem item,
        CompletionProvider provider, Image image) {
        this.documentUri = Objects.requireNonNull(documentUri);
        this.document = Objects.requireNonNull(document);
        this.item = Objects.requireNonNull(item);
        this.provider = Objects.requireNonNull(provider);
        this.image = image;
        if (offset < 0)
            throw new IllegalArgumentException();
        this.initialOffset = offset;
        this.currentOffset = offset;
        this.bestOffset = getPrefixCompletionStart(document, offset);
    }

    /**
     * See {@link CompletionProposalTools.getFilterFromDocument} for filter
     * generation logic
     *
     * @return The document filter for the given offset
     */
    public String getDocumentFilter(int offset) throws BadLocationException {
        if (documentFilter != null) {
            if (offset != currentOffset) {
                documentFilterAddition = document.get(initialOffset, offset - initialOffset);
                rankScore = null;
                rankCategory = null;
                currentOffset = offset;
            }
            return documentFilter + documentFilterAddition;
        }
        currentOffset = offset;
        return getDocumentFilter();
    }

    /**
     * See {@link CompletionProposalTools.getFilterFromDocument} for filter
     * generation logic
     *
     * @return The document filter for the last given offset
     */
    public String getDocumentFilter() throws BadLocationException {
        if (documentFilter != null) {
            return documentFilter + documentFilterAddition;
        }
        documentFilter = CompletionProposalTools.getFilterFromDocument(document, currentOffset,
            getFilterString(), bestOffset);
        documentFilterAddition = ""; //$NON-NLS-1$
        return documentFilter;
    }

    /**
     * See {@link CompletionProposalTools.getScoreOfFilterMatch} for ranking logic
     *
     * @return The rank of the match between the document's filter and this
     *         completion's filter.
     */
    public int getRankScore() {
        if (rankScore != null)
            return rankScore;
        try {
            rankScore = CompletionProposalTools.getScoreOfFilterMatch(getDocumentFilter(),
                getFilterString());
        } catch (BadLocationException e) {
            Activator.logError(e);
            rankScore = -1;
        }
        return rankScore;
    }

    /**
     * See {@link CompletionProposalTools.getCategoryOfFilterMatch} for category
     * logic
     *
     * @return The category of the match between the document's filter and this
     *         completion's filter.
     */
    public int getRankCategory() {
        if (rankCategory != null) {
            return rankCategory;
        }
        try {
            rankCategory = CompletionProposalTools.getCategoryOfFilterMatch(getDocumentFilter(),
                getFilterString());
        } catch (BadLocationException e) {
            Activator.logError(e);
            rankCategory = 5;
        }
        return rankCategory;
    }

    public int getBestOffset() {
        return bestOffset;
    }

    public void updateOffset(int offset) {
        bestOffset = getPrefixCompletionStart(document, offset);
    }

    public CompletionItem getItem() {
        return item;
    }

    private boolean isDeprecated() {
        List<CompletionItemTag> tags = item.getTags();
        if (tags != null && tags.contains(CompletionItemTag.Deprecated)) {
            return true;
        }
        @SuppressWarnings("deprecation")
        boolean deprecated = Boolean.TRUE.equals(item.getDeprecated());
        return deprecated;
    }

    @Override
    public StyledString getStyledDisplayString(IDocument document, int offset, BoldStylerProvider boldStylerProvider) {
        String rawString = getDisplayString();
        StyledString res = isDeprecated()
            ? new StyledString(rawString, DEPRECATE)
                : new StyledString(rawString);
            if (offset > bestOffset) {
                try {
                String subString = getDocumentFilter(offset).toLowerCase();
                int lastIndex = 0;
                String lowerRawString = rawString.toLowerCase();
                    for (Character c : subString.toCharArray()) {
                    int index = lowerRawString.indexOf(c, lastIndex);
                        if (index < 0) {
                        return res;
                    }
                        res.setStyle(index, 1, new Styler() {

                        @Override
                            public void applyStyles(TextStyle textStyle) {
                                if (isDeprecated()) {
                                DEPRECATE.applyStyles(textStyle);
                            }
                                boldStylerProvider.getBoldStyler().applyStyles(textStyle);
                        }

                    });
                    lastIndex = index + 1;
                }
                } catch (BadLocationException e) {
                Activator.logError(e);
            }
        }
        return res;
    }

    @Override
    public String getDisplayString() {
        return item.getLabel();
    }

    @Override
    public StyledString getStyledDisplayString() {
        if (isDeprecated()) {
            return new StyledString(getDisplayString(), DEPRECATE);
        }
        return new StyledString(getDisplayString());
    }

    @Override
    public boolean isAutoInsertable() {
        // TODO consider what's best
        return false;
    }

    @Override
    public IInformationControlCreator getInformationControlCreator() {
        if (informationControlCreator == null)
            informationControlCreator = newInformationControlCreator();
        return informationControlCreator;
    }

    protected IInformationControlCreator newInformationControlCreator() {
        return new FocusableInformationControlCreator() {
                @Override
                protected String getTooltipAffordanceString() {
                    return EditorsUI.getPreferenceStore().getBoolean(
                        AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SHOW_TEXT_HOVER_AFFORDANCE)
                            ? Messages.LSIncompleteCompletionProposal_Tooltip_affordance_string
                            : null;
                    }
            };
    }

    @Override
    public Object getAdditionalProposalInfo(IProgressMonitor monitor) {
        Either<String, MarkupContent> documentation = item.getDocumentation();
        if (documentation == null)
            documentation = resolvedCompletionItem().getDocumentation();
        if (documentation == null)
            return null;

        MarkupContent markupContent = documentation.isLeft()
            ? new MarkupContent(MarkupKind.PLAINTEXT, documentation.getLeft())
            : documentation.getRight();
        if (markupContent == null)
            return null;
        return toAdditionalProposalInfo(markupContent);
    }

    protected Object toAdditionalProposalInfo(MarkupContent markupContent) {
        String value = markupContent.getValue();
        if (value.isEmpty())
            return null;
        String html = Markdown.toHtml(value, false);
        return StyledBrowserInformationControlInput.of(html);
    }

    private synchronized CompletionItem resolvedCompletionItem() {
        if (resolvedItem == null) {

            if (Boolean.TRUE.equals(
                provider.getRegistrationOptions().getResolveProvider())) {

                CompletionResolveRequest request = newCompletionResolveRequest();
                request.setProvider(provider);
                request.setParams(item);
                request.setDefaultResult(item);
                request.setTimeout(getCompletionResolveTimeout());
                request.setMayThrow(false);

                resolvedItem = request.sendAndReceive();
            } else {
                resolvedItem = item;
            }
        }
        return resolvedItem;
    }

    protected CompletionResolveRequest newCompletionResolveRequest() {
        return new CompletionResolveRequest();
    }

    protected Duration getCompletionResolveTimeout() {
        return Duration.ofSeconds(1);
    }

    @Override
    public CharSequence getPrefixCompletionText(IDocument document, int completionOffset) {
        return getInsertText().substring(completionOffset - bestOffset);
    }

    @Override
    public int getPrefixCompletionStart(IDocument document, int completionOffset) {
        if (item.getTextEdit() != null) {
            try {
                return DocumentUtil.toOffset(document, item.getTextEdit().getLeft().getRange().getStart());
            } catch (BadLocationException e) {
                Activator.logError(e);
            }
        }
        String insertText = getInsertText();
        try {
            String subDoc = document.get(
                Math.max(0, completionOffset - insertText.length()),
                Math.min(insertText.length(), completionOffset));
            for (int i = 0; i < insertText.length() && i < completionOffset; i++) {
                String tentativeCommonString = subDoc.substring(i);
                if (insertText.startsWith(tentativeCommonString)) {
                    return completionOffset - tentativeCommonString.length();
                }
            }
        } catch (BadLocationException e) {
            Activator.logError(e);
        }
        return completionOffset;
    }

    @Override
    public void apply(IDocument document) {
        apply(document, Character.MIN_VALUE, 0, bestOffset);
    }

    protected void apply(IDocument document, char trigger, int stateMask, int offset) {
        String insertText = null;
        TextEdit textEdit = null;
        if (item.getTextEdit() != null)
            textEdit = item.getTextEdit().getLeft();
        try {
            if (textEdit == null) {
                insertText = getInsertText();
                Position start = DocumentUtil.toPosition(document, bestOffset);
                Position end = DocumentUtil.toPosition(document, offset); // need 2 distinct objects
                textEdit = new TextEdit(new Range(start, end), insertText);
            } else if (offset > initialOffset) {
                // characters were added after completion was activated
                int shift = offset - initialOffset;
                textEdit.getRange().getEnd().setCharacter(textEdit.getRange().getEnd().getCharacter() + shift);
            }
            { // workaround https://github.com/Microsoft/vscode/issues/17036
                Position start = textEdit.getRange().getStart();
                Position end = textEdit.getRange().getEnd();
                if (start.getLine() > end.getLine() || (start.getLine() == end.getLine() && start.getCharacter() > end.getCharacter())) {
                    textEdit.getRange().setEnd(start);
                    textEdit.getRange().setStart(end);
                }
            }
            { // allow completion items to be wrong with a too wide range
                Position documentEnd = DocumentUtil.toPosition(document, document.getLength());
                Position textEditEnd = textEdit.getRange().getEnd();
                if (documentEnd.getLine() < textEditEnd.getLine()
                    || (documentEnd.getLine() == textEditEnd.getLine() && documentEnd.getCharacter() < textEditEnd.getCharacter())) {
                    textEdit.getRange().setEnd(documentEnd);
                }
            }

            if (insertText != null) {
                // try to reuse existing characters after completion location
                int shift = offset - bestOffset;
                int commonSize = 0;
                while (commonSize < insertText.length() - shift
                    && document.getLength() > offset + commonSize
                    && document.getChar(bestOffset + shift + commonSize) == insertText.charAt(commonSize + shift)) {
                    commonSize++;
                }
                textEdit.getRange().getEnd().setCharacter(textEdit.getRange().getEnd().getCharacter() + commonSize);
            }
            insertText = textEdit.getNewText();
            LinkedHashMap<String, List<LinkedPosition>> regions = new LinkedHashMap<>();
            int insertionOffset = DocumentUtil.toOffset(document, textEdit.getRange().getStart());
            insertionOffset = computeNewOffset(item.getAdditionalTextEdits(), insertionOffset, document);
            if (item.getInsertTextFormat() == InsertTextFormat.Snippet) {
                int currentSnippetOffsetInInsertText = 0;
                while ((currentSnippetOffsetInInsertText = insertText.indexOf('$', currentSnippetOffsetInInsertText)) != -1) {
                    StringBuilder keyBuilder = new StringBuilder();
                    boolean isChoice = false;
                    List<String> snippetProposals = new ArrayList<>();
                    int offsetInSnippet = 1;
                    while (currentSnippetOffsetInInsertText + offsetInSnippet < insertText.length() && Character.isDigit(insertText.charAt(currentSnippetOffsetInInsertText + offsetInSnippet))) {
                        keyBuilder.append(insertText.charAt(currentSnippetOffsetInInsertText + offsetInSnippet));
                        offsetInSnippet++;
                    }
                    if (keyBuilder.length() == 0 && insertText.substring(currentSnippetOffsetInInsertText).startsWith("${")) { //$NON-NLS-1$
                        offsetInSnippet = 2;
                        while (currentSnippetOffsetInInsertText + offsetInSnippet < insertText.length() && Character.isDigit(insertText.charAt(currentSnippetOffsetInInsertText + offsetInSnippet))) {
                            keyBuilder.append(insertText.charAt(currentSnippetOffsetInInsertText + offsetInSnippet));
                            offsetInSnippet++;
                        }
                        if (currentSnippetOffsetInInsertText + offsetInSnippet < insertText.length()) {
                            char currentChar = insertText.charAt(currentSnippetOffsetInInsertText + offsetInSnippet);
                            if (currentChar == ':' || currentChar == '|') {
                                isChoice |= currentChar == '|';
                                offsetInSnippet++;
                            }
                        }
                        boolean close = false;
                        StringBuilder valueBuilder = new StringBuilder();
                        while (currentSnippetOffsetInInsertText + offsetInSnippet < insertText.length() && !close) {
                            char currentChar = insertText.charAt(currentSnippetOffsetInInsertText + offsetInSnippet);
                            if (valueBuilder.length() > 0 &&
                                ((isChoice && (currentChar == ',' || currentChar == '|') || currentChar == '}'))) {
                                String value = valueBuilder.toString();
                                if (value.startsWith("$")) { //$NON-NLS-1$
                                    String varValue = getVariableValue(value.substring(1));
                                    if (varValue != null) {
                                        value = varValue;
                                    }
                                }
                                snippetProposals.add(value);
                                valueBuilder = new StringBuilder();
                            } else if (currentChar != '}') {
                                valueBuilder.append(currentChar);
                            }
                            close = currentChar == '}';
                            offsetInSnippet++;
                        }
                    }
                    String defaultProposal = snippetProposals.isEmpty() ? "" : snippetProposals.get(0); //$NON-NLS-1$
                    if (keyBuilder.length() > 0) {
                        String key = keyBuilder.toString();
                        if (!regions.containsKey(key)) {
                            regions.put(key, new ArrayList<>());
                        }
                        insertText = insertText.substring(0, currentSnippetOffsetInInsertText) + defaultProposal + insertText.substring(currentSnippetOffsetInInsertText + offsetInSnippet);
                        LinkedPosition position = null;
                        if (isChoice && !snippetProposals.isEmpty()) {
                            int replacementOffset = insertionOffset + currentSnippetOffsetInInsertText;
                            ICompletionProposal[] proposals = snippetProposals.stream().map(string ->
                            new CompletionProposal(string, replacementOffset, defaultProposal.length(), replacementOffset + string.length())
                                ).toArray(ICompletionProposal[]::new);
                            position = new ProposalPosition(document, insertionOffset + currentSnippetOffsetInInsertText, defaultProposal.length(), proposals);
                        } else {
                            position = new LinkedPosition(document, insertionOffset + currentSnippetOffsetInInsertText, defaultProposal.length());
                        }
                        regions.get(key).add(position);
                        currentSnippetOffsetInInsertText += defaultProposal.length();
                    } else {
                        currentSnippetOffsetInInsertText++;
                    }
                }
            }
            textEdit.setNewText(insertText); // insertText now has placeholder removed

            Point selectedRange = viewer.getSelectedRange();
            if (selectedRange.y > 0 && isInsertEdit(textEdit)) {
                adjustToSelection(textEdit, DocumentUtil.toRange(document,
                    selectedRange.x, selectedRange.y));
            }

            List<TextEdit> additionalEdits = item.getAdditionalTextEdits();
            if (additionalEdits != null && !additionalEdits.isEmpty()) {
                List<TextEdit> allEdits = new ArrayList<>();
                allEdits.add(textEdit);
                allEdits.addAll(additionalEdits);
                DocumentUtil.applyEdits(document, allEdits);
            } else {
                DocumentUtil.applyEdit(document, textEdit);
            }

            int exitOffset;
            List<LinkedPosition> exitPositions = regions.remove(String.valueOf(0));
            if (exitPositions != null && exitPositions.size() == 1) {
                LinkedPosition exitPosition = exitPositions.get(0);
                exitOffset = exitPosition.getOffset() + exitPosition.getLength();
            } else {
                exitOffset = insertionOffset + textEdit.getNewText().length();
            }

            if (viewer != null && !regions.isEmpty()) {
                LinkedModeModel model = new LinkedModeModel();
                for (List<LinkedPosition> positions: regions.values()) {
                    LinkedPositionGroup group = new LinkedPositionGroup();
                    for (LinkedPosition position : positions) {
                        group.addPosition(position);
                    }
                    model.addGroup(group);
                }
                model.forceInstall();

                LinkedModeUI ui = new EditorLinkedModeUI(model, viewer);
                ui.setExitPosition(viewer, exitOffset, 0, Integer.MAX_VALUE);
                ui.enter();

                selection = ui.getSelectedRegion();
            } else {
                selection = new Region(exitOffset, 0);
            }
        } catch (BadLocationException ex) {
            Activator.logError(ex);
        }

        Command command = item.getCommand();
        if (command != null) {
            CommandService commandService = provider.getCommandService();
            if (commandService != null) {
                CompletableFuture<Object> future = commandService.executeCommand(
                    command.getCommand(), command.getArguments());
                if (future != null) {
                    future.exceptionally(e -> {
                        e = Activator.unwrap(e);
                        if (!Activator.isCancellation(e)) {
                            StatusManager.getManager().handle(Activator.createErrorStatus(
                                MessageFormat.format(Messages.LSIncompleteCompletionProposal_Execution_error,
                                    command.getTitle()), e),
                                StatusManager.LOG);
                        }
                        return null;
                    });
                }
            }
        }
    }

    private static boolean isInsertEdit(TextEdit textEdit) {
        if (textEdit.getNewText().isEmpty()) {
            return false;
        }
        Range range = textEdit.getRange();
        return range.getStart().equals(range.getEnd());
    }

    private static void adjustToSelection(TextEdit textEdit, Range selectedRange) {
        if (textEdit.getRange().getStart().equals(selectedRange.getStart())) {
            textEdit.setRange(selectedRange);
        }
    }

    private int computeNewOffset(List<TextEdit> additionalTextEdits, int insertionOffset, IDocument doc) {
        if (additionalTextEdits != null && !additionalTextEdits.isEmpty()) {
            int adjustment = 0;
            for (TextEdit edit : additionalTextEdits) {
                try {
                    Range rng = edit.getRange();
                    int start = DocumentUtil.toOffset(doc, rng.getStart());
                    if (start <= insertionOffset) {
                        int end = DocumentUtil.toOffset(doc, rng.getEnd());
                        int orgLen = end - start;
                        int newLeng = edit.getNewText().length();
                        int editChange = newLeng - orgLen;
                        adjustment += editChange;
                    }
                } catch (BadLocationException e) {
                    Activator.logError(e);
                }
            }
            return insertionOffset + adjustment;
        }
        return insertionOffset;
    }

    private String getVariableValue(String variableName) {
        switch (variableName) {
        case TM_FILENAME_BASE:
            IPath pathBase = Path.fromPortableString(documentUri.getPath()).removeFileExtension();
            String fileName = pathBase.lastSegment();
            return fileName != null ? fileName : ""; //$NON-NLS-1$
        case TM_FILENAME:
            return Path.fromPortableString(documentUri.getPath()).lastSegment();
        case TM_FILEPATH:
            return documentUri.getPath();
        case TM_DIRECTORY:
            return Path.fromPortableString(documentUri.getPath()).removeLastSegments(1).toString();
        case TM_LINE_INDEX:
            int lineIndex = item.getTextEdit().getLeft().getRange().getStart().getLine();
            return Integer.toString(lineIndex);
        case TM_LINE_NUMBER:
            int lineNumber = item.getTextEdit().getLeft().getRange().getStart().getLine();
            return Integer.toString(lineNumber + 1);
        case TM_CURRENT_LINE:
            int currentLineIndex = item.getTextEdit().getLeft().getRange().getStart().getLine();
            try {
                IRegion lineInformation = document.getLineInformation(currentLineIndex);
                String line = document.get(lineInformation.getOffset(), lineInformation.getLength());
                return line;
            } catch (BadLocationException e) {
                Activator.logError(e);
                return ""; //$NON-NLS-1$
            }
        case TM_SELECTED_TEXT:
            Range selectedRange = item.getTextEdit().getLeft().getRange();
            try {
                int startOffset = DocumentUtil.toOffset(document, selectedRange.getStart());
                int endOffset = DocumentUtil.toOffset(document, selectedRange.getEnd());
                String selectedText = document.get(startOffset, endOffset - startOffset);
                return selectedText;
            } catch (BadLocationException e) {
                Activator.logError(e);
                return ""; //$NON-NLS-1$
            }
        case TM_CURRENT_WORD:
            return ""; //$NON-NLS-1$
        default:
            return null;
        }
    }

    protected String getInsertText() {
        String insertText = item.getInsertText();
        if (item.getTextEdit() != null) {
            insertText = item.getTextEdit().getLeft().getNewText();
        }
        if (insertText == null) {
            insertText = item.getLabel();
        }
        return insertText;
    }

    @Override
    public Point getSelection(IDocument document) {
        if (selection == null) {
            return null;
        }
        return new Point(selection.getOffset(), selection.getLength());
    }

    @Override
    public String getAdditionalProposalInfo() {
        String detail = item.getDetail();
        if (detail == null)
            detail = resolvedCompletionItem().getDetail();
        return detail;
    }

    @Override
    public Image getImage() {
        return image;
    }

    @Override
    public IContextInformation getContextInformation() {
        return this;
    }

    @Override
    public String getContextDisplayString() {
        return getAdditionalProposalInfo();
    }

    @Override
    public String getInformationDisplayString() {
        return getAdditionalProposalInfo();
    }

    public String getSortText() {
        if (item.getSortText() != null && !item.getSortText().isEmpty()) {
            return item.getSortText();
        }
        return item.getLabel();
    }

    public String getFilterString() {
        if (item.getFilterText() != null && !item.getFilterText().isEmpty()) {
            return item.getFilterText();
        }
        return item.getLabel();
    }
}
