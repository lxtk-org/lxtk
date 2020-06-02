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
 *******************************************************************************/
package org.lxtk.client;

import java.util.ArrayList;
import java.util.Arrays;
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
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.DocumentFilter;
import org.eclipse.lsp4j.Registration;
import org.eclipse.lsp4j.SaveOptions;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.SynchronizationCapabilities;
import org.eclipse.lsp4j.TextDocumentChangeRegistrationOptions;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.TextDocumentRegistrationOptions;
import org.eclipse.lsp4j.TextDocumentSaveRegistrationOptions;
import org.eclipse.lsp4j.TextDocumentSyncKind;
import org.eclipse.lsp4j.TextDocumentSyncOptions;
import org.eclipse.lsp4j.Unregistration;
import org.eclipse.lsp4j.VersionedTextDocumentIdentifier;
import org.eclipse.lsp4j.services.LanguageServer;
import org.lxtk.DocumentUri;
import org.lxtk.TextDocument;
import org.lxtk.TextDocumentChangeEvent;
import org.lxtk.TextDocumentSaveEvent;
import org.lxtk.TextDocumentSnapshot;
import org.lxtk.Workspace;
import org.lxtk.jsonrpc.DefaultGson;
import org.lxtk.util.Disposable;

import com.google.gson.JsonElement;

/**
 * A language client feature that can dynamically synchronize text documents
 * contained in a given {@link Workspace} to the language server.
 * <p>
 * This implementation is thread-safe.
 * </p>
 */
