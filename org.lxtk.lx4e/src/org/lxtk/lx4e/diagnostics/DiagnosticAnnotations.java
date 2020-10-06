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

import static org.lxtk.lx4e.internal.util.AnnotationUtil.replaceAnnotations;

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
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.lsp4j.Diagnostic;
import org.lxtk.TextDocument;
import org.lxtk.DocumentService;
import org.lxtk.lx4e.DocumentUtil;
import org.lxtk.lx4e.EclipseTextDocument;
import org.lxtk.lx4e.internal.Activator;
import org.lxtk.util.Disposable;

/**
 * Manages annotations representing LSP diagnostics for text documents
 * of a given {@link DocumentService}.
 */
/*
 * Implementation note: methods are synchronized to avoid a race between a thread
 * calling public API methods and a task calling removeAnnotations(TextDocument)
 * scheduled from the 'onDidRemoveTextDocument' event handler.
 */
public class DiagnosticAnnotations
    implements BiConsumer<URI, Collection<Diagnostic>>, Disposable
{
    private final Map<TextDocument, Map<IAnnotationModel, Collection<Annotation>>> info =
        new IdentityHashMap<>();
    private final DocumentService documentService;
    private final Disposable subscription;
    private boolean disposed;

    /**
     * Constructor.
     *
     * @param documentService not <code>null</code>
     */
    public DiagnosticAnnotations(DocumentService documentService)
    {
        this.documentService = Objects.requireNonNull(documentService);
        this.subscription = documentService.onDidRemoveTextDocument().subscribe(
            textDocument -> CompletableFuture.runAsync(() -> removeAnnotations(textDocument)));
    }

    /**
     * Returns the associated {@link DocumentService}.
     *
     * @return the associated <code>DocumentService</code> (never <code>null</code>)
     */
    public final DocumentService getDocumentService()
    {
        return documentService;
    }

    @Override
    public final synchronized void accept(URI uri, Collection<Diagnostic> diagnostics)
    {
        if (disposed)
            return;

        TextDocument textDocument = documentService.getTextDocument(uri);
        if (textDocument == null)
            return;

        IAnnotationModel annotationModel = getAnnotationModel(textDocument);

        Collection<Annotation> toRemove = null;
        Map<IAnnotationModel, Collection<Annotation>> annotations = info.get(textDocument);
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
                        replaceAnnotations(entry.getKey(), entry.getValue(), null);
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
                    addAnnotationInfo(textDocument, annotationModel, toAdd.keySet());
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
     * Removes all annotations currently managed by this object.
     */
    public final synchronized void clear()
    {
        if (info.isEmpty())
            return;

        info.values().forEach(annotations -> annotations.forEach((annotationModel, toRemove) ->
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
     * Removes the annotations currently managed by this object for the given URI.
     *
     * @param uri may be <code>null</code>
     */
    public final synchronized void removeAnnotations(URI uri)
    {
        if (disposed)
            return;

        TextDocument textDocument = documentService.getTextDocument(uri);
        if (textDocument == null)
            return;

        removeAnnotations(textDocument);
    }

    private synchronized void removeAnnotations(TextDocument textDocument)
    {
        Map<IAnnotationModel, Collection<Annotation>> annotations = info.remove(textDocument);
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
     * Adds annotations representing the given diagnostics for the given URI.
     *
     * @param uri not <code>null</code>
     * @param diagnostics not <code>null</code>
     */
    public final synchronized void addAnnotations(URI uri, Collection<Diagnostic> diagnostics)
    {
        Objects.requireNonNull(uri);
        Objects.requireNonNull(diagnostics);

        if (disposed)
            return;

        TextDocument textDocument = documentService.getTextDocument(uri);
        if (textDocument == null)
            return;

        IAnnotationModel annotationModel = getAnnotationModel(textDocument);
        if (annotationModel == null)
            return;

        IDocument document = getUnderlyingDocument(textDocument);
        if (document == null)
            return;

        Map<Annotation, Position> toAdd = toDiagnosticAnnotations(diagnostics, document);
        if (toAdd.isEmpty())
            return;

        addAnnotationInfo(textDocument, annotationModel, toAdd.keySet());
        replaceAnnotations(annotationModel, null, toAdd);
    }

    /**
     * Returns the underlying {@link IAnnotationModel} for the given {@link
     * TextDocument}.
     *
     * @param textDocument may be <code>null</code>, in which case <code>null</code>
     *  is returned
     * @return the underlying <code>IAnnotationModel</code>, or <code>null</code>
     *  if none
     */
    protected IAnnotationModel getAnnotationModel(TextDocument textDocument)
    {
        if (textDocument instanceof EclipseTextDocument)
            return ((EclipseTextDocument)textDocument).getAnnotationModel();
        return null;
    }

    /**
     * Returns the underlying {@link IDocument} for the given {@link TextDocument}.
     *
     * @param textDocument may be <code>null</code>, in which case <code>null</code>
     *  is returned
     * @return the underlying <code>IDocument</code>, or <code>null</code> if none
     */
    protected IDocument getUnderlyingDocument(TextDocument textDocument)
    {
        if (textDocument instanceof EclipseTextDocument)
            return ((EclipseTextDocument)textDocument).getUnderlyingDocument();
        return null;
    }

    /**
     * Creates and returns an annotation that represents the given diagnostic.
     *
     * @param diagnostic never <code>null</code>
     * @return the created annotation (not <code>null</code>)
     */
    protected Annotation createAnnotation(Diagnostic diagnostic)
    {
        return new DiagnosticAnnotation(diagnostic);
    }

    private Map<Annotation, Position> toDiagnosticAnnotations(Collection<Diagnostic> diagnostics,
        IDocument document)
    {
        Map<Annotation, Position> result = new IdentityHashMap<>(diagnostics.size());
        for (Diagnostic diagnostic : diagnostics)
        {
            IRegion r;
            try
            {
                r = DocumentUtil.toRegion(document, diagnostic.getRange());
            }
            catch (BadLocationException e)
            {
                // silently ignore: the document might have changed in the meantime
                continue;
            }
            result.put(createAnnotation(diagnostic), new Position(r.getOffset(), r.getLength()));
        }
        return result;
    }

    private void addAnnotationInfo(TextDocument textDocument, IAnnotationModel annotationModel,
        Collection<Annotation> toAdd)
    {
        Map<IAnnotationModel, Collection<Annotation>> annotationsMap =
            info.computeIfAbsent(textDocument, k -> new IdentityHashMap<>(2));
        Collection<Annotation> annotations = annotationsMap.get(annotationModel);
        if (annotations != null)
            annotations.addAll(toAdd);
        else
        {
            annotations = new ArrayList<>(toAdd);
            annotationsMap.put(annotationModel, annotations);
        }
    }
}
