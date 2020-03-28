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
package org.lxtk.lx4e.internal.util;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

import java.util.Collection;
import java.util.Map;

import org.eclipse.jface.text.ISynchronizable;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelExtension;

/**
 * TODO JavaDoc
 */
public class AnnotationUtil
{
    private static final Annotation[] NO_ANNOTATIONS = new Annotation[0];

    /**
     * TODO JavaDoc
     *
     * @param annotationModel not <code>null</code>
     * @param toRemove may be <code>null</code> or empty
     * @param toAdd may be <code>null</code> or empty
     */
    public static void replaceAnnotations(IAnnotationModel annotationModel,
        Collection<Annotation> toRemove, Map<Annotation, Position> toAdd)
    {
        if (toRemove == null)
            toRemove = emptyList();
        if (toAdd == null)
            toAdd = emptyMap();
        if (toRemove.isEmpty() && toAdd.isEmpty())
            return;
        synchronized (getLockObject(annotationModel))
        {
            if (annotationModel instanceof IAnnotationModelExtension)
            {
                ((IAnnotationModelExtension)annotationModel).replaceAnnotations(
                    toRemove.toArray(NO_ANNOTATIONS), toAdd);
            }
            else
            {
                for (Annotation annotation : toRemove)
                {
                    annotationModel.removeAnnotation(annotation);
                }
                for (Map.Entry<Annotation, Position> entry : toAdd.entrySet())
                {
                    annotationModel.addAnnotation(entry.getKey(),
                        entry.getValue());
                }
            }
        }
    }

    private static Object getLockObject(IAnnotationModel annotationModel)
    {
        if (annotationModel instanceof ISynchronizable)
        {
            Object lock = ((ISynchronizable)annotationModel).getLockObject();
            if (lock != null)
                return lock;
        }
        return annotationModel;
    }

    private AnnotationUtil()
    {
    }
}
