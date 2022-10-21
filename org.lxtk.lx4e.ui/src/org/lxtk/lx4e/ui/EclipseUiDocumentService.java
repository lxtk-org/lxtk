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
package org.lxtk.lx4e.ui;

import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.lxtk.DocumentService;
import org.lxtk.TextDocument;
import org.lxtk.UiDocumentService;
import org.lxtk.util.Disposable;
import org.lxtk.util.EventEmitter;
import org.lxtk.util.EventStream;
import org.lxtk.util.SafeRun;

/**
 * Default implementation of an Eclipse-based {@link UiDocumentService}.
 * <p>
 * <b>Note:</b> Methods for creation and disposal of instances of this class may only be called
 *  in the UI thread. Otherwise, this implementation is thread-safe.
 * </p>
 */
public final class EclipseUiDocumentService
    implements UiDocumentService, Disposable
{
    private final DocumentService documentService;
    private final Consumer<Throwable> exceptionLogger;
    private final EventEmitter<TextDocument> onDidBecomeActiveDocument = new EventEmitter<>();
    private final EventEmitter<TextDocument> onDidBecomeInactiveDocument = new EventEmitter<>();
    private final EventEmitter<TextDocument> onDidBecomeVisibleDocument = new EventEmitter<>();
    private final EventEmitter<TextDocument> onDidBecomeHiddenDocument = new EventEmitter<>();
    private final EventEmitter<TextDocument> onDidOpenDocument = new EventEmitter<>();
    private final EventEmitter<TextDocument> onDidCloseDocument = new EventEmitter<>();
    private final DocumentSet openDocuments = new DocumentSet();
    private final DocumentSet visibleDocuments = new DocumentSet();
    private volatile TextDocument activeDocument;
    private Runnable disposeRunnable;

    /**
     * Constructor.
     * <p>
     * <b>Note:</b> This constructor may only be called in the UI thread.
     * </p>
     *
     * @param documentService not <code>null</code>
     * @param exceptionLogger not <code>null</code>
     */
    public EclipseUiDocumentService(DocumentService documentService,
        Consumer<Throwable> exceptionLogger)
    {
        this.documentService = Objects.requireNonNull(documentService);
        this.exceptionLogger = Objects.requireNonNull(exceptionLogger);
        this.disposeRunnable = initialize()::dispose;
    }

    /**
     * {@inheritDoc}
     * <p>
     * <b>Note:</b> This method of {@link EclipseUiDocumentService} may only be called
     * in the UI thread.
     * </p>
     */
    @Override
    public void dispose()
    {
        if (disposeRunnable != null)
        {
            disposeRunnable.run();
            disposeRunnable = null;
        }
    }

    @Override
    public TextDocument getActiveTextDocument()
    {
        return activeDocument;
    }

    @Override
    public Collection<TextDocument> getVisibleTextDocuments()
    {
        return visibleDocuments.copyOf();
    }

    @Override
    public Collection<TextDocument> getOpenTextDocuments()
    {
        return openDocuments.copyOf();
    }

    @Override
    public EventStream<TextDocument> onDidBecomeActiveTextDocument()
    {
        return onDidBecomeActiveDocument;
    }

    @Override
    public EventStream<TextDocument> onDidBecomeInactiveTextDocument()
    {
        return onDidBecomeInactiveDocument;
    }

    @Override
    public EventStream<TextDocument> onDidBecomeVisibleTextDocument()
    {
        return onDidBecomeVisibleDocument;
    }

    @Override
    public EventStream<TextDocument> onDidBecomeHiddenTextDocument()
    {
        return onDidBecomeHiddenDocument;
    }

    @Override
    public EventStream<TextDocument> onDidOpenTextDocument()
    {
        return onDidOpenDocument;
    }

    @Override
    public EventStream<TextDocument> onDidCloseTextDocument()
    {
        return onDidCloseDocument;
    }

    static IEditorPart getActiveEditor()
    {
        IWorkbenchWindow activeWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (activeWindow == null)
            return null;
        IWorkbenchPage activePage = activeWindow.getActivePage();
        if (activePage == null)
            return null;
        return activePage.getActiveEditor();
    }

    /*
     * Note: this method may only be called in the UI thread.
     */
    void setActiveDocument(TextDocument document)
    {
        TextDocument activeDocumentOld = activeDocument;
        if (Objects.equals(activeDocumentOld, document))
            return;
        activeDocument = document;
        if (activeDocumentOld != null)
            onDidBecomeInactiveDocument.emit(activeDocumentOld, exceptionLogger);
        if (document != null)
            onDidBecomeActiveDocument.emit(document, exceptionLogger);
    }

    TextDocument getDocument(IEditorReference editorRef)
    {
        IEditorInput editorInput;
        try
        {
            editorInput = editorRef.getEditorInput();
        }
        catch (PartInitException e)
        {
            return null;
        }
        return getDocument(editorInput);
    }

    TextDocument getDocument(IEditorInput editorInput)
    {
        if (!(editorInput instanceof IURIEditorInput))
            return null;

        URI documentUri = ((IURIEditorInput)editorInput).getURI();
        return documentService.getTextDocument(documentUri);
    }

    private Disposable initialize()
    {
        return SafeRun.runWithResult(rollback ->
        {
            Disposable disposable = documentService.onDidRemoveTextDocument().subscribe(document ->
            {
                Executor executor = Display.getCurrent() == null
                    ? PlatformUI.getWorkbench().getDisplay()::asyncExec : Runnable::run;
                executor.execute(() ->
                {
                    if (document.equals(activeDocument))
                        setActiveDocument(null);

                    if (visibleDocuments.evict(document))
                        onDidBecomeHiddenDocument.emit(document, exceptionLogger);

                    if (openDocuments.evict(document))
                        onDidCloseDocument.emit(document, exceptionLogger);
                });
            });
            rollback.add(disposable::dispose);

            IPartListener2 partListener = new IPartListener2()
            {
                @Override
                public void partActivated(IWorkbenchPartReference partRef)
                {
                    if (partRef instanceof IEditorReference
                        && partRef.getPage().getWorkbenchWindow().equals(
                            PlatformUI.getWorkbench().getActiveWorkbenchWindow()))
                    {
                        setActiveDocument(getDocument((IEditorReference)partRef));
                    }
                };

                @Override
                public void partVisible(IWorkbenchPartReference partRef)
                {
                    if (partRef instanceof IEditorReference)
                    {
                        TextDocument document = getDocument((IEditorReference)partRef);
                        if (document != null && visibleDocuments.add(document))
                            onDidBecomeVisibleDocument.emit(document, exceptionLogger);
                    }
                };

                @Override
                public void partHidden(IWorkbenchPartReference partRef)
                {
                    if (partRef instanceof IEditorReference)
                    {
                        TextDocument document = getDocument((IEditorReference)partRef);
                        if (document != null)
                        {
                            if (document.equals(activeDocument)
                                && partRef.getPage().getWorkbenchWindow().equals(
                                    PlatformUI.getWorkbench().getActiveWorkbenchWindow()))
                                setActiveDocument(null);

                            if (visibleDocuments.remove(document))
                                onDidBecomeHiddenDocument.emit(document, exceptionLogger);
                        }
                    }
                };

                @Override
                public void partOpened(IWorkbenchPartReference partRef)
                {
                    if (partRef instanceof IEditorReference)
                    {
                        TextDocument document = getDocument((IEditorReference)partRef);
                        if (document != null && openDocuments.add(document))
                            onDidOpenDocument.emit(document, exceptionLogger);
                    }
                };

                @Override
                public void partClosed(IWorkbenchPartReference partRef)
                {
                    if (partRef instanceof IEditorReference)
                    {
                        TextDocument document = getDocument((IEditorReference)partRef);
                        if (document != null && openDocuments.remove(document))
                            onDidCloseDocument.emit(document, exceptionLogger);
                    }
                };

                @Override
                public void partInputChanged(IWorkbenchPartReference partRef)
                {
                    if (partRef instanceof IEditorReference)
                    {
                        TextDocument document = getDocument((IEditorReference)partRef);
                        if (document != null)
                        {
                            if (openDocuments.add(document))
                                onDidOpenDocument.emit(document, exceptionLogger);

                            IWorkbenchPart part = partRef.getPart(false);
                            if (part != null)
                            {
                                IWorkbenchPage page = partRef.getPage();
                                if (page.isPartVisible(part) && visibleDocuments.add(document))
                                    onDidBecomeVisibleDocument.emit(document, exceptionLogger);

                                if (part.equals(getActiveEditor()))
                                    setActiveDocument(document);
                            }
                        }
                    }
                };
            };

            IWindowListener windowListener = new IWindowListener()
            {
                @Override
                public void windowOpened(IWorkbenchWindow window)
                {
                    window.getPartService().addPartListener(partListener);
                }

                @Override
                public void windowClosed(IWorkbenchWindow window)
                {
                    window.getPartService().removePartListener(partListener);
                }

                @Override
                public void windowActivated(IWorkbenchWindow window)
                {
                    TextDocument document = null;
                    IWorkbenchPage activePage = window.getActivePage();
                    if (activePage != null)
                    {
                        IEditorPart activeEditor = activePage.getActiveEditor();
                        if (activeEditor != null)
                            document = getDocument(activeEditor.getEditorInput());
                    }
                    setActiveDocument(document);
                }

                @Override
                public void windowDeactivated(IWorkbenchWindow window)
                {
                    setActiveDocument(null);
                }
            };

            IWorkbench workbench = PlatformUI.getWorkbench();

            rollback.add(() -> workbench.removeWindowListener(windowListener));
            workbench.addWindowListener(windowListener);

            rollback.add(() ->
            {
                activeDocument = null;
                visibleDocuments.clear();
                openDocuments.clear();

                for (IWorkbenchWindow window : workbench.getWorkbenchWindows())
                    window.getPartService().removePartListener(partListener);
            });
            IEditorPart activeEditor = getActiveEditor();
            for (IWorkbenchWindow window : workbench.getWorkbenchWindows())
            {
                window.getPartService().addPartListener(partListener);

                IWorkbenchPage page = window.getActivePage();
                if (page != null)
                {
                    for (IEditorReference editorRef : page.getEditorReferences())
                    {
                        IEditorPart editor = editorRef.getEditor(false);
                        if (editor != null)
                        {
                            TextDocument document = getDocument(editor.getEditorInput());
                            if (document != null)
                            {
                                openDocuments.add(document);

                                if (page.isPartVisible(editor))
                                    visibleDocuments.add(document);

                                if (editor.equals(activeEditor))
                                    activeDocument = document;
                            }
                        }
                    }
                }
            }

            rollback.setLogger(exceptionLogger);
            return rollback::run;
        });
    }

    private static class DocumentSet
    {
        private final Map<TextDocument, Integer> documents = new HashMap<>();

        synchronized Set<TextDocument> copyOf()
        {
            return new HashSet<>(documents.keySet());
        }

        synchronized boolean add(TextDocument document)
        {
            if (documents.putIfAbsent(document, 1) == null)
                return true;
            documents.computeIfPresent(document, (doc, val) -> val + 1);
            return false;
        }

        synchronized boolean remove(TextDocument document)
        {
            if (documents.remove(document, 1))
                return true;
            documents.computeIfPresent(document, (doc, val) -> val - 1);
            return false;
        }

        synchronized boolean evict(TextDocument document)
        {
            return documents.remove(document) != null;
        }

        synchronized void clear()
        {
            documents.clear();
        }
    }
}
