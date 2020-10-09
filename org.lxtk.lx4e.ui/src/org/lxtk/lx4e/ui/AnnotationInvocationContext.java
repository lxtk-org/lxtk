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
package org.lxtk.lx4e.ui;

import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.TextInvocationContext;

/**
 * A quick assist invocation context that can be associated with an annotation.
 */
public class AnnotationInvocationContext
    extends TextInvocationContext
{
    private final Annotation annotation;

    /**
     * Constructor.
     *
     * @param sourceViewer may be <code>null</code> if none
     * @param offset 0-based offset or -1 if unknown
     * @param length may be -1 if unknown
     * @param annotation may be <code>null</code> if none
     */
    public AnnotationInvocationContext(ISourceViewer sourceViewer, int offset, int length,
        Annotation annotation)
    {
        super(sourceViewer, offset, length);
        this.annotation = annotation;
    }

    /**
     * Returns the annotation associated with this context.
     *
     * @return the associated annotation or <code>null</code> if none
     */
    public Annotation getAnnotation()
    {
        return annotation;
    }
}
