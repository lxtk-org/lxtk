/*******************************************************************************
 * Copyright (c) 2019, 2021 1C-Soft LLC.
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

import java.time.Duration;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.lsp4j.ClientCapabilities;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.DocumentFilter;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.Registration;
import org.eclipse.lsp4j.SaveOptions;
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
import org.eclipse.lsp4j.jsonrpc.Endpoint;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.LanguageServer;
import org.lxtk.DocumentService;
import org.lxtk.DocumentUri;
import org.lxtk.TextDocument;
import org.lxtk.TextDocumentChangeEvent;
import org.lxtk.TextDocumentChangeEventMergeBuilder;
import org.lxtk.TextDocumentChangeEventMergeStrategy;
import org.lxtk.TextDocumentSaveEvent;
import org.lxtk.TextDocumentSnapshot;
import org.lxtk.jsonrpc.DefaultGson;
import org.lxtk.util.Disposable;

import com.google.gson.JsonElement;

/**
 * A language client feature that can dynamically synchronize text documents
 * managed by a given {@link DocumentService} to the language server.
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

    private final DocumentService documentService;
    private LanguageServer languageServer;
    private Map<String, Map<String, TextDocumentRegistrationOptions>> registrations;
    private final Map<String, Disposable> subscriptions = new HashMap<>();
    private final Map<TextDocument, Integer> syncedDocumentVersions = new HashMap<>();
    private final PendingChangeManager pendingChangeManager =
        new PendingChangeManager(this::flushPendingChange);

    /**
     * Constructor.
     *
     * @param documentService not <code>null</code>
     */
    public TextDocumentSyncFeature(DocumentService documentService)
    {
        this.documentService = Objects.requireNonNull(documentService);
    }

    /**
     * Sets the delay for which a change may be pending.
     *
     * @param delay not <code>null</code>
     */
    public void setPendingChangeDelay(Duration delay)
    {
        pendingChangeManager.setDelay(delay);
    }

    /**
     * Sets the strategy for change event merging.
     *
     * @param strategy may be <code>null</code>
     */
    public void setChangeEventMergeStrategy(TextDocumentChangeEventMergeStrategy strategy)
    {
        pendingChangeManager.setEventMergeStrategy(strategy);
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
    public Endpoint adviseServerEndpoint(Endpoint endpoint)
    {
        return new Endpoint()
        {
            @Override
            public CompletableFuture<?> request(String method, Object parameter)
            {
                flushPendingChange();
                return endpoint.request(method, parameter);
            }

            @Override
            public void notify(String method, Object parameter)
            {
                flushPendingChange();
                endpoint.notify(method, parameter);
            }
        };
    }

    @Override
    public synchronized void initialize(LanguageServer server, InitializeResult initializeResult,
        List<DocumentFilter> documentSelector)
    {
        languageServer = server;
        registrations = new HashMap<>();

        if (documentSelector == null)
            return;

        TextDocumentSyncOptions syncOptions =
            ServerCapabilitiesUtil.getTextDocumentSyncOptions(initializeResult.getCapabilities());

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

        Either<Boolean, SaveOptions> save = syncOptions.getSave();
        if (save != null && (Boolean.TRUE.equals(save.getLeft()) || save.isRight()))
        {
            SaveOptions saveOptions = save.isRight() ? save.getRight() : new SaveOptions();
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
                subsription = documentService.onDidAddTextDocument().subscribe(this::onDidAdd);
            }
            else if (DID_CLOSE.equals(registrationMethod))
            {
                subsription =
                    documentService.onDidRemoveTextDocument().subscribe(this::onDidRemove);
            }
            else if (DID_CHANGE.equals(registrationMethod))
            {
                Disposable willChange =
                    documentService.onWillChangeTextDocument().subscribe(this::onWillChange);
                Disposable didChange =
                    documentService.onDidChangeTextDocument().subscribe(this::onDidChange);
                subsription = () -> Disposable.disposeAll(willChange, didChange);
            }
            else if (DID_SAVE.equals(registrationMethod))
            {
                subsription = documentService.onDidSaveTextDocument().subscribe(this::onDidSave);
            }
            if (subsription != null)
                subscriptions.put(registrationMethod, subsription);
        }

        map.put(registration.getId(), registrationOptions);

        if (DID_OPEN.equals(registrationMethod))
        {
            for (TextDocument document : documentService.getTextDocuments())
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
        Collection<Disposable> disposables = new ArrayList<>(subscriptions.values());
        disposables.add(pendingChangeManager);
        subscriptions.clear();
        try
        {
            Disposable.disposeAll(disposables);
        }
        finally
        {
            syncedDocumentVersions.clear();
        }
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

    private synchronized void onWillChange(TextDocumentChangeEvent event)
    {
        if (event.getContentChanges().isEmpty())
            return;

        TextDocument document = event.getDocument();
        int snapshotVersion = event.getSnapshot().getVersion();

        Integer syncedDocumentVersion = syncedDocumentVersions.get(document);
        if (syncedDocumentVersion == null || syncedDocumentVersion > snapshotVersion)
            return;

        TextDocumentChangeRegistrationOptions registrationOptions =
            (TextDocumentChangeRegistrationOptions)getRegistrationOptions(document, DID_CHANGE);
        if (registrationOptions == null)
            return;

        pendingChangeManager.willAddChange(event, registrationOptions.getSyncKind());
    }

    private synchronized void onDidChange(TextDocumentChangeEvent event)
    {
        if (event.getContentChanges().isEmpty())
            return;

        TextDocument document = event.getDocument();
        int snapshotVersion = event.getSnapshot().getVersion();

        Integer syncedDocumentVersion = syncedDocumentVersions.get(document);
        if (syncedDocumentVersion == null || syncedDocumentVersion >= snapshotVersion)
            return;

        TextDocumentChangeRegistrationOptions registrationOptions =
            (TextDocumentChangeRegistrationOptions)getRegistrationOptions(document, DID_CHANGE);
        if (registrationOptions == null)
            return;

        pendingChangeManager.addChange(event, registrationOptions.getSyncKind());
    }

    private synchronized void flushPendingChange()
    {
        PendingChange change = pendingChangeManager.removeChange();
        if (change == null)
            return;

        TextDocument document = change.getDocument();

        Integer syncedDocumentVersion = syncedDocumentVersions.get(document);
        if (syncedDocumentVersion == null)
            return;

        TextDocumentChangeRegistrationOptions registrationOptions =
            (TextDocumentChangeRegistrationOptions)getRegistrationOptions(document, DID_CHANGE);
        if (registrationOptions == null)
            return;

        int version;
        List<TextDocumentContentChangeEvent> contentChanges;

        if (registrationOptions.getSyncKind() == TextDocumentSyncKind.Full
            || change.getSyncKind() == TextDocumentSyncKind.Full)
        {
            TextDocumentSnapshot snapshot = document.getLastChange().getSnapshot();
            version = snapshot.getVersion();
            contentChanges =
                Collections.singletonList(new TextDocumentContentChangeEvent(snapshot.getText()));
        }
        else
        {
            TextDocumentChangeEvent changeEvent = change.getEvent();
            version = changeEvent.getSnapshot().getVersion();
            contentChanges = changeEvent.getContentChanges();
        }

        if (syncedDocumentVersion >= version)
            return;

        DidChangeTextDocumentParams params = new DidChangeTextDocumentParams();
        params.setTextDocument(
            new VersionedTextDocumentIdentifier(DocumentUri.convert(document.getUri()), version));
        params.setContentChanges(contentChanges);

        languageServer.getTextDocumentService().didChange(params);
        syncedDocumentVersions.put(document, version);
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
        return documentService.getDocumentMatcher().getFirstMatch(map.values(),
            TextDocumentRegistrationOptions::getDocumentSelector, document.getUri(),
            document.getLanguageId()) != null;
    }

    private synchronized TextDocumentRegistrationOptions getRegistrationOptions(
        TextDocument document, String method)
    {
        Map<String, TextDocumentRegistrationOptions> map = registrations.get(method);
        if (map == null)
            return null;
        return documentService.getDocumentMatcher().getBestMatch(map.values(),
            TextDocumentRegistrationOptions::getDocumentSelector, document.getUri(),
            document.getLanguageId());
    }

    private boolean isMatch(TextDocument document,
        TextDocumentRegistrationOptions registrationOptions)
    {
        return documentService.getDocumentMatcher().isMatch(
            registrationOptions.getDocumentSelector(), document.getUri(), document.getLanguageId());
    }

    private static TextDocumentItem toTextDocumentItem(TextDocumentSnapshot snapshot)
    {
        TextDocument document = snapshot.getDocument();
        return new TextDocumentItem(DocumentUri.convert(document.getUri()),
            document.getLanguageId(), snapshot.getVersion(), snapshot.getText());
    }

    private interface PendingChange
    {
        TextDocument getDocument();

        TextDocumentSyncKind getSyncKind();

        boolean isEmpty(); // this is orthogonal to whether getEvent() returns null

        TextDocumentChangeEvent getEvent();

        void add(TextDocumentChangeEvent event);
    }

    private static class FullChange
        implements PendingChange
    {
        private final TextDocument document;

        FullChange(TextDocument document)
        {
            this.document = Objects.requireNonNull(document);
        }

        @Override
        public TextDocument getDocument()
        {
            return document;
        }

        @Override
        public TextDocumentSyncKind getSyncKind()
        {
            return TextDocumentSyncKind.Full;
        }

        @Override
        public boolean isEmpty()
        {
            return false; // full change is never empty
        }

        @Override
        public TextDocumentChangeEvent getEvent()
        {
            return null;
        }

        @Override
        public void add(TextDocumentChangeEvent event)
        {
        }
    }

    private static class AccumulatingChange
        implements PendingChange
    {
        private final TextDocument document;
        private final List<TextDocumentContentChangeEvent> contentChanges = new ArrayList<>();
        private TextDocumentSnapshot snapshot;

        AccumulatingChange(TextDocument document)
        {
            this.document = Objects.requireNonNull(document);
        }

        @Override
        public TextDocument getDocument()
        {
            return document;
        }

        @Override
        public TextDocumentSyncKind getSyncKind()
        {
            return TextDocumentSyncKind.Incremental;
        }

        @Override
        public boolean isEmpty()
        {
            return snapshot == null;
        }

        @Override
        public TextDocumentChangeEvent getEvent()
        {
            if (snapshot == null)
                return null;

            return new TextDocumentChangeEvent(snapshot, contentChanges);
        }

        @Override
        public void add(TextDocumentChangeEvent event)
        {
            contentChanges.addAll(event.getContentChanges());
            snapshot = event.getSnapshot();
        }
    }

    private static class MergingChange
        implements PendingChange
    {
        private final TextDocument document;
        private final TextDocumentChangeEventMergeBuilder builder;

        MergingChange(TextDocument document, TextDocumentChangeEventMergeBuilder builder)
        {
            this.document = Objects.requireNonNull(document);
            this.builder = Objects.requireNonNull(builder);
        }

        @Override
        public TextDocument getDocument()
        {
            return document;
        }

        @Override
        public TextDocumentSyncKind getSyncKind()
        {
            return TextDocumentSyncKind.Incremental;
        }

        @Override
        public boolean isEmpty()
        {
            return !builder.hasResult();
        }

        @Override
        public TextDocumentChangeEvent getEvent()
        {
            return builder.getResult();
        }

        @Override
        public void add(TextDocumentChangeEvent event)
        {
            builder.merge(event);
        }
    }

    private static class PendingChangeManager
        implements Disposable
    {
        private final Runnable flushCallback;
        private long delay = 500;
        private ScheduledChange change;
        private ScheduledExecutorService executor;
        private TextDocumentChangeEventMergeStrategy eventMergeStrategy;

        PendingChangeManager(Runnable flushCallback)
        {
            this.flushCallback = Objects.requireNonNull(flushCallback);
        }

        void setDelay(Duration delay)
        {
            this.delay = delay.toMillis();
        }

        void setEventMergeStrategy(TextDocumentChangeEventMergeStrategy eventMergeStrategy)
        {
            this.eventMergeStrategy = eventMergeStrategy;
        }

        // note that willAddChange can be called twice in a row (for different documents)
        // or not called at all (if the document does not support willChange notifications)
        void willAddChange(TextDocumentChangeEvent event, TextDocumentSyncKind syncKind)
        {
            if (change != null)
            {
                change.cancel();

                if (change.getDocument() != event.getDocument())
                {
                    flushCallback.run();
                    change = null;
                }
            }

            if (change == null && syncKind != TextDocumentSyncKind.Full
                && eventMergeStrategy != null)
            {
                change = new ScheduledChange(new MergingChange(event.getDocument(),
                    eventMergeStrategy.startMerging(event.getSnapshot().getText())));
            }
        }

        // note that addChange can be called twice in a row;
        // the corresponding willAddChange might not have been called
        void addChange(TextDocumentChangeEvent event, TextDocumentSyncKind syncKind)
        {
            if (change != null)
            {
                change.cancel();

                if (change.getDocument() != event.getDocument())
                {
                    flushCallback.run();
                    change = null;
                }
            }

            if (change == null)
            {
                change = new ScheduledChange(
                    syncKind == TextDocumentSyncKind.Full ? new FullChange(event.getDocument())
                        : new AccumulatingChange(event.getDocument()));
            }

            change.add(event);

            if (executor == null)
                executor = Executors.newSingleThreadScheduledExecutor();

            change.future = executor.schedule(flushCallback, delay, TimeUnit.MILLISECONDS);
        }

        PendingChange removeChange()
        {
            if (change == null || change.isEmpty())
                return null;

            PendingChange result = change;
            change = null;
            return result;
        }

        @Override
        public void dispose()
        {
            if (change != null)
            {
                change.cancel();

                flushCallback.run();
                change = null;
            }
            if (executor != null)
            {
                executor.shutdown();
                executor = null;
            }
        }

        private static class ScheduledChange
            implements PendingChange
        {
            private final PendingChange delegate;
            Future<?> future;

            ScheduledChange(PendingChange delegate)
            {
                this.delegate = Objects.requireNonNull(delegate);
            }

            @Override
            public TextDocument getDocument()
            {
                return delegate.getDocument();
            }

            @Override
            public TextDocumentSyncKind getSyncKind()
            {
                return delegate.getSyncKind();
            }

            @Override
            public boolean isEmpty()
            {
                return delegate.isEmpty();
            }

            @Override
            public TextDocumentChangeEvent getEvent()
            {
                return delegate.getEvent();
            }

            @Override
            public void add(TextDocumentChangeEvent event)
            {
                delegate.add(event);
            }

            void cancel()
            {
                if (future != null)
                {
                    future.cancel(false);
                    future = null;
                }
            }
        }
    }
}
