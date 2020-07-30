/*******************************************************************************
 * Copyright (c) 2020 1C-Soft LLC.
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
package org.lxtk.lx4e.internal.examples.proto;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.editors.text.TextSourceViewerConfiguration;
import org.eclipse.ui.texteditor.ITextEditor;
import org.lxtk.LanguageOperationTarget;
import org.lxtk.lx4e.ui.completion.CompletionProposalSorter;
import org.lxtk.lx4e.ui.completion.ContentAssistProcessor;

/**
 * Configuration for a source viewer which shows Proto content.
 */
public class ProtoSourceViewerConfiguration
    extends TextSourceViewerConfiguration
{
    private final ITextEditor editor;

    /**
     * Constructor.
     *
     * @param preferenceStore may be <code>null</code>
     * @param editor may be <code>null</code>
     */
    public ProtoSourceViewerConfiguration(IPreferenceStore preferenceStore, ITextEditor editor)
    {
        super(preferenceStore);
        this.editor = editor;
    }

    @Override
    public IContentAssistant getContentAssistant(ISourceViewer sourceViewer)
    {
        if (editor == null || !editor.isEditable())
            return null;

        ContentAssistant assistant = new ContentAssistant(true);
        assistant.setContentAssistProcessor(
            new ContentAssistProcessor(this::getLanguageOperationTarget),
            IDocument.DEFAULT_CONTENT_TYPE);
        assistant.setSorter(new CompletionProposalSorter());
        assistant.setInformationControlCreator(
            parent -> new DefaultInformationControl(parent, true));
        assistant.enableColoredLabels(true);
        return assistant;
    }

    private LanguageOperationTarget getLanguageOperationTarget()
    {
        return ProtoOperationTargetProvider.getOperationTarget(editor);
    }
}
