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

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.AnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelExtension;
import org.eclipse.jface.text.source.ISourceViewer;

class TabStopAnnotationModel
    extends AnnotationModel
{
    void connect(ITextViewer viewer)
    {
        IAnnotationModelExtension model = getAnnotationModel(viewer);
        if (model != null)
            model.addAnnotationModel(this, this);
    }

    void disconnect(ITextViewer viewer)
    {
        IAnnotationModelExtension model = getAnnotationModel(viewer);
        if (model != null)
            model.removeAnnotationModel(this);
    }

    void setPositions(Iterable<Position> positions)
    {
        removeAllAnnotations(false);
        for (Position position : positions)
        {
            try
            {
                addAnnotation(new Annotation("org.lxtk.lx4e.ui.completion.tabStop", false, ""), //$NON-NLS-1$ //$NON-NLS-2$
                    position, false);
            }
            catch (BadLocationException e)
            {
                // ignore invalid position
            }
        }
        fireModelChanged();
    }

    @Override
    protected void addPosition(IDocument document, Position position)
    {
        // don't to anything as our positions are managed by custom position updaters
    }

    @Override
    protected void removePosition(IDocument document, Position pos)
    {
        // don't to anything as our positions are managed by custom position updaters
    }

    private static IAnnotationModelExtension getAnnotationModel(ITextViewer viewer)
    {
        if (viewer instanceof ISourceViewer)
        {
            IAnnotationModel annotationModel = ((ISourceViewer)viewer).getAnnotationModel();
            if (annotationModel instanceof IAnnotationModelExtension)
                return (IAnnotationModelExtension)annotationModel;
        }
        return null;
    }
}
