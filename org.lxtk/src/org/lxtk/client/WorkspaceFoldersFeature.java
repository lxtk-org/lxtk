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
package org.lxtk.client;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.eclipse.lsp4j.ClientCapabilities;
import org.eclipse.lsp4j.DidChangeWorkspaceFoldersParams;
import org.eclipse.lsp4j.DocumentFilter;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.Registration;
import org.eclipse.lsp4j.Unregistration;
import org.eclipse.lsp4j.WorkspaceFoldersOptions;
import org.eclipse.lsp4j.WorkspaceServerCapabilities;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.LanguageServer;
import org.lxtk.WorkspaceFolder;
import org.lxtk.WorkspaceFoldersChangeEvent;
import org.lxtk.WorkspaceService;
import org.lxtk.util.Disposable;
import org.lxtk.util.UriUtil;

/**
 * A language client feature that notifies the language server about changes to the
 * collection of workspace folders managed by a given {@link WorkspaceService}.
 * <p>
 * This implementation is thread-safe.
 * </p>
 */
public final class WorkspaceFoldersFeature
    implements DynamicFeature<LanguageServer>
{
    private static final String METHOD = "workspace/didChangeWorkspaceFolders"; //$NON-NLS-1$
    private static final Set<String> METHODS = Collections.singleton(METHOD);

    private final WorkspaceService workspaceService;
    private LanguageServer languageServer;
    private Set<String> registrations;
    private Disposable subscription;

    /**
     * Constructor.
     *
     * @param workspaceService not <code>null</code>
     */
    public WorkspaceFoldersFeature(WorkspaceService workspaceService)
    {
        this.workspaceService = Objects.requireNonNull(workspaceService);
    }

    @Override
    public Set<String> getMethods()
    {
        return METHODS;
    }

    @Override
    public void fillClientCapabilities(ClientCapabilities capabilities)
    {
        ClientCapabilitiesUtil.getOrCreateWorkspace(capabilities).setWorkspaceFolders(true);
    }

    @Override
    public synchronized void fillInitializeParams(InitializeParams params)
    {
        params.setWorkspaceFolders(
            WorkspaceFolder.toProtocol(workspaceService.getWorkspaceFolders()));
    }

    @Override
    public synchronized void initialize(LanguageServer server, InitializeResult initializeResult,
        List<DocumentFilter> documentSelector)
    {
        languageServer = server;
        registrations = new HashSet<>();

        WorkspaceServerCapabilities workspace = initializeResult.getCapabilities().getWorkspace();
        if (workspace == null)
            return;

        WorkspaceFoldersOptions workspaceFolders = workspace.getWorkspaceFolders();
        if (workspaceFolders == null)
            return;

        Either<String, Boolean> changeNotifications = workspaceFolders.getChangeNotifications();
        if (changeNotifications == null)
            return;

        String id = changeNotifications.getLeft();
        if (id == null)
        {
            if (!Boolean.TRUE.equals(changeNotifications.getRight()))
                return;

            id = UUID.randomUUID().toString();
        }
        register(new Registration(id, METHOD));
    }

    @Override
    public synchronized void register(Registration registration)
    {
        if (!METHOD.equals(registration.getMethod()))
            throw new IllegalArgumentException();

        if (registrations == null)
            return;

        if (!registrations.add(registration.getId()))
            throw new IllegalArgumentException();

        if (subscription == null)
        {
            subscription =
                workspaceService.onDidChangeWorkspaceFolders().subscribe(this::onDidChange);
        }
    }

    @Override
    public synchronized void unregister(Unregistration unregistration)
    {
        if (registrations == null)
            return;

        if (!registrations.remove(unregistration.getId()))
            return;

        if (registrations.isEmpty())
        {
            if (subscription != null)
            {
                subscription.dispose();
                subscription = null;
            }
        }
    }

    @Override
    public synchronized void dispose()
    {
        registrations = null;
        if (subscription != null)
        {
            subscription.dispose();
            subscription = null;
        }
    }

    private synchronized void onDidChange(WorkspaceFoldersChangeEvent event)
    {
        List<org.eclipse.lsp4j.WorkspaceFolder> added =
            diff(event.getNewFolders(), event.getOldFolders());
        List<org.eclipse.lsp4j.WorkspaceFolder> removed =
            diff(event.getOldFolders(), event.getNewFolders());
        if (!added.isEmpty() || !removed.isEmpty())
        {
            languageServer.getWorkspaceService().didChangeWorkspaceFolders(
                new DidChangeWorkspaceFoldersParams(
                    new org.eclipse.lsp4j.WorkspaceFoldersChangeEvent(added, removed)));
        }
    }

    private static List<org.eclipse.lsp4j.WorkspaceFolder> diff(Collection<WorkspaceFolder> a,
        Collection<WorkspaceFolder> b)
    {
        List<org.eclipse.lsp4j.WorkspaceFolder> result = new ArrayList<>();
        Map<String, WorkspaceFolder> map = new HashMap<>();
        for (WorkspaceFolder f : b)
        {
            if (map.put(getKey(f.getUri()), f) != null)
                throw new IllegalArgumentException();
        }
        for (WorkspaceFolder f : a)
        {
            WorkspaceFolder match = map.get(getKey(f.getUri()));
            if (match == null || !f.getName().equals(match.getName()))
                result.add(f.toProtocol());
        }
        return result;
    }

    private static String getKey(URI uri)
    {
        String key = UriUtil.normalize(uri).toString();
        if (key.charAt(key.length() - 1) != '/')
            key += '/';
        return key;
    }
}
