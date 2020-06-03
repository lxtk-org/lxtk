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
package org.lxtk.lx4e.model.impl;

import static org.eclipse.handly.context.Contexts.EMPTY_CONTEXT;
import static org.eclipse.handly.context.Contexts.of;
import static org.eclipse.handly.context.Contexts.with;
import static org.eclipse.handly.model.Elements.FORCE_RECONCILING;

import java.net.URI;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.handly.context.Context;
import org.eclipse.handly.context.IContext;
import org.eclipse.handly.model.impl.IReconcileStrategy;
import org.eclipse.handly.model.impl.IWorkingCopyInfo;
import org.eclipse.handly.model.impl.WorkingCopyCallback;
import org.eclipse.handly.model.impl.support.ISourceFileImplSupport;
import org.eclipse.handly.snapshot.DocumentSnapshot;
import org.eclipse.handly.snapshot.ISnapshot;
import org.eclipse.handly.snapshot.ISnapshotProvider;
import org.eclipse.handly.snapshot.NonExpiringSnapshot;
import org.eclipse.handly.util.Property;
import org.eclipse.jface.text.Document;
import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.DocumentSymbolParams;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.SymbolKind;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.lxtk.DocumentSymbolProvider;
import org.lxtk.DocumentUri;
import org.lxtk.LanguageService;
import org.lxtk.TextDocumentChangeEvent;
import org.lxtk.DocumentService;
import org.lxtk.lx4e.EclipseTextDocument;
import org.lxtk.lx4e.EclipseTextDocumentChangeEvent;
import org.lxtk.lx4e.internal.Activator;
import org.lxtk.lx4e.model.ILanguageSourceFile;
import org.lxtk.lx4e.model.ILanguageSymbol;
import org.lxtk.lx4e.requests.DocumentSymbolRequest;
import org.lxtk.lx4e.requests.Request.Handler;
import org.lxtk.util.Disposable;
import org.lxtk.util.SafeRun;

/**
 * Partial implementation of {@link ILanguageSourceFile}.
 */
