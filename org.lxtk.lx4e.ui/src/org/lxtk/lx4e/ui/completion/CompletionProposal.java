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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension2;
import org.eclipse.lsp4j.CompletionItem;
import org.lxtk.lx4e.internal.ui.Activator;

/**
 * Default implementation of a completion proposal.
 */
public class CompletionProposal
    extends BaseCompletionProposal
    implements ICompletionProposalExtension, ICompletionProposalExtension2
{
    private static final char[] NO_CHARS = new char[0];

    private int editingDelta;
    private char[] triggerCharacters;

    /**
     * Constructor.
     *
     * @param completionItem not <code>null</code>
     * @param completionContext not <code>null</code>
     */
    public CompletionProposal(CompletionItem completionItem, CompletionContext completionContext)
    {
        super(completionItem, completionContext);
    }

    @Override
    public boolean isValidFor(IDocument document, int offset)
    {
        return getMatchResult(getPrefix(document, offset), getFilterString()) != null;
    }

    @Override
    public boolean validate(IDocument document, int offset, DocumentEvent event)
    {
        boolean isValid = isValidFor(document, offset);

        if (isValid && event != null)
            editingDelta += (event.fText == null ? 0 : event.fText.length()) - event.fLength;

        return isValid;
    }

    @Override
    public void apply(IDocument document, char trigger, int offset)
    {
        if (document != completionContext.getDocument())
            throw new IllegalArgumentException();

        apply(completionContext.getTextViewer(), trigger, 0, offset);
    }

    @Override
    public void apply(ITextViewer viewer, char trigger, int stateMask, int offset)
    {
        try
        {
            super.apply(viewer, trigger, stateMask, offset);
        }
        catch (BadLocationException e)
        {
            Activator.logError(e);
        }
    }

    @Override
    public void selected(ITextViewer viewer, boolean smartToggle)
    {
    }

    @Override
    public void unselected(ITextViewer viewer)
    {
    }

    @Override
    public char[] getTriggerCharacters()
    {
        if (triggerCharacters == null)
            triggerCharacters = computeTriggerCharacters();
        return triggerCharacters;
    }

    @Override
    protected int getEditingDelta()
    {
        return editingDelta;
    }

    private char[] computeTriggerCharacters()
    {
        List<String> commitCharacters = completionItem.getCommitCharacters();
        if (commitCharacters == null || commitCharacters.isEmpty())
            return NO_CHARS;

        Set<Character> commitCharactersWithoutDuplicates = new HashSet<>();
        for (String each : commitCharacters)
        {
            if (each.length() == 1)
                commitCharactersWithoutDuplicates.add(each.charAt(0));
        }
        char[] result = new char[commitCharactersWithoutDuplicates.size()];
        int i = 0;
        for (Character ch : commitCharactersWithoutDuplicates)
        {
            result[i++] = ch;
        }
        return result;
    }
}
