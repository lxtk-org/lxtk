/*******************************************************************************
 * Copyright (c) 2022 1C-Soft LLC.
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
package org.lxtk.lx4e.ui.diagnostics;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.AnnotationModelEvent;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelListener;
import org.eclipse.jface.text.source.IAnnotationModelListenerExtension;
import org.eclipse.ui.texteditor.SimpleMarkerAnnotation;
import org.lxtk.lx4e.diagnostics.DiagnosticMarkers;

/**
 * Provides special treatment for diagnostic annotations added to an annotation model.
 * Clients need to register this listener with an annotation model that may contain
 * diagnostic annotations.
 */
public final class DiagnosticAnnotationModelListener
    implements IAnnotationModelListener, IAnnotationModelListenerExtension
{
    @Override
    public void modelChanged(AnnotationModelEvent event)
    {
        IAnnotationModel model = event.getAnnotationModel();
        if (event.isWorldChange())
        {
            modelChanged(model);
        }
        else
        {
            Annotation[] annotations = event.getAddedAnnotations();
            for (Annotation annotation : annotations)
            {
                if (annotation instanceof SimpleMarkerAnnotation)
                {
                    if (!annotation.isMarkedDeleted() && getOverlayingAnnotation(model,
                        (SimpleMarkerAnnotation)annotation) != null)
                    {
                        annotation.markDeleted(true);
                    }
                }
                else if (annotation instanceof IDiagnosticAnnotation)
                {
                    List<SimpleMarkerAnnotation> overlayedAnnotations =
                        getOverlayedAnnotations(model, (IDiagnosticAnnotation)annotation);
                    for (SimpleMarkerAnnotation overlayedAnnotation : overlayedAnnotations)
                    {
                        overlayedAnnotation.markDeleted(true);
                    }
                }
            }
        }
    }

    @Override
    public void modelChanged(IAnnotationModel model)
    {
        Iterator<Annotation> iterator = model.getAnnotationIterator();
        while (iterator.hasNext())
        {
            Annotation annotation = iterator.next();
            if (annotation instanceof SimpleMarkerAnnotation && !annotation.isMarkedDeleted()
                && getOverlayingAnnotation(model, (SimpleMarkerAnnotation)annotation) != null)
            {
                annotation.markDeleted(true);
            }
        }
    }

    private IDiagnosticAnnotation getOverlayingAnnotation(IAnnotationModel model,
        SimpleMarkerAnnotation annotation)
    {
        if (annotation.getMarker().getAttribute(DiagnosticMarkers.DIAGNOSTIC_ATTRIBUTE,
            null) != null)
        {
            Position position = model.getPosition(annotation);
            if (position != null && !position.isDeleted())
            {
                Iterator<Annotation> iterator = model.getAnnotationIterator();
                while (iterator.hasNext())
                {
                    Annotation a = iterator.next();
                    if (a instanceof IDiagnosticAnnotation
                        && !(a instanceof SimpleMarkerAnnotation))
                    {
                        Position p = model.getPosition(a);
                        if (position.equals(p) && !p.isDeleted())
                            return (IDiagnosticAnnotation)a;
                    }
                }
            }
        }
        return null;
    }

    private List<SimpleMarkerAnnotation> getOverlayedAnnotations(IAnnotationModel model,
        IDiagnosticAnnotation annotation)
    {
        if (annotation instanceof SimpleMarkerAnnotation)
            throw new AssertionError();

        List<SimpleMarkerAnnotation> result = new ArrayList<>();
        Position position = model.getPosition((Annotation)annotation);
        if (position != null && !position.isDeleted())
        {
            Iterator<Annotation> iterator = model.getAnnotationIterator();
            while (iterator.hasNext())
            {
                Annotation a = iterator.next();
                if (a instanceof SimpleMarkerAnnotation)
                {
                    Position p = model.getPosition(a);
                    if (position.equals(p) && !p.isDeleted())
                    {
                        SimpleMarkerAnnotation overlayed = (SimpleMarkerAnnotation)a;
                        if (overlayed.getMarker().getAttribute(
                            DiagnosticMarkers.DIAGNOSTIC_ATTRIBUTE, null) != null)
                        {
                            result.add(overlayed);
                        }
                    }
                }
            }
        }
        return result;
    }
}
