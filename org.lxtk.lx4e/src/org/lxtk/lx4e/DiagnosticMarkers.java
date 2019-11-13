/*******************************************************************************
 * Copyright (c) 2019 1C-Soft LLC.
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

import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.handly.buffer.IBuffer;
import org.eclipse.handly.buffer.TextFileBuffer;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IRegion;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.lxtk.lx4e.internal.Activator;
import org.lxtk.util.Disposable;

import com.google.gson.Gson;

/**
 * TODO JavaDoc
 * <p>
 * This implementation assumes that the given marker type is <b>not</b> persistent.
 * <p>
 * </p>
 * This implementation is <b>not</b> thread-safe.
 * </p>
 */
public class DiagnosticMarkers
    implements BiConsumer<URI, Collection<Diagnostic>>, Disposable
{
    /**
     * Marker diagnostic attribute. Contains a {@link Diagnostic}
     * serialized to JSON.
     */
    public static final String DIAGNOSTIC_ATTRIBUTE = "diagnostic"; //$NON-NLS-1$

    /**
     * Holds the given marker type.
     */
    protected final String markerType;

    /**
     * Holds the markers map.
     *
     * @see #getMarkers()
     */
    protected Map<URI, Set<IMarker>> markers;

    private Gson gson;

    /**
     * TODO JavaDoc
     *
     * @param markerType not <code>null</code>
     */
    public DiagnosticMarkers(String markerType)
    {
        this.markerType = Objects.requireNonNull(markerType);
    }

    @Override
    public void accept(URI uri, Collection<Diagnostic> diagnostics)
    {
        deleteMarkers(uri);
        if (diagnostics.isEmpty())
            return;
        IFile[] files =
            ResourcesPlugin.getWorkspace().getRoot().findFilesForLocationURI(
                uri);
        for (IFile file : files)
            if (file.exists())
                createMarkers(file, uri, diagnostics);
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
        getMarkers().values().forEach(markers -> markers.forEach(marker ->
        {
            try
            {
                marker.delete();
            }
            catch (CoreException e)
            {
                Activator.logError(e);
            }
        }));
        markers = null;
    }

    /**
     * TODO JavaDoc
     *
     * @param uri not <code>null</code>
     */
    public void deleteMarkers(URI uri)
    {
        Set<IMarker> markers = getMarkers().remove(uri);
        if (markers != null)
        {
            for (IMarker marker : markers)
            {
                try
                {
                    marker.delete();
                }
                catch (CoreException e)
                {
                    Activator.logError(e);
                }
            }
        }
    }

    /**
     * TODO JavaDoc
     *
     * @param file not <code>null</code>
     * @param uri not <code>null</code>
     * @param diagnostics not <code>null</code>
     */
    public void createMarkers(IFile file, URI uri,
        Collection<Diagnostic> diagnostics)
    {
        if (!file.exists() || diagnostics.isEmpty())
            return;
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        try
        {
            workspace.run(monitor -> doCreateMarkers(file, uri, diagnostics),
                workspace.getRuleFactory().markerRule(file),
                IWorkspace.AVOID_UPDATE, null);
        }
        catch (CoreException e)
        {
            Activator.logError(e);
        }
    }

    private void doCreateMarkers(IFile file, URI uri,
        Collection<Diagnostic> diagnostics)
    {
        Set<IMarker> markers = getMarkers().computeIfAbsent(uri,
            k -> new HashSet<>());
        for (Diagnostic diagnostic : diagnostics)
        {
            try
            {
                IMarker marker = file.createMarker(markerType);
                try
                {
                    Map<String, Object> attributes = new HashMap<>();
                    fillMarkerAttributes(attributes, file, uri, diagnostic);
                    marker.setAttributes(attributes);
                }
                catch (Throwable e)
                {
                    try
                    {
                        marker.delete();
                    }
                    catch (CoreException e2)
                    {
                        e.addSuppressed(e2);
                    }
                    throw e;
                }
                markers.add(marker);
            }
            catch (CoreException e)
            {
                if (!file.exists())
                    return;
                Activator.logError(e);
            }
        }
    }

    /**
     * TODO JavaDoc
     *
     * @param attributes never <code>null</code>
     * @param file never <code>null</code>
     * @param uri never <code>null</code>
     * @param diagnostic never <code>null</code>
     */
    protected void fillMarkerAttributes(Map<String, Object> attributes,
        IFile file, URI uri, Diagnostic diagnostic)
    {
        attributes.put(IMarker.SEVERITY, getMarkerSeverity(
            diagnostic.getSeverity()));
        attributes.put(IMarker.MESSAGE, diagnostic.getMessage());
        attributes.put(IMarker.LINE_NUMBER,
            diagnostic.getRange().getStart().getLine() + 1);
        try (IBuffer buffer = TextFileBuffer.forFile(file))
        {
            IRegion region = DocumentUtil.toRegion(buffer.getDocument(),
                diagnostic.getRange());
            attributes.put(IMarker.CHAR_START, region.getOffset());
            attributes.put(IMarker.CHAR_END, region.getOffset()
                + region.getLength());
        }
        catch (CoreException | BadLocationException e)
        {
            Activator.logError(e);
        }
        if (gson == null)
            gson = new Gson();
        attributes.put(DIAGNOSTIC_ATTRIBUTE, gson.toJson(diagnostic));
    }

    private static int getMarkerSeverity(DiagnosticSeverity severity)
    {
        if (severity == null)
            return IMarker.SEVERITY_ERROR;
        switch (severity)
        {
        case Error:
            return IMarker.SEVERITY_ERROR;
        case Warning:
            return IMarker.SEVERITY_WARNING;
        default:
            return IMarker.SEVERITY_INFO;
        }
    }

    /**
     * TODO JavaDoc
     *
     * @return the markers map (never <code>null</code>)
     */
    protected Map<URI, Set<IMarker>> getMarkers()
    {
        if (markers == null)
            markers = new HashMap<>();
        return markers;
    }
}
