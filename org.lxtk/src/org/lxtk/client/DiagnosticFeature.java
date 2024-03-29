/*******************************************************************************
 * Copyright (c) 2022 1C-Soft LLC.
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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.eclipse.lsp4j.ClientCapabilities;
import org.eclipse.lsp4j.DiagnosticCapabilities;
import org.eclipse.lsp4j.DiagnosticRegistrationOptions;
import org.eclipse.lsp4j.DiagnosticWorkspaceCapabilities;
import org.eclipse.lsp4j.DocumentDiagnosticParams;
import org.eclipse.lsp4j.DocumentDiagnosticReport;
import org.eclipse.lsp4j.DocumentFilter;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.Registration;
import org.eclipse.lsp4j.Unregistration;
import org.eclipse.lsp4j.WorkspaceDiagnosticParams;
import org.eclipse.lsp4j.WorkspaceDiagnosticReport;
import org.eclipse.lsp4j.services.LanguageServer;
import org.lxtk.DefaultDocumentMatcher;
import org.lxtk.DiagnosticProvider;
import org.lxtk.DocumentMatcher;
import org.lxtk.ProgressService;
import org.lxtk.TextDocument;
import org.lxtk.TextDocumentChangeEvent;
import org.lxtk.UiDocumentService;
import org.lxtk.client.DiagnosticRequestor.TriggeringContext;
import org.lxtk.client.DiagnosticRequestor.TriggeringReason;
import org.lxtk.jsonrpc.DefaultGson;
import org.lxtk.util.Disposable;
import org.lxtk.util.EventEmitter;
import org.lxtk.util.EventStream;
import org.lxtk.util.SafeRun;

import com.google.gson.JsonElement;

/**
 * Provides support for diagnostic pulls.
 * <p>
 * This implementation is thread-safe.
 * </p>
 */
