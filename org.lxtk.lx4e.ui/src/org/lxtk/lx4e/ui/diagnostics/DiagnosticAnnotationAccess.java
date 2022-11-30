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

import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationPresentation;
import org.eclipse.jface.text.source.ImageUtilities;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.ui.texteditor.DefaultMarkerAnnotationAccess;
import org.eclipse.ui.texteditor.SimpleMarkerAnnotation;
import org.lxtk.jsonrpc.DefaultGson;
import org.lxtk.lx4e.diagnostics.DiagnosticMarkers;
import org.lxtk.lx4e.internal.ui.Activator;
import org.lxtk.lx4e.internal.ui.LSPImages;

import com.google.gson.JsonParseException;

/**
 * Provides special treatment for diagnostic annotations. Text editors that may contain
 * diagnostic annotations need to create an instance of this class in their
 * <code>createAnnotationAccess()</code> method.
 */
public class DiagnosticAnnotationAccess
    extends DefaultMarkerAnnotationAccess
{
    @Override
    public int getLayer(Annotation annotation)
    {
        int layer = super.getLayer(annotation);

        if (annotation instanceof IDiagnosticAnnotation
            && !(annotation instanceof SimpleMarkerAnnotation
                || annotation instanceof IAnnotationPresentation || annotation.isMarkedDeleted()))
            layer++;

        return layer;
    }

    @Override
    public void paint(Annotation annotation, GC gc, Canvas canvas, Rectangle bounds)
    {
        if (annotation.isMarkedDeleted() && annotation instanceof SimpleMarkerAnnotation
            && !(annotation instanceof IAnnotationPresentation))
        {
            Diagnostic diagnostic;
            try
            {
                diagnostic = DefaultGson.INSTANCE.fromJson(
                    ((SimpleMarkerAnnotation)annotation).getMarker().getAttribute(
                        DiagnosticMarkers.DIAGNOSTIC_ATTRIBUTE, null),
                    Diagnostic.class);
            }
            catch (JsonParseException e)
            {
                Activator.logError(e);
                diagnostic = null;
            }
            if (diagnostic != null)
            {
                Image image = LSPImages.getDiagnosticImageGray(diagnostic.getSeverity());
                if (image != null)
                {
                    ImageUtilities.drawImage(image, gc, canvas, bounds, SWT.CENTER, SWT.TOP);
                    return;
                }
            }
        }
        super.paint(annotation, gc, canvas, bounds);
    }
}
