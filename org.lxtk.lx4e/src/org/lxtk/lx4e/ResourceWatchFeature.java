/*******************************************************************************
 * Copyright (c) 2020, 2022 1C-Soft LLC.
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

import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.lsp4j.ClientCapabilities;
import org.eclipse.lsp4j.DidChangeWatchedFilesCapabilities;
import org.eclipse.lsp4j.DidChangeWatchedFilesParams;
import org.eclipse.lsp4j.DidChangeWatchedFilesRegistrationOptions;
import org.eclipse.lsp4j.DocumentFilter;
import org.eclipse.lsp4j.FileChangeType;
import org.eclipse.lsp4j.FileEvent;
import org.eclipse.lsp4j.FileSystemWatcher;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.Registration;
import org.eclipse.lsp4j.RelativePattern;
import org.eclipse.lsp4j.Unregistration;
import org.eclipse.lsp4j.WatchKind;
import org.eclipse.lsp4j.WorkspaceClientCapabilities;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.LanguageServer;
import org.lxtk.DocumentUri;
import org.lxtk.client.DynamicFeature;
import org.lxtk.jsonrpc.DefaultGson;
import org.lxtk.lx4e.internal.Activator;

import com.google.gson.JsonElement;

/**
 * A language client feature that supports dynamic registration for
 * the 'workspace/didChangeWatchedFiles' notification and notifies the
 * language server about relevant resource changes in the workspace.
 * <p>
 * This implementation is thread-safe.
 * </p>
 */