public final class DiagnosticFeature
    implements DynamicFeature<LanguageServer>
{
    private static final String METHOD = "textDocument/diagnostic"; //$NON-NLS-1$
    private static final Set<String> METHODS = Collections.singleton(METHOD);

    private final CompletableFuture<? extends UiDocumentService> uiDocumentServiceFuture;
    private final EventStream<TextDocumentChangeEvent> onDidFlushPendingChange;
    private final Function<DiagnosticProvider, DiagnosticRequestor> diagnosticRequestorFactory;
    private final Function<DiagnosticProvider,
        WorkspaceDiagnosticRequestor> workspaceDiagnosticRequestorFactory;
    private final EventEmitter<Set<TextDocument>> onRefreshDiagnostics = new EventEmitter<>();
    private final Set<TextDocument> trackedDocuments = new HashSet<>();

    private DocumentMatcher documentMatcher = DefaultDocumentMatcher.INSTANCE;
    private AbstractLanguageClient<? extends LanguageServer> languageClient;
    private Consumer<Throwable> logger;
    private Disposable clientDisposable;
    private LanguageServer languageServer;
    private Map<String, RegistrationData> registrations;
    private Set<String> pendingRegistrations;
    private CompletableFuture<Disposable> diagnosticPullFuture;

    /**
     * Constructor.
     *
     * @param uiDocumentServiceFuture not <code>null</code>
     * @param textDocumentSyncFeature not <code>null</code>
     * @param diagnosticRequestorFactory not <code>null</code>
     * @param workspaceDiagnosticRequestorFactory may be <code>null</code>, in which case
     *  workspace diagnostics will not be supported by this feature
     */
    public DiagnosticFeature(CompletableFuture<? extends UiDocumentService> uiDocumentServiceFuture,
        TextDocumentSyncFeature textDocumentSyncFeature,
        Function<DiagnosticProvider, DiagnosticRequestor> diagnosticRequestorFactory, //
        Function<DiagnosticProvider,
            WorkspaceDiagnosticRequestor> workspaceDiagnosticRequestorFactory)
    {
        this.uiDocumentServiceFuture = Objects.requireNonNull(uiDocumentServiceFuture);
        this.onDidFlushPendingChange = textDocumentSyncFeature.onDidFlushPendingChange();
        this.diagnosticRequestorFactory = Objects.requireNonNull(diagnosticRequestorFactory);
        this.workspaceDiagnosticRequestorFactory = workspaceDiagnosticRequestorFactory;
    }

    /**
     * Sets the document matcher for this feature.
     *
     * @param documentMatcher not <code>null</code>
     */
    public void setDocumentMatcher(DocumentMatcher documentMatcher)
    {
        this.documentMatcher = Objects.requireNonNull(documentMatcher);
    }

    @Override
    public void setLanguageClient(AbstractLanguageClient<? extends LanguageServer> client)
    {
        languageClient = Objects.requireNonNull(client);
        logger = client.log()::error;
        clientDisposable =
            client.onRefreshDiagnostics().subscribe(e -> onRefreshDiagnostics.emit(null, logger));
    }

    @Override
    public Set<String> getMethods()
    {
        return METHODS;
    }

    @Override
    public void fillClientCapabilities(ClientCapabilities capabilities)
    {
        DiagnosticCapabilities diagnostic = new DiagnosticCapabilities();
        diagnostic.setDynamicRegistration(true);
        diagnostic.setRelatedDocumentSupport(false);
        ClientCapabilitiesUtil.getOrCreateTextDocument(capabilities).setDiagnostic(diagnostic);

        DiagnosticWorkspaceCapabilities workspaceDiagnostics =
            new DiagnosticWorkspaceCapabilities();
        workspaceDiagnostics.setRefreshSupport(true);
        ClientCapabilitiesUtil.getOrCreateWorkspace(capabilities).setDiagnostics(
            workspaceDiagnostics);
    }

    @Override
    public synchronized void initialize(LanguageServer server, InitializeResult initializeResult,
        List<DocumentFilter> documentSelector)
    {
        languageServer = Objects.requireNonNull(server);
        registrations = new HashMap<>();

        DiagnosticRegistrationOptions options =
            initializeResult.getCapabilities().getDiagnosticProvider();
        if (options == null)
            return;

        String id = options.getId();
        if (id == null)
            id = UUID.randomUUID().toString();

        if (options.getDocumentSelector() == null)
            options.setDocumentSelector(documentSelector);

        register(new Registration(id, METHOD, options));
    }

    @Override
    public synchronized void dispose()
    {
        if (registrations == null)
            return;

        Iterable<Disposable> disposables = getDisposables();
        try
        {
            Disposable.disposeAll(disposables);
        }
        finally
        {
            registrations = null;
            pendingRegistrations = null;
            trackedDocuments.clear();
        }
    }

    private Iterable<Disposable> getDisposables()
    {
        List<Disposable> result = new ArrayList<>();
        if (registrations != null)
            result.addAll(registrations.values());
        if (clientDisposable != null)
            result.add(clientDisposable);
        if (diagnosticPullFuture != null)
            result.add(() -> diagnosticPullFuture.thenAccept(Disposable::dispose));
        return result;
    }

    @Override
    public synchronized void register(Registration registration)
    {
        if (!getMethods().contains(registration.getMethod()))
            throw new IllegalArgumentException();

        Object rO = registration.getRegisterOptions();
        DiagnosticRegistrationOptions options = rO instanceof JsonElement
            ? DefaultGson.INSTANCE.fromJson((JsonElement)rO, DiagnosticRegistrationOptions.class)
            : DiagnosticRegistrationOptions.class.cast(rO);
        if (options == null || options.getDocumentSelector() == null)
            return;

        if (registrations == null)
            return;

        if (registrations.containsKey(registration.getId()))
            throw new IllegalArgumentException();

        RegistrationData registrationData = register(registration.getMethod(), options);
        registrations.put(registration.getId(), registrationData);

        if (diagnosticPullFuture == null)
        {
            diagnosticPullFuture =
                uiDocumentServiceFuture.thenApplyAsync(this::startDiagnosticPull);
            diagnosticPullFuture.whenComplete((x, e) ->
            {
                if (e != null)
                {
                    synchronized (DiagnosticFeature.this)
                    {
                        pendingRegistrations = null;
                    }

                    if (languageClient != null)
                        languageClient.log().error(Messages.getString(
                            "DiagnosticFeature.Error.FailedToStartDiagnosticPull"), e); //$NON-NLS-1$
                    else // should never happen
                        e.printStackTrace();

                    if (!supportsWorkspaceDiagnostics())
                        dispose();
                }
            });
        }
        else if (!diagnosticPullFuture.isCompletedExceptionally())
        {
            if (pendingRegistrations == null)
                pendingRegistrations = new HashSet<>();

            pendingRegistrations.add(registration.getId());

            diagnosticPullFuture.thenRunAsync(this::handlePendingRegistrations);
        }

        if (supportsWorkspaceDiagnostics() && options.isWorkspaceDiagnostics())
        {
            WorkspaceDiagnosticRequestor workspaceDiagnosticRequestor =
                registrationData.getWorkspaceDiagnosticRequestor();
            if (workspaceDiagnosticRequestor != null)
                workspaceDiagnosticRequestor.triggerWorkspacePull();
        }
    }

    private synchronized void handlePendingRegistrations()
    {
        if (registrations == null)
            return;

        Set<String> registrationIds = pendingRegistrations;
        if (registrationIds == null)
            return;

        pendingRegistrations = null;

        if (!trackedDocuments.isEmpty())
        {
            List<List<DocumentFilter>> documentSelectors = new ArrayList<>();
            for (String registrationId : registrationIds)
            {
                RegistrationData registrationData = registrations.get(registrationId);
                if (registrationData != null)
                {
                    documentSelectors.add(
                        registrationData.getRegistrationOptions().getDocumentSelector());
                }
            }

            Set<TextDocument> documents = new HashSet<>();
            for (TextDocument document : trackedDocuments)
            {
                for (List<DocumentFilter> documentSelector : documentSelectors)
                {
                    if (documentMatcher.isMatch(documentSelector, document.getUri(),
                        document.getLanguageId()))
                    {
                        documents.add(document);
                        break;
                    }
                }
            }

            if (!documents.isEmpty())
                onRefreshDiagnostics.emit(documents, logger);
        }
    }

    @Override
    public synchronized void unregister(Unregistration unregistration)
    {
        if (registrations == null)
            return;

        RegistrationData registrationData = registrations.remove(unregistration.getId());
        if (registrationData != null)
            registrationData.dispose();
    }

    private RegistrationData register(String method, DiagnosticRegistrationOptions options)
    {
        return new RegistrationData(new DiagnosticProvider()
        {
            @Override
            public DiagnosticRegistrationOptions getRegistrationOptions()
            {
                return options;
            }

            @Override
            public CompletableFuture<DocumentDiagnosticReport> getDocumentDiagnostics(
                DocumentDiagnosticParams params)
            {
                return languageServer.getTextDocumentService().diagnostic(params);
            }

            @Override
            public CompletableFuture<WorkspaceDiagnosticReport> getWorkspaceDiagnostics(
                WorkspaceDiagnosticParams params)
            {
                if (!Boolean.TRUE.equals(options.isWorkspaceDiagnostics()))
                    throw new UnsupportedOperationException();

                return languageServer.getWorkspaceService().diagnostic(params);
            }

            @Override
            public ProgressService getProgressService()
            {
                return languageClient != null ? languageClient.getProgressService() : null;
            }
        });
    }

    /**
     * Triggers workspace diagnostic pull.
     */
    public synchronized void triggerWorkspacePull()
    {
        if (!supportsWorkspaceDiagnostics() || registrations == null)
            return;

        for (RegistrationData registrationData : registrations.values())
        {
            if (registrationData.getRegistrationOptions().isWorkspaceDiagnostics())
            {
                WorkspaceDiagnosticRequestor workspaceDiagnosticRequestor =
                    registrationData.getWorkspaceDiagnosticRequestor();
                if (workspaceDiagnosticRequestor != null)
                    workspaceDiagnosticRequestor.triggerWorkspacePull();
            }
        }
    }

    synchronized void triggerDocumentPull(TextDocument document, TriggeringContext context)
    {
        List<DiagnosticRequestor> diagnosticRequestors = getDiagnosticRequestors(document, true);
        if (!diagnosticRequestors.isEmpty())
        {
            trackedDocuments.add(document);

            for (DiagnosticRequestor diagnosticRequestor : diagnosticRequestors)
                diagnosticRequestor.triggerDocumentPull(document, context);
        }
    }

    synchronized void cancelDocumentPull(TextDocument document)
    {
        if (trackedDocuments.contains(document))
        {
            List<DiagnosticRequestor> diagnosticRequestors =
                getDiagnosticRequestors(document, false);
            for (DiagnosticRequestor diagnosticRequestor : diagnosticRequestors)
                diagnosticRequestor.cancelDocumentPull(document);
        }
    }

    synchronized void endDocumentPullSequence(TextDocument document)
    {
        if (trackedDocuments.remove(document))
        {
            List<DiagnosticRequestor> diagnosticRequestors =
                getDiagnosticRequestors(document, false);
            for (DiagnosticRequestor diagnosticRequestor : diagnosticRequestors)
                diagnosticRequestor.endDocumentPullSequence(document);
        }
    }

    private List<DiagnosticRequestor> getDiagnosticRequestors(TextDocument document,
        boolean createIfNecessary)
    {
        if (registrations == null)
            return Collections.emptyList();

        List<DiagnosticRequestor> result = new ArrayList<>();
        for (RegistrationData registrationData : registrations.values())
        {
            if (documentMatcher.isMatch(
                registrationData.getRegistrationOptions().getDocumentSelector(), document.getUri(),
                document.getLanguageId()))
            {
                DiagnosticRequestor diagnosticRequestor =
                    registrationData.getDiagnosticRequestor(createIfNecessary);
                if (diagnosticRequestor != null)
                    result.add(diagnosticRequestor);
            }
        }
        return result;
    }

    private Disposable startDiagnosticPull(UiDocumentService uiDocumentService)
    {
        return SafeRun.runWithResult(rollback ->
        {
            Disposable disposable = uiDocumentService.onDidCloseTextDocument().subscribe(document ->
            {
                synchronized (DiagnosticFeature.this)
                {
                    endDocumentPullSequence(document);

                    // the closed document may had been dirty; update to the saved content
                    for (TextDocument visibleDocument : uiDocumentService.getVisibleTextDocuments())
                        triggerDocumentPull(visibleDocument, TriggeringContexts.INTERFILE_CHANGE);
                }
            });
            rollback.add(disposable::dispose);

            disposable = uiDocumentService.onDidBecomeHiddenTextDocument().subscribe(
                document -> cancelDocumentPull(document));
            rollback.add(disposable::dispose);

            disposable = uiDocumentService.onDidBecomeVisibleTextDocument().subscribe(
                document -> triggerDocumentPull(document, TriggeringContexts.DID_BECOME_VISIBLE));
            rollback.add(disposable::dispose);

            disposable = uiDocumentService.onDidBecomeActiveTextDocument().subscribe(
                document -> triggerDocumentPull(document, TriggeringContexts.DID_BECOME_ACTIVE));
            rollback.add(disposable::dispose);

            disposable = onDidFlushPendingChange.subscribe(event ->
            {
                synchronized (DiagnosticFeature.this)
                {
                    // the changed document may have been closed in the meantime
                    if (uiDocumentService.getOpenTextDocuments().contains(event.getDocument()))
                    {
                        triggerDocumentPull(event.getDocument(), TriggeringContexts.CONTENT_CHANGE);

                        for (TextDocument document : uiDocumentService.getVisibleTextDocuments())
                            if (!document.equals(event.getDocument()))
                                triggerDocumentPull(document, TriggeringContexts.INTERFILE_CHANGE);
                    }
                }
            });
            rollback.add(disposable::dispose);

            disposable = onRefreshDiagnostics.subscribe(documents ->
            {
                synchronized (DiagnosticFeature.this)
                {
                    Predicate<TextDocument> predicate =
                        documents != null && !documents.isEmpty() ? documents::contains : x -> true;

                    TextDocument activeDocument = uiDocumentService.getActiveTextDocument();
                    if (activeDocument != null && predicate.test(activeDocument))
                        triggerDocumentPull(activeDocument, null);

                    for (TextDocument document : uiDocumentService.getVisibleTextDocuments())
                        if (!document.equals(activeDocument) && predicate.test(document))
                            triggerDocumentPull(document, null);
                }
            });
            rollback.add(disposable::dispose);

            synchronized (DiagnosticFeature.this)
            {
                TextDocument activeDocument = uiDocumentService.getActiveTextDocument();
                if (activeDocument != null)
                    triggerDocumentPull(activeDocument, TriggeringContexts.DID_BECOME_ACTIVE);

                for (TextDocument document : uiDocumentService.getVisibleTextDocuments())
                    if (!document.equals(activeDocument))
                        triggerDocumentPull(document, TriggeringContexts.DID_BECOME_VISIBLE);
            }

            rollback.setLogger(logger);
            return rollback::run;
        });
    }

    private boolean supportsWorkspaceDiagnostics()
    {
        return workspaceDiagnosticRequestorFactory != null;
    }

    private class RegistrationData
        implements Disposable
    {
        private final DiagnosticProvider diagnosticProvider;
        private DiagnosticRequestor diagnosticRequestor;
        private WorkspaceDiagnosticRequestor workspaceDiagnosticRequestor;

        RegistrationData(DiagnosticProvider diagnosticProvider)
        {
            this.diagnosticProvider = Objects.requireNonNull(diagnosticProvider);
        }

        DiagnosticRegistrationOptions getRegistrationOptions()
        {
            return diagnosticProvider.getRegistrationOptions();
        }

        synchronized DiagnosticRequestor getDiagnosticRequestor(boolean createIfNecessary)
        {
            if (diagnosticRequestor == null && createIfNecessary)
                diagnosticRequestor = diagnosticRequestorFactory.apply(diagnosticProvider);
            return diagnosticRequestor;
        }

        synchronized WorkspaceDiagnosticRequestor getWorkspaceDiagnosticRequestor()
        {
            if (workspaceDiagnosticRequestor == null)
                workspaceDiagnosticRequestor =
                    workspaceDiagnosticRequestorFactory.apply(diagnosticProvider);
            return workspaceDiagnosticRequestor;
        }

        @Override
        public synchronized void dispose()
        {
            Disposable.disposeAll(diagnosticRequestor, workspaceDiagnosticRequestor);
        }
    }

    private static enum TriggeringContexts
        implements TriggeringContext
    {
        DID_BECOME_ACTIVE(TriggeringReason.DID_BECOME_ACTIVE),
        DID_BECOME_VISIBLE(TriggeringReason.DID_BECOME_VISIBLE),
        CONTENT_CHANGE(TriggeringReason.CONTENT_CHANGE),
        INTERFILE_CHANGE(TriggeringReason.INTERFILE_CHANGE);

        private final TriggeringReason reason;

        private TriggeringContexts(TriggeringReason reason)
        {
            this.reason = reason;
        }

        @Override
        public TriggeringReason getTriggeringReason()
        {
            return reason;
        }
    }
}
