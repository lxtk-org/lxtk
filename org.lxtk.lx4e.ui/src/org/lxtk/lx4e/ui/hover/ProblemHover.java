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
package org.lxtk.lx4e.ui.hover;

import java.util.Objects;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.quickassist.IQuickAssistProcessor;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.ISourceViewer;
import org.lxtk.lx4e.ui.AnnotationInvocationContext;

/**
 * An annotation hover that shows description of the selected quick-fixable annotation
 * with the list of possible quick fixes.
 */
public class ProblemHover
    extends AnnotationHover
{
    private final IQuickAssistProcessor processor;

    /**
     * Constructor.
     *
     * @param store a preference store (not <code>null</code>)
     * @param processor a quick assist processor (not <code>null</code>)
     */
    public ProblemHover(IPreferenceStore store, IQuickAssistProcessor processor)
    {
        super(store);
        this.processor = Objects.requireNonNull(processor);
    }

    @Override
    protected boolean isIncluded(Annotation annotation)
    {
        return processor.canFix(annotation);
    }

    @Override
    protected AnnotationInfo createAnnotationInfo(Annotation annotation, Position position,
        ITextViewer textViewer)
    {
        return new AnnotationInfo(annotation, position, textViewer)
        {
            @Override
            public ICompletionProposal[] getCompletionProposals()
            {
                ISourceViewer sourceViewer = null;
                if (textViewer instanceof ISourceViewer)
                    sourceViewer = (ISourceViewer)textViewer;

                return processor.computeQuickAssistProposals(new AnnotationInvocationContext(
                    sourceViewer, position.getOffset(), position.getLength(), annotation));
            }
        };
    }
}
