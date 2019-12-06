/*******************************************************************************
 * Copyright (c) 2016, 2019 Red Hat Inc. and others.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Mickael Istria (Red Hat Inc.) - initial implementation
 *   Michał Niewrzał (Rogue Wave Software Inc.)
 *   Lucas Bullen (Red Hat Inc.) - Refactored for incomplete completion lists
 *   Vladimir Piskarev (1C) - adaptation
 *******************************************************************************/
package org.lxtk.lx4e.ui.completion;

import java.net.URI;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension2;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.lxtk.CompletionProvider;
import org.lxtk.lx4e.DocumentUtil;
import org.lxtk.lx4e.internal.ui.Activator;

//@formatter:off
class LSCompletionProposal extends LSIncompleteCompletionProposal
        implements ICompletionProposalExtension, ICompletionProposalExtension2 {

    /**
     * Constructor.
     *
     * @param documentUri not <code>null</code>
     * @param document not <code>null</code>
     * @param offset
     * @param item not <code>null</code>
     * @param provider not <code>null</code>
     * @param image may be <code>null</code>
     */
    public LSCompletionProposal(URI documentUri, IDocument document, int offset, CompletionItem item,
        CompletionProvider provider, Image image) {
        super(documentUri, document, offset, item, provider, image);
    }

    @Override
    public boolean isValidFor(IDocument document, int offset) {
        return validate(document, offset, null);
    }

    @Override
    public void selected(ITextViewer viewer, boolean smartToggle) {
        this.viewer = viewer;
    }

    @Override
    public void unselected(ITextViewer viewer) {
    }

    @Override
    public boolean validate(IDocument document, int offset, DocumentEvent event) {
        if (item.getLabel() == null || item.getLabel().isEmpty()) {
            return false;
        }
        if (offset < bestOffset) {
            return false;
        }
        try {
            String documentFilter = getDocumentFilter(offset);
            if (!documentFilter.isEmpty()) {
                return CompletionProposalTools.isSubstringFoundOrderedInString(documentFilter, getFilterString());
            } else if (item.getTextEdit() != null) {
                return offset == DocumentUtil.toOffset(document, item.getTextEdit().getRange().getStart());
            }
        } catch (BadLocationException e) {
            Activator.logError(e);
        }
        return true;
    }

    @Override
    public void apply(ITextViewer viewer, char trigger, int stateMask, int offset) {
        this.viewer = viewer;
        apply(viewer.getDocument(), trigger, stateMask, offset);
    }

    @Override
    public void apply(IDocument document, char trigger, int offset) {
        apply(document, trigger, 0, offset);
    }

    @Override
    public void apply(IDocument document) {
        apply(document, Character.MIN_VALUE, 0, bestOffset);
    }

    @Override
    public char[] getTriggerCharacters() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getContextInformationPosition() {
        return SWT.RIGHT;
    }
}
