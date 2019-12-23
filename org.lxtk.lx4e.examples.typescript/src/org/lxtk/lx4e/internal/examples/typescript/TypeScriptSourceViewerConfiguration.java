/*******************************************************************************
 * Copyright (c) 2019 1C-Soft LLC.
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
package org.lxtk.lx4e.internal.examples.typescript;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.handly.ui.IWorkingCopyManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.jface.text.reconciler.IReconciler;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.editors.text.TextSourceViewerConfiguration;
import org.eclipse.ui.texteditor.ITextEditor;
import org.lxtk.LanguageOperationTarget;
import org.lxtk.lx4e.ui.completion.CompletionProposalSorter;
import org.lxtk.lx4e.ui.completion.ContentAssistProcessor;
import org.lxtk.lx4e.ui.hover.TextHover;
import org.lxtk.lx4e.ui.hyperlinks.DefinitionHyperlinkDetector;

/**
 * TODO JavaDoc
 */
public class TypeScriptSourceViewerConfiguration
    extends TextSourceViewerConfiguration
{
    private final ITextEditor editor;
    private final IWorkingCopyManager workingCopyManager;

    public TypeScriptSourceViewerConfiguration(IPreferenceStore preferenceStore,
        ITextEditor editor, IWorkingCopyManager workingCopyManager)
    {
        super(preferenceStore);
        this.editor = editor;
        this.workingCopyManager = workingCopyManager;
    }

    @Override
    public IReconciler getReconciler(ISourceViewer sourceViewer)
    {
        if (editor == null || !editor.isEditable()
            || workingCopyManager == null)
            return null;

        return new TypeScriptReconciler(editor, workingCopyManager);
    }

    @Override
    public IContentAssistant getContentAssistant(ISourceViewer sourceViewer)
    {
        if (editor == null)
            return null;

        ContentAssistant assistant = new ContentAssistant(true);
        assistant.setContentAssistProcessor(new ContentAssistProcessor(
            this::getLanguageOperationTarget), IDocument.DEFAULT_CONTENT_TYPE);
        assistant.setSorter(new CompletionProposalSorter());
        assistant.setInformationControlCreator(
            parent -> new DefaultInformationControl(parent, true));
        assistant.enableColoredLabels(true);
        return assistant;
    }

    @Override
    public IHyperlinkDetector[] getHyperlinkDetectors(
        ISourceViewer sourceViewer)
    {
        IAdaptable context = new IAdaptable()
        {
            @Override
            public <T> T getAdapter(Class<T> adapter)
            {
                if (adapter == LanguageOperationTarget.class)
                    return adapter.cast(getLanguageOperationTarget());
                return null;
            }
        };
        DefinitionHyperlinkDetector definitionHyperlinkDetector =
            new DefinitionHyperlinkDetector();
        definitionHyperlinkDetector.setContext(context);
        return new IHyperlinkDetector[] { definitionHyperlinkDetector };
    }

    @Override
    public ITextHover getTextHover(ISourceViewer sourceViewer,
        String contentType)
    {
        return new TextHover(this::getLanguageOperationTarget);
    }

    private LanguageOperationTarget getLanguageOperationTarget()
    {
        return TypeScriptOperationTargetProvider.getOperationTarget(editor);
    }
}