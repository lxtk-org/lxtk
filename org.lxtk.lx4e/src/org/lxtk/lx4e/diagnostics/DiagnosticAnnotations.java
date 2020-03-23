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
package org.lxtk.lx4e.diagnostics;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
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
import org.lxtk.lx4e.DocumentUtil;
import org.lxtk.lx4e.EclipseTextDocument;
import org.lxtk.lx4e.internal.Activator;
import org.lxtk.util.Disposable;

/**
 * TODO JavaDoc
 */
/*
 * Implementation note: methods are synchronized to avoid a race between a thread
 * calling public API methods and a task calling removeAnnotations(TextDocument)
 * scheduled from the 'onDidRemoveTextDocument' event handler.
 */
public class DiagnosticAnnotations
    implements BiConsumer<URI, Collection<Diagnostic>>, Disposable
{
    private static final Annotation[] NO_ANNOTATIONS = new Annotation[0];

    private final Map<TextDocument, Map<IAnnotationModel, Collection<Annotation>>> info =
        new IdentityHashMap<>();
    private final Workspace workspace;
    private final Disposable subscription;
    private boolean disposed;

    /**
     * TODO JavaDoc
     *
     * @param workspace not <code>null</code>
     */
    public DiagnosticAnnotations(Workspace workspace)
    {
        this.workspace = Objects.requireNonNull(workspace);
        this.subscription = workspace.onDidRemoveTextDocument().subscribe(
            textDocument -> CompletableFuture.runAsync(() -> removeAnnotations(
                textDocument)));
    }

    /**
     * TODO JavaDoc
     *
     * @return the underlying {@link Workspace} (never <code>null</code>)
     */
    public final Workspace getWorkspace()
    {
        return workspace;
    }

    @Override
    public final synchronized void accept(URI uri,
        Collection<Diagnostic> diagnostics)
    {
        if (disposed)
            return;

        TextDocument textDocument = workspace.getTextDocument(uri);
        if (textDocument == null)
            return;

        IAnnotationModel annotationModel = getAnnotationModel(textDocument);

        Collection<Annotation> toRemove = null;
        Map<IAnnotationModel, Collection<Annotation>> annotations = info.get(
            textDocument);
        if (annotations != null)
        {
            for (Map.Entry<IAnnotationModel, Collection<Annotation>> entry : annotations.entrySet())
            {
                if (entry.getKey() == annotationModel)
                    toRemove = entry.getValue();
                else
                {
                    try
                    {
                        replaceAnnotations(entry.getKey(), entry.getValue(),
                            null);
                    }
                    catch (Throwable e)
                    {
                        Activator.logError(e);
                    }
                }
            }
            annotations.clear();
        }

        if (annotationModel == null)
            return;

        Map<Annotation, Position> toAdd = null;
        if (diagnostics != null && !diagnostics.isEmpty())
        {
            IDocument document = getUnderlyingDocument(textDocument);
            if (document != null)
            {
                toAdd = toDiagnosticAnnotations(diagnostics, document);
                if (!toAdd.isEmpty())
                    addAnnotationInfo(textDocument, annotationModel,
                        toAdd.keySet());
            }
        }

        replaceAnnotations(annotationModel, toRemove, toAdd);
    }

    @Override
    public final synchronized void dispose()
    {
        if (disposed)
            return;

        clear();
        subscription.dispose();
        disposed = true;
    }

    /**
     * TODO JavaDoc
     */
    public final synchronized void clear()
    {
        if (info.isEmpty())
            return;

        info.values().forEach(annotations -> annotations.forEach((
            annotationModel, toRemove) ->
        {
            try
            {
                replaceAnnotations(annotationModel, toRemove, null);
            }
            catch (Throwable e)
            {
                Activator.logError(e);
            }
        }));
        info.clear();
    }

    /**
     * TODO JavaDoc
     *
     * @param uri may be <code>null</code>
     */
    public final synchronized void removeAnnotations(URI uri)
    {
        if (disposed)
            return;

        TextDocument textDocument = workspace.getTextDocument(uri);
        if (textDocument == null)
            return;

        removeAnnotations(textDocument);
    }

    private synchronized void removeAnnotations(TextDocument textDocument)
    {
        Map<IAnnotationModel, Collection<Annotation>> annotations = info.remove(
            textDocument);
        if (annotations == null)
            return;
        annotations.forEach((annotationModel, toRemove) ->
        {
            try
            {
                replaceAnnotations(annotationModel, toRemove, null);
            }
            catch (Throwable e)
            {
                Activator.logError(e);
            }
        });
    }

    /**
     * TODO JavaDoc
     *
     * @param uri not <code>null</code>
     * @param diagnostics not <code>null</code>
     */
    public final synchronized void addAnnotations(URI uri,
        Collection<Diagnostic> diagnostics)
    {
        Objects.requireNonNull(uri);
        Objects.requireNonNull(diagnostics);

        if (disposed)
            return;

        TextDocument textDocument = workspace.getTextDocument(uri);
        if (textDocument == null)
            return;

        IAnnotationModel annotationModel = getAnnotationModel(textDocument);
        if (annotationModel == null)
            return;

        IDocument document = getUnderlyingDocument(textDocument);
        if (document == null)
            return;

        Map<Annotation, Position> toAdd = toDiagnosticAnnotations(diagnostics,
            document);
        if (toAdd.isEmpty())
            return;

        addAnnotationInfo(textDocument, annotationModel, toAdd.keySet());
        replaceAnnotations(annotationModel, null, toAdd);
    }

    /**
     * TODO JavaDoc
     *
     * @param textDocument may be <code>null</code>, in which case <code>null</code>
     *  is returned
     * @return the underlying {@link IAnnotationModel}, or <code>null</code> if none
     */
    protected IAnnotationModel getAnnotationModel(TextDocument textDocument)
    {
        if (textDocument instanceof EclipseTextDocument)
            return ((EclipseTextDocument)textDocument).getAnnotationModel();
        return null;
    }

    /**
     * TODO JavaDoc
     *
     * @param textDocument may be <code>null</code>, in which case <code>null</code>
     *  is returned
     * @return the underlying {@link IDocument}, or <code>null</code> if none
     */
    protected IDocument getUnderlyingDocument(TextDocument textDocument)
    {
        if (textDocument instanceof EclipseTextDocument)
            return ((EclipseTextDocument)textDocument).getUnderlyingDocument();
        return null;
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

    private Map<Annotation, Position> toDiagnosticAnnotations(
        Collection<Diagnostic> diagnostics, IDocument document)
    {
        Map<Annotation, Position> result = new IdentityHashMap<>(
            diagnostics.size());
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

    private void addAnnotationInfo(TextDocument textDocument,
        IAnnotationModel annotationModel, Collection<Annotation> toAdd)
    {
        Map<IAnnotationModel, Collection<Annotation>> annotationsMap =
            info.computeIfAbsent(textDocument, k -> new IdentityHashMap<>(2));
        Collection<Annotation> annotations = annotationsMap.get(
            annotationModel);
        if (annotations != null)
            annotations.addAll(toAdd);
        else
        {
            annotations = new ArrayList<>(toAdd);
            annotationsMap.put(annotationModel, annotations);
        }
    }

    private static void replaceAnnotations(IAnnotationModel annotationModel,
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
}