public abstract class LanguageSourceFile
    extends LanguageSourceElement
    implements ILanguageSourceFile, ISourceFileImplSupport
{
    private static final Property<URI> WORKING_COPY_URI =
        Property.get(LanguageSourceFile.class.getName() + ".workingCopyUri", URI.class); //$NON-NLS-1$

    private static final Property<DocumentSymbolTimeoutHandler> DOCUMENT_SYMBOL_TIMEOUT_HANDLER =
        Property.get(LanguageSourceFile.class.getName() + ".documentSymbolTimeoutHandler", //$NON-NLS-1$
            DocumentSymbolTimeoutHandler.class);

    private final FileWrapper fileWrapper;
    private final String languageId;

    /**
     * Constructs a handle for a source file with the given parent element,
     * workspace file, and language.
     *
     * @param parent the parent of the source file,
     *  or <code>null</code> if the source file has no parent
     * @param file the underlying workspace file (not <code>null</code>)
     * @param languageId the language identifier (not <code>null</code>)
     */
    public LanguageSourceFile(LanguageElement parent, IFile file, String languageId)
    {
        this(parent, FileWrapper.forFile(file), languageId);
    }

    /**
     * Constructs a handle for a source file with the given parent element,
     * file system location URI, and language. The URI must be suitable to
     * passing to <code>EFS.getStore(URI)</code>. This constructor is intended
     * to be used for source files that have an underlying {@link IFileStore}
     * outside the workspace.
     *
     * @param parent the parent of the source file,
     *  or <code>null</code> if the source file has no parent
     * @param locationUri the file system location URI (not <code>null</code>)
     * @param languageId the language identifier (not <code>null</code>)
     */
    /*
     * Note: We use URI rather than IFileStore as the second argument type
     * to avoid introducing a compile-time dependency on org.eclipse.core.filesystem
     * in subclasses that do not otherwise depend on it. For the same reason,
     * we chose not to override getFileStore_(), although a more efficient
     * implementation could have been provided.
     */
    public LanguageSourceFile(LanguageElement parent, URI locationUri, String languageId)
    {
        this(parent, FileWrapper.forLocationUri(locationUri), languageId);
    }

    /**
     * Constructs a handle for a source file with the given parent element, name,
     * and language. This constructor is intended to be used for source files
     * that have no underlying file object.
     *
     * @param parent the parent of the source file,
     *  or <code>null</code> if the source file has no parent
     * @param name the name of the source file, or <code>null</code>
     *  if the source file has no name
     * @param languageId the language identifier (not <code>null</code>)
     */
    public LanguageSourceFile(LanguageElement parent, String name, String languageId)
    {
        super(parent, name);
        this.fileWrapper = FileWrapper.NULL;
        this.languageId = Objects.requireNonNull(languageId);
    }

    private LanguageSourceFile(LanguageElement parent, FileWrapper fileWrapper, String languageId)
    {
        super(parent, fileWrapper.getName());
        this.fileWrapper = fileWrapper;
        this.languageId = Objects.requireNonNull(languageId);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (!(obj instanceof LanguageSourceFile))
            return false;
        return languageId.equals(((LanguageSourceFile)obj).languageId) && super.equals(obj);
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + languageId.hashCode();
        return result;
    }

    @Override
    public Object getFileObject_()
    {
        return fileWrapper.getFileObject();
    }

    @Override
    public IResource getResource_()
    {
        return getFile_();
    }

    @Override
    public IFile getFile_()
    {
        return fileWrapper.getFile();
    }

    @Override
    public URI getLocationUri_()
    {
        return fileWrapper.getLocationUri();
    }

    @Override
    public URI getDocumentUri()
    {
        IContext context = getWorkingCopyContext_();
        if (context != null)
            return context.get(WORKING_COPY_URI);
        return getLocationUri_();
    }

    @Override
    public final String getLanguageId()
    {
        return languageId;
    }

    @Override
    public final void becomeWorkingCopy(IProgressMonitor monitor) throws CoreException
    {
        becomeWorkingCopy_(EMPTY_CONTEXT, monitor);
    }

    @Override
    public final void releaseWorkingCopy()
    {
        releaseWorkingCopy_();
    }

    @Override
    public ILanguageSymbol getSymbol(String name, SymbolKind kind)
    {
        return new LanguageSymbol(this, name, kind);
    }

    @Override
    public ILanguageSymbol[] getSymbols(IProgressMonitor monitor) throws CoreException
    {
        return (ILanguageSymbol[])getChildren_(EMPTY_CONTEXT, monitor);
    }

    @Override
    public boolean becomeWorkingCopy_(IContext context, IProgressMonitor monitor)
        throws CoreException
    {
        return ISourceFileImplSupport.super.becomeWorkingCopy_(
            with(of(WORKING_COPY_CALLBACK, new LanguageWorkingCopyCallback()), context), monitor);
    }

    @Override
    public IContext newWorkingCopyContext_(IContext context)
    {
        return with(ISourceFileImplSupport.super.newWorkingCopyContext_(context),
            of(WORKING_COPY_URI, getDocumentUri()),
            of(DOCUMENT_SYMBOL_TIMEOUT_HANDLER, newDocumentSymbolTimeoutHandler()));
    }

    @Override
    public void buildSourceStructure_(IContext context, IProgressMonitor monitor)
        throws CoreException
    {
        throw new AssertionError("This method should not be called"); //$NON-NLS-1$
    }

    @Override
    public void buildStructure_(IContext context, IProgressMonitor monitor) throws CoreException
    {
        SymbolBuilder symbolBuilder = (SymbolBuilder)context.get(SOURCE_AST);
        if (symbolBuilder == null)
        {
            if (isWorkingCopy_())
                throw new AssertionError();
            symbolBuilder = new FileSymbolBuilder();
        }

        SymbolBuilder.Result input = symbolBuilder.buildSymbols(monitor);

        StructureBuilder structureBuilder = new StructureBuilder(context.get(NEW_ELEMENTS),
            new Document(input.source), input.snapshot);
        structureBuilder.buildStructure(this, input.symbols);
    }

    @Override
    public void toStringName_(StringBuilder builder, IContext context)
    {
        IFile file = getFile_();
        if (file != null)
            builder.append(file.getFullPath());
        else
        {
            IFileStore fileStore = getFileStore_();
            if (fileStore != null)
                builder.append(fileStore);
            else
                super.toStringName_(builder, context);
        }
    }

    @Override
    public <T> T getAdapter(Class<T> adapter)
    {
        if (adapter == IResource.class || adapter == IFile.class)
            return adapter.cast(getFile_());
        if (adapter == IFileStore.class)
            return adapter.cast(getFileStore_());
        return super.getAdapter(adapter);
    }

    /**
     * Returns the {@link DocumentService} associated with this source file.
     *
     * @return the associated <code>DocumentService</code> (not <code>null</code>)
     */
    protected abstract DocumentService getDocumentService();

    /**
     * Returns the {@link LanguageService} associated with this source file.
     *
     * @return the associated <code>LanguageService</code> (not <code>null</code>)
     */
    protected abstract LanguageService getLanguageService();

    /**
     * Returns the timeout for computing document symbols.
     *
     * @return a positive duration
     */
    protected Duration getDocumentSymbolTimeout()
    {
        return Duration.ofSeconds(2);
    }

    /**
     * Returns a new instance of {@link DocumentSymbolTimeoutHandler}.
     *
     * @return the created handler (not <code>null</code>)
     */
    protected DocumentSymbolTimeoutHandler newDocumentSymbolTimeoutHandler()
    {
        return new DocumentSymbolTimeoutHandler();
    }

    /**
     * Returns a request for computing document symbols.
     *
     * @return the created request object (not <code>null</code>)
     */
    protected DocumentSymbolRequest newDocumentSymbolRequest()
    {
        DocumentSymbolRequest request = new DocumentSymbolRequest();
        request.setHandler(new DocumentSymbolRequestHandler());
        return request;
    }

    private List<DocumentSymbol> getDocumentSymbols(URI documentUri, IProgressMonitor monitor)
        throws CoreException
    {
        if (documentUri == null)
            return Collections.emptyList();
        DocumentSymbolProvider provider =
            getDocumentSymbolProvider(getLanguageService(), documentUri);
        if (provider == null)
            return Collections.emptyList();
        return getDocumentSymbols(provider, documentUri, monitor);
    }

    private DocumentSymbolProvider getDocumentSymbolProvider(LanguageService languageService,
        URI documentUri)
    {
        return languageService.getDocumentMatcher().getBestMatch(
            languageService.getDocumentSymbolProviders(),
            DocumentSymbolProvider::getDocumentSelector, documentUri, getLanguageId());
    }

    private List<DocumentSymbol> getDocumentSymbols(DocumentSymbolProvider provider,
        URI documentUri, IProgressMonitor monitor) throws CoreException
    {
        DocumentSymbolRequest request = newDocumentSymbolRequest();
        request.setProvider(provider);
        request.setParams(
            new DocumentSymbolParams(DocumentUri.toTextDocumentIdentifier(documentUri)));
        request.setTimeout(getDocumentSymbolTimeout());
        request.setProgressMonitor(monitor);

        List<Either<SymbolInformation, DocumentSymbol>> result;
        try
        {
            result = request.sendAndReceive();
        }
        catch (CompletionException e)
        {
            throw Activator.toCoreException(e.getCause(), request.getErrorMessage());
        }

        if (result == null || result.isEmpty() || result.get(0).isLeft())
            return Collections.emptyList();

        List<DocumentSymbol> symbols = new ArrayList<>(result.size());
        for (Either<SymbolInformation, DocumentSymbol> item : result)
        {
            if (item.isRight())
                symbols.add(item.getRight());
        }
        return symbols;
    }

    /**
     * A handler for document symbol requests.
     */
    protected class DocumentSymbolRequestHandler
        extends Handler<List<Either<SymbolInformation, DocumentSymbol>>>
    {
        @Override
        protected List<Either<SymbolInformation, DocumentSymbol>> doReceive()
            throws ExecutionException, InterruptedException, TimeoutException
        {
            boolean timeout = false;
            try
            {
                return super.doReceive();
            }
            catch (TimeoutException e)
            {
                timeout = true;
                throw e;
            }
            finally
            {
                IContext context = getWorkingCopyContext_();
                if (context != null)
                {
                    DocumentSymbolTimeoutHandler timeoutHandler =
                        context.get(DOCUMENT_SYMBOL_TIMEOUT_HANDLER);
                    if (timeout)
                        timeoutHandler.onTimeout((DocumentSymbolRequest)getRequest());
                    else
                        timeoutHandler.reset();
                }
            }
        }
    }

    /**
     * A handler for document symbol timeouts.
     */
    protected class DocumentSymbolTimeoutHandler
    {
        protected final AtomicInteger timeouts = new AtomicInteger();
        protected final Job job = createJob();

        /**
         * Returns the number of timeouts seen by this handler.
         *
         * @return the number of timeouts
         */
        public int getTimeouts()
        {
            return timeouts.get();
        }

        /**
         * Handles a timeout for the given request.
         *
         * @param request never <code>null</code>
         */
        public void onTimeout(DocumentSymbolRequest request)
        {
            timeouts.incrementAndGet();

            request.setMayThrow(false); // suppress the TimeoutExeption

            if (shouldRetry())
            {
                job.cancel();
                job.schedule();
            }
        }

        /**
         * Resets this handler.
         */
        public void reset()
        {
            timeouts.set(0);
            job.cancel();
        }

        protected boolean shouldRetry()
        {
            return getTimeouts() < 5;
        }

        protected Job createJob()
        {
            Job job = new Job("") //$NON-NLS-1$
            {
                @Override
                protected IStatus run(IProgressMonitor monitor)
                {
                    try
                    {
                        reconcile(monitor);
                    }
                    catch (CoreException e)
                    {
                        Activator.logError(e);
                    }
                    return Status.OK_STATUS;
                }
            };
            job.setSystem(true);
            return job;
        }
    }

    private static interface FileWrapper
    {
        String getName();

        Object getFileObject();

        IFile getFile();

        URI getLocationUri();

        FileWrapper NULL = new FileWrapper()
        {
            @Override
            public String getName()
            {
                return null;
            }

            @Override
            public Object getFileObject()
            {
                return null;
            }

            @Override
            public IFile getFile()
            {
                return null;
            }

            @Override
            public URI getLocationUri()
            {
                return null;
            }

            @Override
            public String toString()
            {
                return "NULL"; //$NON-NLS-1$
            }
        };

        static FileWrapper forFile(IFile file)
        {
            return new FileWrapper()
            {
                @Override
                public String getName()
                {
                    return file.getName();
                }

                @Override
                public Object getFileObject()
                {
                    return file;
                }

                @Override
                public IFile getFile()
                {
                    return file;
                }

                @Override
                public URI getLocationUri()
                {
                    return file.getLocationURI();
                }

                @Override
                public String toString()
                {
                    return file.getFullPath().toString();
                }
            };
        }

        static FileWrapper forFileStore(IFileStore fileStore)
        {
            return new FileWrapper()
            {
                @Override
                public String getName()
                {
                    return fileStore.getName();
                }

                @Override
                public Object getFileObject()
                {
                    return fileStore;
                }

                @Override
                public IFile getFile()
                {
                    return null;
                }

                @Override
                public URI getLocationUri()
                {
                    return fileStore.toURI();
                }

                @Override
                public String toString()
                {
                    return fileStore.toString();
                }
            };
        }

        static FileWrapper forLocationUri(URI uri)
        {
            IFileStore fileStore;
            try
            {
                fileStore = EFS.getStore(uri);
            }
            catch (CoreException e)
            {
                throw new IllegalArgumentException(e);
            }
            return forFileStore(fileStore);
        }
    }

    private class LanguageWorkingCopyCallback
        extends WorkingCopyCallback
    {
        private EclipseTextDocument document;
        private Runnable disposeRunnable;
        private final Object reconcilingLock = new Object();
        private volatile DocumentSymbolInput lastReconcileInput;

        @Override
        public void onInit(IWorkingCopyInfo info) throws CoreException
        {
            super.onInit(info);
            URI uri = info.getContext().get(WORKING_COPY_URI);
            if (uri == null)
            {
                throw new CoreException(Activator.createErrorStatus(
                    MessageFormat.format(Messages.LanguageSourceFile_No_working_copy_URI,
                        toDisplayString_(EMPTY_CONTEXT)),
                    null));
            }
            SafeRun.run(rollback ->
            {
                document = new EclipseTextDocument(uri, getLanguageId(), info.getBuffer(),
                    LanguageSourceFile.this);
                rollback.add(document::dispose);

                Disposable registration = getDocumentService().addTextDocument(document);
                rollback.add(registration::dispose);

                rollback.setLogger(e -> Activator.logError(e));
                disposeRunnable = rollback;
            });
        }

        @Override
        public void onDispose()
        {
            try
            {
                if (disposeRunnable != null)
                    disposeRunnable.run();
            }
            finally
            {
                super.onDispose();
            }
        }

        @Override
        public boolean needsReconciling()
        {
            DocumentSymbolInput lastInput = lastReconcileInput;
            if (lastInput == null)
                return true;

            return document.getLastChange() != lastInput.event
                || getDocumentSymbolProvider(getLanguageService(),
                    document.getUri()) != lastInput.symbolProvider
                || getWorkingCopyInfo().getContext().get(
                    DOCUMENT_SYMBOL_TIMEOUT_HANDLER).getTimeouts() > 0;
        }

        @Override
        public void reconcile(IContext context, IProgressMonitor monitor) throws CoreException
        {
            synchronized (reconcilingLock)
            {
                boolean needsReconciling = needsReconciling();
                if (needsReconciling || context.getOrDefault(FORCE_RECONCILING))
                {
                    DocumentSymbolBuilder symbolBuilder = new DocumentSymbolBuilder(document);

                    Context context2 = new Context();
                    context2.bind(IReconcileStrategy.SOURCE_AST).to(symbolBuilder);
                    context2.bind(IReconcileStrategy.RECONCILING_FORCED).to(!needsReconciling);

                    getWorkingCopyInfo().getReconcileStrategy().reconcile(with(context2, context),
                        monitor);

                    DocumentSymbolInput input = symbolBuilder.getInput();
                    if (input != null)
                        lastReconcileInput = input;
                }
            }
        }
    }

    private interface SymbolBuilder
    {
        /**
         * Computes a list of {@link DocumentSymbol}s.
         *
         * @param monitor a progress monitor (may be <code>null</code>)
         * @return the computed {@link Result} (never <code>null</code>)
         * @throws CoreException if this method fails
         * @throws OperationCanceledException if the operation is canceled.
         *  Cancellation can occur even if no progress monitor is provided.
         */
        Result buildSymbols(IProgressMonitor monitor) throws CoreException;

        class Result
        {
            final List<DocumentSymbol> symbols;
            final String source;
            final ISnapshot snapshot;

            Result(List<DocumentSymbol> symbols, String source, ISnapshot snapshot)
            {
                this.symbols = symbols;
                this.source = source;
                this.snapshot = snapshot;
            }
        }
    }

    private class FileSymbolBuilder
        implements SymbolBuilder
    {
        FileSymbolBuilder()
        {
        }

        @Override
        public Result buildSymbols(IProgressMonitor monitor) throws CoreException
        {
            List<DocumentSymbol> symbols;
            String source;
            ISnapshot snapshot;
            try (ISnapshotProvider provider = getFileSnapshotProvider_())
            {
                NonExpiringSnapshot fileSnapshot;
                try
                {
                    fileSnapshot = new NonExpiringSnapshot(provider);
                }
                catch (IllegalStateException e)
                {
                    Throwable cause = e.getCause();
                    if (cause instanceof CoreException)
                        throw (CoreException)cause;
                    throw Activator.toCoreException(e, e.getMessage());
                }
                symbols = getDocumentSymbols(getDocumentUri(), monitor);
                source = fileSnapshot.getContents();
                snapshot = fileSnapshot.getWrappedSnapshot();
                if (!snapshot.isEqualTo(provider.getSnapshot()))
                    throw new OperationCanceledException();
            }
            return new Result(symbols, source, snapshot);
        }
    }

    private class DocumentSymbolBuilder
        implements SymbolBuilder
    {
        private final EclipseTextDocument document;
        private DocumentSymbolInput input;

        DocumentSymbolBuilder(EclipseTextDocument document)
        {
            this.document = document;
        }

        @Override
        public Result buildSymbols(IProgressMonitor monitor) throws CoreException
        {
            EclipseTextDocumentChangeEvent documentChange = document.getLastChange();

            if (documentChange.getModificationStamp() != document.getModificationStamp())
                throw new OperationCanceledException();

            ISnapshot documentSnapshot = new DocumentSnapshot(document.getUnderlyingDocument());

            DocumentSymbolProvider documentSymbolProvider =
                getDocumentSymbolProvider(getLanguageService(), document.getUri());

            List<DocumentSymbol> documentSymbols =
                documentSymbolProvider == null ? Collections.emptyList()
                    : getDocumentSymbols(documentSymbolProvider, document.getUri(), monitor);

            if (documentChange.getModificationStamp() != document.getModificationStamp())
                throw new OperationCanceledException();

            input = new DocumentSymbolInput(documentChange, documentSymbolProvider);

            return new Result(documentSymbols, documentChange.getSnapshot().getText(),
                documentSnapshot);
        }

        DocumentSymbolInput getInput()
        {
            return input;
        }
    }

    private static class DocumentSymbolInput
    {
        final TextDocumentChangeEvent event;
        final DocumentSymbolProvider symbolProvider;

        DocumentSymbolInput(TextDocumentChangeEvent event, DocumentSymbolProvider symbolProvider)
        {
            this.event = event;
            this.symbolProvider = symbolProvider;
        }
    }
}
