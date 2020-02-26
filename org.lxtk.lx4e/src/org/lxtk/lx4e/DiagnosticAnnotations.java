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
package org.lxtk.lx4e;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ISynchronizable;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelExtension;
import org.eclipse.lsp4j.Diagnostic;
import org.lxtk.TextDocument;
import org.lxtk.Workspace;
import org.lxtk.lx4e.internal.Activator;
import org.lxtk.util.Disposable;

/**
 * TODO JavaDoc
 */
public class DiagnosticAnnotations
    implements BiConsumer<URI, Collection<Diagnostic>>, Disposable
{
    protected final Workspace workspace;

    /**
     * TODO JavaDoc
     *
     * @param workspace not <code>null</code>
     */
    public DiagnosticAnnotations(Workspace workspace)
    {
        this.workspace = Objects.requireNonNull(workspace);
    }

    @Override
    public void accept(URI uri, Collection<Diagnostic> diagnostics)
    {
        IAnnotationModel annotationModel = getAnnotationModel(uri);
        if (annotationModel == null)
            return;

        Map<Annotation, Position> toAdd = emptyMap();
        if (diagnostics != null && !diagnostics.isEmpty())
        {
            IDocument document = getDocument(uri);
            if (document != null)
                toAdd = toDiagnosticAnnotations(diagnostics, document);
        }

        replaceAnnotations(annotationModel, getDiagnosticAnnotations(
            annotationModel), toAdd);
    }

    @Override
    public void dispose()
    {
        clear();
    }

    /**
     * TODO JavaDoc
     */
    public void clear()
    {
        workspace.getTextDocuments().forEach(textDocument -> removeAnnotations(
            textDocument.getUri()));
    }

    /**
     * TODO JavaDoc
     *
     * @param uri may be <code>null</code>
     */
    public void removeAnnotations(URI uri)
    {
        IAnnotationModel annotationModel = getAnnotationModel(uri);
        if (annotationModel == null)
            return;

        replaceAnnotations(annotationModel, getDiagnosticAnnotations(
            annotationModel), emptyMap());
    }

    /**
     * TODO JavaDoc
     *
     * @param uri not <code>null</code>
     * @param diagnostics not <code>null</code>
     */
    public void addAnnotations(URI uri, Collection<Diagnostic> diagnostics)
    {
        Objects.requireNonNull(uri);
        Objects.requireNonNull(diagnostics);

        IAnnotationModel annotationModel = getAnnotationModel(uri);
        if (annotationModel == null)
            return;

        IDocument document = getDocument(uri);
        if (document == null)
            return;

        replaceAnnotations(annotationModel, emptyList(),
            toDiagnosticAnnotations(diagnostics, document));
    }

    /**
     * TODO JavaDoc
     *
     * @param uri may be <code>null</code>, in which case <code>null</code>
     *  is returned
     * @return the corresponding annotation model, or <code>null</code> if none
     */
    protected IAnnotationModel getAnnotationModel(URI uri)
    {
        TextDocument textDocument = workspace.getTextDocument(uri);
        if (textDocument instanceof EclipseTextDocument)
            return ((EclipseTextDocument)textDocument).getAnnotationModel();
        return null;
    }

    /**
     * TODO JavaDoc
     *
     * @param uri may be <code>null</code>, in which case <code>null</code>
     *  is returned
     * @return the corresponding document, or <code>null</code> if none
     */
    protected IDocument getDocument(URI uri)
    {
        TextDocument textDocument = workspace.getTextDocument(uri);
        if (textDocument instanceof EclipseTextDocument)
            return ((EclipseTextDocument)textDocument).getUnderlyingDocument();
        return null;
    }

    /**
     * TODO JavaDoc
     *
     * @param annotation never <code>null</code>
     * @return whether the given annotation is a diagnostic annotation
     */
    protected boolean isDiagnosticAnnotation(Annotation annotation)
    {
        return annotation instanceof IDiagnosticAnnotation;
    }

    /**
     * TODO JavaDoc
     *
     * @param diagnostic never <code>null</code>
     * @return the diagnostic annotation (not <code>null</code>)
     */
    protected Annotation createAnnotation(Diagnostic diagnostic)
    {
        return new DiagnosticAnnotation(diagnostic);
    }

    private List<Annotation> getDiagnosticAnnotations(
        IAnnotationModel annotationModel)
    {
        List<Annotation> result = new ArrayList<>();
        Iterator<Annotation> it = annotationModel.getAnnotationIterator();
        while (it.hasNext())
        {
            Annotation annotation = it.next();
            if (isDiagnosticAnnotation(annotation))
                result.add(annotation);
        }
        return result;
    }

    private Map<Annotation, Position> toDiagnosticAnnotations(
        Collection<Diagnostic> diagnostics, IDocument document)
    {
        Map<Annotation, Position> result = new HashMap<>((int)Math.ceil(
            diagnostics.size() / 0.75), 0.75f);
        for (Diagnostic diagnostic : diagnostics)
        {
            IRegion r;
            try
            {
                r = DocumentUtil.toRegion(document, diagnostic.getRange());
            }
            catch (BadLocationException e)
            {
                Activator.logError(e);
                continue;
            }
            result.put(createAnnotation(diagnostic), new Position(r.getOffset(),
                r.getLength()));
        }
        return result;
    }

    private static void replaceAnnotations(IAnnotationModel annotationModel,
        List<Annotation> toRemove, Map<Annotation, Position> toAdd)
    {
        synchronized (getLockObject(annotationModel))
        {
            if (annotationModel instanceof IAnnotationModelExtension)
            {
                ((IAnnotationModelExtension)annotationModel).replaceAnnotations(
                    toRemove.toArray(new Annotation[toRemove.size()]), toAdd);
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
}