public class ResourceWatchFeature
    implements DynamicFeature<LanguageServer>
{
    private static final String METHOD = "workspace/didChangeWatchedFiles"; //$NON-NLS-1$
    private static final Set<String> METHODS = Collections.singleton(METHOD);

    private LanguageServer languageServer;
    private Map<String, Collection<Watcher>> registrations;
    private final Collection<Watcher> allWatchers = new HashSet<>();
    private final IWorkspace workspace = ResourcesPlugin.getWorkspace();
    private final IResourceChangeListener listener = event -> handle(event);

    /**
     * Adds the given watchers to the collection of watchers managed by this feature.
     * This method may be called even before the feature gets initialized.
     * <p>
     * Clients can use this method to force pushing file events to a language server
     * that does not explicitly register for them.
     * </p>
     *
     * @param watchers not <code>null</code>
     */
    public final synchronized void addWatchers(FileSystemWatcher... watchers)
    {
        if (watchers.length == 0)
            throw new IllegalArgumentException();

        Collection<Watcher> toAdd = new ArrayList<>();
        for (FileSystemWatcher watcher : watchers)
        {
            toAdd.add(Watcher.fromFileSystemWatcher(watcher));
        }

        if (allWatchers.isEmpty() && registrations != null)
            workspace.addResourceChangeListener(listener, IResourceChangeEvent.POST_CHANGE);

        allWatchers.addAll(toAdd);
    }

    @Override
    public final Set<String> getMethods()
    {
        return METHODS;
    }

    @Override
    public final void fillClientCapabilities(ClientCapabilities capabilities)
    {
        getOrCreateWorkspace(capabilities).setDidChangeWatchedFiles(
            new DidChangeWatchedFilesCapabilities(true));
    }

    @Override
    public final synchronized void initialize(LanguageServer server,
        InitializeResult initializeResult, List<DocumentFilter> documentSelector)
    {
        languageServer = server;
        registrations = new HashMap<>();

        if (!allWatchers.isEmpty())
            workspace.addResourceChangeListener(listener, IResourceChangeEvent.POST_CHANGE);
    }

    @Override
    public final synchronized void register(Registration registration)
    {
        if (!METHOD.equals(registration.getMethod()))
            throw new IllegalArgumentException();

        if (registrations == null)
            return;

        Object rO = registration.getRegisterOptions();
        if (rO == null)
            return;

        if (registrations.containsKey(registration.getId()))
            throw new IllegalArgumentException();

        DidChangeWatchedFilesRegistrationOptions options = DefaultGson.INSTANCE.fromJson(
            (JsonElement)rO, DidChangeWatchedFilesRegistrationOptions.class);
        if (options.getWatchers().isEmpty())
            return;

        Collection<Watcher> watchers = new ArrayList<>();
        for (FileSystemWatcher watcher : options.getWatchers())
        {
            watchers.add(Watcher.fromFileSystemWatcher(watcher));
        }

        registrations.put(registration.getId(), watchers);

        if (allWatchers.isEmpty())
            workspace.addResourceChangeListener(listener, IResourceChangeEvent.POST_CHANGE);

        allWatchers.addAll(watchers);
    }

    @Override
    public final synchronized void unregister(Unregistration unregistration)
    {
        if (!METHOD.equals(unregistration.getMethod()))
            throw new IllegalArgumentException();

        if (registrations == null)
            return;

        Collection<Watcher> watchers = registrations.remove(unregistration.getId());
        if (watchers == null)
            return;

        allWatchers.removeAll(watchers);

        if (allWatchers.isEmpty())
            workspace.removeResourceChangeListener(listener);
    }

    @Override
    public final synchronized void dispose()
    {
        registrations = null;

        if (!allWatchers.isEmpty())
        {
            workspace.removeResourceChangeListener(listener);
            allWatchers.clear();
        }
    }

    /**
     * Returns whether resource changes in the given project are to be watched.
     *
     * @param project never <code>null</code>
     * @return <code>true</code> if the given project is watched,
     *  and <code>false</code> otherwise
     */
    protected boolean isWatched(IProject project)
    {
        return true;
    }

    private synchronized void handle(IResourceChangeEvent event)
    {
        if (registrations == null || allWatchers.isEmpty())
            return;

        ResourceDeltaVisitor visitor = new ResourceDeltaVisitor();
        for (IResourceDelta delta : event.getDelta().getAffectedChildren())
        {
            if (isWatched((IProject)delta.getResource()))
            {
                try
                {
                    delta.accept(visitor);
                }
                catch (CoreException e)
                {
                    Activator.logError(e);
                }
            }
        }
        if (!visitor.changes.isEmpty())
        {
            languageServer.getWorkspaceService().didChangeWatchedFiles(
                new DidChangeWatchedFilesParams(visitor.changes));
        }
    }

    private FileEvent toFileEvent(IResourceDelta delta)
    {
        FileEvent event = new FileEvent();
        event.setUri(DocumentUri.convert(delta.getResource().getLocationURI()));
        event.setType(toFileChangeType(delta.getKind()));
        return event;
    }

    private FileChangeType toFileChangeType(int deltaKind)
    {
        switch (deltaKind)
        {
        case IResourceDelta.ADDED:
            return FileChangeType.Created;
        case IResourceDelta.REMOVED:
            return FileChangeType.Deleted;
        case IResourceDelta.CHANGED:
            return FileChangeType.Changed;
        default:
            throw new AssertionError();
        }
    }

    private boolean shouldReport(IResourceDelta delta)
    {
        if (delta.getKind() == IResourceDelta.CHANGED
            && (delta.getResource().getType() != IResource.FILE
                || (delta.getFlags() & IResourceDelta.CONTENT) == 0))
            return false;

        for (Watcher watcher : allWatchers)
        {
            if (shouldReport(delta, watcher))
                return true;
        }
        return false;
    }

    private boolean shouldReport(IResourceDelta delta, Watcher watcher)
    {
        if (!isWatchedKind(delta.getKind(), watcher.watchKinds))
            return false;

        IPath location = delta.getResource().getLocation();
        if (location == null)
            return false;

        return watcher.pathMatcher.matches(Paths.get(location.toOSString()));
    }

    private static boolean isWatchedKind(int deltaKind, Integer watchKinds)
    {
        if (watchKinds == null)
            return true;

        switch (deltaKind)
        {
        case IResourceDelta.ADDED:
            return (watchKinds & WatchKind.Create) != 0;
        case IResourceDelta.REMOVED:
            return (watchKinds & WatchKind.Delete) != 0;
        case IResourceDelta.CHANGED:
            return (watchKinds & WatchKind.Change) != 0;
        default:
            return false;
        }
    }

    private static WorkspaceClientCapabilities getOrCreateWorkspace(ClientCapabilities capabilities)
    {
        return Optional.ofNullable(capabilities.getWorkspace()).orElseGet(() ->
        {
            WorkspaceClientCapabilities workspace = new WorkspaceClientCapabilities();
            capabilities.setWorkspace(workspace);
            return workspace;
        });
    }

    private static class Watcher
    {
        final PathMatcher pathMatcher;
        final Integer watchKinds;

        static Watcher fromFileSystemWatcher(FileSystemWatcher watcher)
        {
            Either<String, RelativePattern> globPattern = watcher.getGlobPattern();
            if (!globPattern.isLeft())
                throw new IllegalArgumentException("Relative patterns are not supported"); //$NON-NLS-1$
            return new Watcher(
                FileSystems.getDefault().getPathMatcher("glob:" + globPattern.getLeft()), //$NON-NLS-1$
                watcher.getKind());
        }

        private Watcher(PathMatcher pathMatcher, Integer watchKinds)
        {
            this.pathMatcher = pathMatcher;
            this.watchKinds = watchKinds;
        }
    }

    private class ResourceDeltaVisitor
        implements IResourceDeltaVisitor
    {
        final List<FileEvent> changes = new ArrayList<>();

        @Override
        public boolean visit(IResourceDelta delta) throws CoreException
        {
            if (shouldReport(delta))
            {
                changes.add(toFileEvent(delta));
            }
            return true;
        }
    }
}