public final class TextDocumentSyncFeature
    implements DynamicFeature<LanguageServer>
{
    private static final String DID_OPEN = "textDocument/didOpen"; //$NON-NLS-1$
    private static final String DID_CLOSE = "textDocument/didClose"; //$NON-NLS-1$
    private static final String DID_CHANGE = "textDocument/didChange"; //$NON-NLS-1$
    private static final String DID_SAVE = "textDocument/didSave"; //$NON-NLS-1$
    private static final Set<String> METHODS = Collections.unmodifiableSet(
        new HashSet<>(Arrays.asList(DID_OPEN, DID_CLOSE, DID_CHANGE, DID_SAVE)));

    private final Workspace workspace;
    private LanguageServer languageServer;
    private Map<String, Map<String, TextDocumentRegistrationOptions>> registrations;
    private final Map<String, Disposable> subscriptions = new HashMap<>();
    private final Map<TextDocument, Integer> syncedDocumentVersions = new HashMap<>();

    /**
     * Constructor.
     *
     * @param workspace not <code>null</code>
     */
    public TextDocumentSyncFeature(Workspace workspace)
    {
        this.workspace = Objects.requireNonNull(workspace);
    }

    @Override
    public Set<String> getMethods()
    {
        return METHODS;
    }

    @Override
    public void fillClientCapabilities(ClientCapabilities capabilities)
    {
        SynchronizationCapabilities syncronization =
            ClientCapabilitiesUtil.getOrCreateSynchronization(
                ClientCapabilitiesUtil.getOrCreateTextDocument(capabilities));
        syncronization.setDynamicRegistration(true);
        syncronization.setDidSave(true);
    }

    @Override
    public synchronized void initialize(LanguageServer server, ServerCapabilities capabilities,
        List<DocumentFilter> documentSelector)
    {
        languageServer = server;
        registrations = new HashMap<>();

        if (documentSelector == null)
            return;

        TextDocumentSyncOptions syncOptions =
            ServerCapabilitiesUtil.getTextDocumentSyncOptions(capabilities);

        if (Boolean.TRUE.equals(syncOptions.getOpenClose()))
        {
            register(new Registration(UUID.randomUUID().toString(), DID_OPEN,
                new TextDocumentRegistrationOptions(documentSelector)));
            register(new Registration(UUID.randomUUID().toString(), DID_CLOSE,
                new TextDocumentRegistrationOptions(documentSelector)));
        }

        TextDocumentSyncKind syncKind = syncOptions.getChange();
        if (syncKind != null && syncKind != TextDocumentSyncKind.None)
        {
            TextDocumentChangeRegistrationOptions registrationOptions =
                new TextDocumentChangeRegistrationOptions(syncKind);
            registrationOptions.setDocumentSelector(documentSelector);
            register(
                new Registration(UUID.randomUUID().toString(), DID_CHANGE, registrationOptions));
        }

        SaveOptions saveOptions = syncOptions.getSave();
        if (saveOptions != null)
        {
            TextDocumentSaveRegistrationOptions registrationOptions =
                new TextDocumentSaveRegistrationOptions(saveOptions.getIncludeText());
            registrationOptions.setDocumentSelector(documentSelector);
            register(new Registration(UUID.randomUUID().toString(), DID_SAVE, registrationOptions));
        }
    }

    @Override
    public synchronized void register(Registration registration)
    {
        String registrationMethod = registration.getMethod();
        if (!METHODS.contains(registrationMethod))
            throw new IllegalArgumentException();

        Class<? extends TextDocumentRegistrationOptions> optionsClass;
        if (DID_CHANGE.equals(registrationMethod))
            optionsClass = TextDocumentChangeRegistrationOptions.class;
        else if (DID_SAVE.equals(registrationMethod))
            optionsClass = TextDocumentSaveRegistrationOptions.class;
        else
            optionsClass = TextDocumentRegistrationOptions.class;

        Object rO = registration.getRegisterOptions();
        TextDocumentRegistrationOptions registrationOptions = rO instanceof JsonElement
            ? DefaultGson.INSTANCE.fromJson((JsonElement)rO, optionsClass) : optionsClass.cast(rO);
        if (registrationOptions == null || registrationOptions.getDocumentSelector() == null)
            return;

        if (registrations == null)
            return;

        Map<String, TextDocumentRegistrationOptions> map = registrations.get(registrationMethod);
        if (map != null && map.containsKey(registration.getId()))
            throw new IllegalArgumentException();

        if (map == null)
        {
            map = new HashMap<>();
            registrations.put(registrationMethod, map);

            Disposable subsription = null;
            if (DID_OPEN.equals(registrationMethod))
            {
                subsription = workspace.onDidAddTextDocument().subscribe(this::onDidAdd);
            }
            else if (DID_CLOSE.equals(registrationMethod))
            {
                subsription = workspace.onDidRemoveTextDocument().subscribe(this::onDidRemove);
            }
            else if (DID_CHANGE.equals(registrationMethod))
            {
                subsription = workspace.onDidChangeTextDocument().subscribe(this::onDidChange);
            }
            else if (DID_SAVE.equals(registrationMethod))
            {
                subsription = workspace.onDidSaveTextDocument().subscribe(this::onDidSave);
            }
            if (subsription != null)
                subscriptions.put(registrationMethod, subsription);
        }

        map.put(registration.getId(), registrationOptions);

        if (DID_OPEN.equals(registrationMethod))
        {
            for (TextDocument document : workspace.getTextDocuments())
            {
                if (!syncedDocumentVersions.containsKey(document)
                    && isMatch(document, registrationOptions))
                {
                    TextDocumentSnapshot snapshot = document.getLastChange().getSnapshot();
                    languageServer.getTextDocumentService().didOpen(
                        new DidOpenTextDocumentParams(toTextDocumentItem(snapshot)));
                    syncedDocumentVersions.put(document, snapshot.getVersion());
                }
            }
        }
    }

    @Override
    public synchronized void unregister(Unregistration unregistration)
    {
        if (registrations == null)
            return;

        Map<String, TextDocumentRegistrationOptions> map =
            registrations.get(unregistration.getMethod());
        if (map == null)
            return;

        TextDocumentRegistrationOptions registrationOptions = map.remove(unregistration.getId());
        if (registrationOptions == null)
            return;

        if (map.isEmpty())
        {
            registrations.remove(unregistration.getMethod());

            Disposable subscription = subscriptions.remove(unregistration.getMethod());
            if (subscription != null)
                subscription.dispose();
        }

        if (DID_CLOSE.equals(unregistration.getMethod()))
        {
            for (TextDocument document : syncedDocumentVersions.keySet())
            {
                if (isMatch(document, registrationOptions)
                    && !hasMatchingRegistration(document, DID_CLOSE))
                {
                    languageServer.getTextDocumentService().didClose(new DidCloseTextDocumentParams(
                        DocumentUri.toTextDocumentIdentifier(document.getUri())));
                    syncedDocumentVersions.remove(document);
                }
            }
        }
    }

    @Override
    public synchronized void dispose()
    {
        registrations = null;
        syncedDocumentVersions.clear();
        Collection<Disposable> disposables = new ArrayList<>(subscriptions.values());
        subscriptions.clear();
        Disposable.disposeAll(disposables);
    }

    private synchronized void onDidAdd(TextDocument document)
    {
        if (!syncedDocumentVersions.containsKey(document)
            && hasMatchingRegistration(document, DID_OPEN))
        {
            TextDocumentSnapshot snapshot = document.getLastChange().getSnapshot();
            languageServer.getTextDocumentService().didOpen(
                new DidOpenTextDocumentParams(toTextDocumentItem(snapshot)));
            syncedDocumentVersions.put(document, snapshot.getVersion());
        }
    }

    private synchronized void onDidRemove(TextDocument document)
    {
        if (syncedDocumentVersions.containsKey(document)
            && hasMatchingRegistration(document, DID_CLOSE))
        {
            languageServer.getTextDocumentService().didClose(new DidCloseTextDocumentParams(
                DocumentUri.toTextDocumentIdentifier(document.getUri())));
            syncedDocumentVersions.remove(document);
        }
    }

    private synchronized void onDidChange(TextDocumentChangeEvent event)
    {
        if (event.getContentChanges().isEmpty())
            return;

        TextDocument document = event.getDocument();
        TextDocumentSnapshot snapshot = event.getSnapshot();
        int snapshotVersion = snapshot.getVersion();

        Integer syncedDocumentVersion = syncedDocumentVersions.get(document);
        if (syncedDocumentVersion == null || syncedDocumentVersion >= snapshotVersion)
            return;

        TextDocumentChangeRegistrationOptions registrationOptions =
            (TextDocumentChangeRegistrationOptions)getRegistrationOptions(document, DID_CHANGE);
        if (registrationOptions == null)
            return;

        DidChangeTextDocumentParams params = new DidChangeTextDocumentParams();
        params.setTextDocument(new VersionedTextDocumentIdentifier(
            DocumentUri.convert(document.getUri()), snapshotVersion));
        params.setContentChanges(registrationOptions.getSyncKind() == TextDocumentSyncKind.Full
            ? Collections.singletonList(new TextDocumentContentChangeEvent(snapshot.getText()))
            : event.getContentChanges());

        languageServer.getTextDocumentService().didChange(params);
        syncedDocumentVersions.put(document, snapshotVersion);
    }

    private synchronized void onDidSave(TextDocumentSaveEvent event)
    {
        TextDocument document = event.getDocument();

        TextDocumentSaveRegistrationOptions registrationOptions =
            (TextDocumentSaveRegistrationOptions)getRegistrationOptions(document, DID_SAVE);
        if (registrationOptions == null)
            return;

        DidSaveTextDocumentParams params = new DidSaveTextDocumentParams();
        params.setTextDocument(DocumentUri.toTextDocumentIdentifier(document.getUri()));
        if (Boolean.TRUE.equals(registrationOptions.getIncludeText()))
            params.setText(event.getText());

        languageServer.getTextDocumentService().didSave(params);
    }

    private synchronized boolean hasMatchingRegistration(TextDocument document, String method)
    {
        Map<String, TextDocumentRegistrationOptions> map = registrations.get(method);
        if (map == null)
            return false;
        return workspace.getDocumentMatcher().getFirstMatch(map.values(),
            TextDocumentRegistrationOptions::getDocumentSelector, document.getUri(),
            document.getLanguageId()) != null;
    }

    private synchronized TextDocumentRegistrationOptions getRegistrationOptions(
        TextDocument document, String method)
    {
        Map<String, TextDocumentRegistrationOptions> map = registrations.get(method);
        if (map == null)
            return null;
        return workspace.getDocumentMatcher().getBestMatch(map.values(),
            TextDocumentRegistrationOptions::getDocumentSelector, document.getUri(),
            document.getLanguageId());
    }

    private boolean isMatch(TextDocument document,
        TextDocumentRegistrationOptions registrationOptions)
    {
        return workspace.getDocumentMatcher().isMatch(registrationOptions.getDocumentSelector(),
            document.getUri(), document.getLanguageId());
    }

    private static TextDocumentItem toTextDocumentItem(TextDocumentSnapshot snapshot)
    {
        TextDocument document = snapshot.getDocument();
        return new TextDocumentItem(DocumentUri.convert(document.getUri()),
            document.getLanguageId(), snapshot.getVersion(), snapshot.getText());
    }
}
