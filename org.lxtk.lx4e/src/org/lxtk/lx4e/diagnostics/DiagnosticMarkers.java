/*******************************************************************************
 * Copyright (c) 2019, 2020 1C-Soft LLC.
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Vladimir Piskarev (1C) - initial API and implementation
 *     Alexander Kozinko (1C)
 *******************************************************************************/
package org.lxtk.lx4e.diagnostics;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiConsumer;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.handly.buffer.IBuffer;
import org.eclipse.handly.buffer.TextFileBuffer;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IRegion;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.lxtk.jsonrpc.DefaultGson;
import org.lxtk.lx4e.DocumentUtil;
import org.lxtk.lx4e.internal.Activator;
import org.lxtk.util.Disposable;

/**
 * Manages resource markers representing LSP diagnostics.
 * <p>
 * This implementation assumes that the given marker type is <b>not</b> persistent.
 * </p>
 * <p>
 * An instance of this class is <b>not</b> safe for concurrent access by
 * multiple threads.
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

    private static final String SOURCE_UUID_ATTRIBUTE = "sourceUuid"; //$NON-NLS-1$

    private static final IMarker[] NO_MARKERS = new IMarker[0];

    /**
     * Holds the given marker type.
     */
    protected final String markerType;

    /**
     * Holds the markers map.
     *
     * @see #getMarkers()
     */
    protected Map<URI, Collection<IMarker>> markers;

    private final IWorkspace workspace = ResourcesPlugin.getWorkspace();
    private final IResourceChangeListener moveProcessor = new MoveProcessor();
    private final String sourceUuid = UUID.randomUUID().toString();

    /**
     * Constructor.
     *
     * @param markerType the marker type for markers created and managed
     *  by this object (not <code>null</code>)
     */
    public DiagnosticMarkers(String markerType)
    {
        this.markerType = Objects.requireNonNull(markerType);
        workspace.addResourceChangeListener(moveProcessor, IResourceChangeEvent.POST_CHANGE);
    }

    @Override
    public void accept(URI uri, Collection<Diagnostic> diagnostics)
    {
        try
        {
            workspace.run(monitor ->
            {
                deleteMarkers(uri);
                if (diagnostics == null || diagnostics.isEmpty())
                    return;
                IFile[] files = workspace.getRoot().findFilesForLocationURI(uri);
                for (IFile file : files)
                {
                    if (file.exists())
                        createMarkers(file, uri, diagnostics);
                }
            }, null, IWorkspace.AVOID_UPDATE, null);
        }
        catch (CoreException e)
        {
            Activator.logError(e);
        }
    }

    @Override
    public void dispose()
    {
        workspace.removeResourceChangeListener(moveProcessor);
        clear();
    }

    /**
     * Deletes all markers currently managed by this object.
     */
    public void clear()
    {
        Collection<IMarker> allMarkers = new ArrayList<>();
        getMarkers().values().forEach(markers -> allMarkers.addAll(markers));
        try
        {
            workspace.deleteMarkers(allMarkers.toArray(NO_MARKERS));
        }
        catch (CoreException e)
        {
            Activator.logError(e);
        }
        markers = null;
    }

    /**
     * Deletes the markers currently managed by this object for the given URI.
     *
     * @param uri not <code>null</code>
     */
    public void deleteMarkers(URI uri)
    {
        Collection<IMarker> markers = getMarkers().remove(uri);
        if (markers != null)
        {
            try
            {
                workspace.deleteMarkers(markers.toArray(NO_MARKERS));
            }
            catch (CoreException e)
            {
                Activator.logError(e);
            }
        }
    }

    /**
     * Creates markers on the given {@link IFile} that represent the given
     * diagnostics for the given URI.
     *
     * @param file not <code>null</code>
     * @param uri not <code>null</code>
     * @param diagnostics not <code>null</code>
     */
    public void createMarkers(IFile file, URI uri, Collection<Diagnostic> diagnostics)
    {
        if (!file.exists() || diagnostics.isEmpty())
            return;
        try
        {
            workspace.run(monitor -> doCreateMarkers(file, uri, diagnostics),
                workspace.getRuleFactory().markerRule(file), IWorkspace.AVOID_UPDATE, null);
        }
        catch (CoreException e)
        {
            Activator.logError(e);
        }
    }

    private void doCreateMarkers(IFile file, URI uri, Collection<Diagnostic> diagnostics)
    {
        Collection<IMarker> markers = getMarkers().computeIfAbsent(uri, k -> new ArrayList<>());
        for (Diagnostic diagnostic : diagnostics)
        {
            try
            {
                IMarker marker = file.createMarker(markerType);
                try
                {
                    Map<String, Object> attributes = new HashMap<>();
                    fillMarkerAttributes(attributes, file, uri, diagnostic);
                    attributes.put(SOURCE_UUID_ATTRIBUTE, sourceUuid);
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
     * Fills attributes of a marker representing the given diagnostic for
     * the given URI on the given file.
     *
     * @param attributes never <code>null</code>
     * @param file never <code>null</code>
     * @param uri never <code>null</code>
     * @param diagnostic never <code>null</code>
     */
    protected void fillMarkerAttributes(Map<String, Object> attributes, IFile file, URI uri,
        Diagnostic diagnostic)
    {
        attributes.put(IMarker.SEVERITY, getMarkerSeverity(diagnostic.getSeverity()));
        attributes.put(IMarker.MESSAGE, diagnostic.getMessage());
        attributes.put(IMarker.LINE_NUMBER, diagnostic.getRange().getStart().getLine() + 1);
        try (IBuffer buffer = TextFileBuffer.forFile(file))
        {
            IRegion region = DocumentUtil.toRegion(buffer.getDocument(), diagnostic.getRange());
            attributes.put(IMarker.CHAR_START, region.getOffset());
            attributes.put(IMarker.CHAR_END, region.getOffset() + region.getLength());
        }
        catch (CoreException | BadLocationException e)
        {
            Activator.logError(e);
        }
        attributes.put(DIAGNOSTIC_ATTRIBUTE, DefaultGson.INSTANCE.toJson(diagnostic));
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
     * Returns the markers map, creating it if necessary.
     *
     * @return the markers map (never <code>null</code>)
     */
    protected Map<URI, Collection<IMarker>> getMarkers()
    {
        if (markers == null)
            markers = new HashMap<>();
        return markers;
    }

    private class MoveProcessor
        implements IResourceChangeListener
    {
        @Override
        public void resourceChanged(IResourceChangeEvent event)
        {
            Collection<IMarker> markers = new ArrayList<>();
            collectMarkersAddedByMove(event.getDelta(), markers);
            if (markers.isEmpty())
                return;
            WorkspaceJob job = new WorkspaceJob("Delete Markers") //$NON-NLS-1$
            {
                @Override
                public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException
                {
                    workspace.deleteMarkers(markers.toArray(NO_MARKERS));
                    return Status.OK_STATUS;
                }
            };
            job.setSystem(true);
            job.schedule();
        }

        private void collectMarkersAddedByMove(IResourceDelta delta, Collection<IMarker> markers)
        {
            int flags = IResourceDelta.MOVED_FROM | IResourceDelta.MARKERS;
            if ((delta.getFlags() & flags) == flags)
            {
                IMarkerDelta[] markerDeltas = delta.getMarkerDeltas();
                for (IMarkerDelta markerDelta : markerDeltas)
                {
                    IMarker marker = markerDelta.getMarker();
                    if (sourceUuid.equals(marker.getAttribute(SOURCE_UUID_ATTRIBUTE, null)))
                    {
                        markers.add(marker);
                    }
                }
            }
            IResourceDelta[] children =
                delta.getAffectedChildren(IResourceDelta.ADDED | IResourceDelta.CHANGED);
            for (IResourceDelta child : children)
                collectMarkersAddedByMove(child, markers);
        }
    }
}
