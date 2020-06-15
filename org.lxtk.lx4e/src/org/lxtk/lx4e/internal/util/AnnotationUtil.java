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
import java.util.Map.Entry;

import org.eclipse.jface.text.ISynchronizable;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelExtension;

/**
 * Provides static utility methods for manipulating annotations.
 */
public class AnnotationUtil
{
    private static final Annotation[] NO_ANNOTATIONS = new Annotation[0];

    /**
     * Adds and removes annotations to/from the given annotation model
     * in a single step.
     *
     * @param annotationModel not <code>null</code>
     * @param toRemove the annotations to be removed (may be <code>null</code>
     *  or empty)
     * @param toAdd the annotations which will be added (may be <code>null</code>
     *  or empty)
     */
    public static void replaceAnnotations(IAnnotationModel annotationModel,
        Collection<? extends Annotation> toRemove,
        Map<? extends Annotation, ? extends Position> toAdd)
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
                for (Entry<? extends Annotation, ? extends Position> entry : toAdd.entrySet())
                {
                    annotationModel.addAnnotation(entry.getKey(), entry.getValue());
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
