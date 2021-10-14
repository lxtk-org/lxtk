/*******************************************************************************
 * Copyright (c) 2020, 2021 1C-Soft LLC.
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
package org.lxtk.lx4e.internal.examples.proto;

import static org.lxtk.lx4e.examples.proto.ProtoCore.DOCUMENT_SERVICE;
import static org.lxtk.lx4e.examples.proto.ProtoCore.LANGUAGE_ID;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.handly.buffer.TextFileBuffer;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.lsp4j.TextDocumentSaveReason;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.ui.editors.text.TextFileDocumentProvider;
import org.lxtk.TextDocument;
import org.lxtk.TextDocumentSaveEvent;
import org.lxtk.TextDocumentSaveEventSource;
import org.lxtk.TextDocumentWillSaveEvent;
import org.lxtk.TextDocumentWillSaveEventSource;
import org.lxtk.TextDocumentWillSaveWaitUntilEventSource;
import org.lxtk.lx4e.DocumentUtil;
import org.lxtk.lx4e.EclipseTextDocument;
import org.lxtk.util.Disposable;
import org.lxtk.util.EventEmitter;
import org.lxtk.util.EventStream;
import org.lxtk.util.SafeRun;
import org.lxtk.util.WaitUntilEvent;
import org.lxtk.util.WaitUntilEventEmitter;

/**
 * Proto document provider.
 */
public class ProtoDocumentProvider
    extends TextFileDocumentProvider
    implements TextDocumentWillSaveEventSource, TextDocumentWillSaveWaitUntilEventSource,
    TextDocumentSaveEventSource
{
    private final EventEmitter<TextDocumentWillSaveEvent> onWillSaveTextDocument =
        new EventEmitter<>();
    private final WaitUntilEventEmitter<TextDocumentWillSaveEvent,
        List<TextEdit>> onWillSaveTextDocumentWaitUntil = new WaitUntilEventEmitter<>();
    private final EventEmitter<TextDocumentSaveEvent> onDidSaveTextDocument = new EventEmitter<>();

    @Override
    public EventStream<TextDocumentWillSaveEvent> onWillSaveTextDocument()
    {
        return onWillSaveTextDocument;
    }

    @Override
    public EventStream<
        WaitUntilEvent<TextDocumentWillSaveEvent, List<TextEdit>>> onWillSaveTextDocumentWaitUntil()
    {
        return onWillSaveTextDocumentWaitUntil;
    }

    @Override
    public EventStream<TextDocumentSaveEvent> onDidSaveTextDocument()
    {
        return onDidSaveTextDocument;
    }

    @Override
    protected FileInfo createEmptyFileInfo()
    {
        return new XFileInfo();
    }

    @Override
    protected FileInfo createFileInfo(Object element) throws CoreException
    {
        XFileInfo info = (XFileInfo)super.createFileInfo(element);
        if (info == null || info.fTextFileBuffer == null)
            return null;

        try (
            TextFileBuffer buffer = info.fTextFileBufferLocationKind == null
                ? TextFileBuffer.forFileStore(info.fTextFileBuffer.getFileStore())
                : TextFileBuffer.forLocation(info.fTextFileBuffer.getLocation(),
                    info.fTextFileBufferLocationKind);)
        {
            SafeRun.run(rollback ->
            {
                EclipseTextDocument document = new EclipseTextDocument(
                    info.fTextFileBuffer.getFileStore().toURI(), LANGUAGE_ID, buffer, element);
                rollback.add(document::dispose);

                Disposable registration = DOCUMENT_SERVICE.addTextDocument(document);
                rollback.add(registration::dispose);

                rollback.setLogger(Activator.LOGGER);
                info.disposeRunnable = rollback;
            });
        }
        return info;
    }

    @Override
    protected void disposeFileInfo(Object element, FileInfo info)
    {
        try
        {
            XFileInfo xInfo = (XFileInfo)info;
            if (xInfo.disposeRunnable != null)
                xInfo.disposeRunnable.run();
        }
        finally
        {
            super.disposeFileInfo(element, info);
        }
    }

    @Override
    protected void commitFileBuffer(IProgressMonitor monitor, FileInfo info, boolean overwrite)
        throws CoreException
    {
        TextDocument document =
            DOCUMENT_SERVICE.getTextDocument(info.fTextFileBuffer.getFileStore().toURI());

        if (document != null)
        {
            TextDocumentWillSaveEvent event =
                new TextDocumentWillSaveEvent(document, TextDocumentSaveReason.Manual);

            onWillSaveTextDocument.emit(event, Activator.LOGGER);

            CompletableFuture<List<List<TextEdit>>> future =
                onWillSaveTextDocumentWaitUntil.emit(event, Activator.LOGGER);
            List<List<TextEdit>> result = null;
            try
            {
                result = future.get(1500, TimeUnit.MILLISECONDS);
            }
            catch (InterruptedException | ExecutionException | TimeoutException e)
            {
                Activator.logError(e);
            }
            if (result != null && !result.isEmpty())
            {
                List<TextEdit> edits = new ArrayList<>();
                result.forEach(edits::addAll);
                try
                {
                    DocumentUtil.applyEdits(info.fTextFileBuffer.getDocument(), edits);
                }
                catch (MalformedTreeException | BadLocationException e)
                {
                    Activator.logError(e);
                }
            }
        }

        super.commitFileBuffer(monitor, info, overwrite);

        if (document != null)
            onDidSaveTextDocument.emit(
                new TextDocumentSaveEvent(document, info.fTextFileBuffer.getDocument().get()),
                Activator.LOGGER);
    }

    private static class XFileInfo
        extends FileInfo
    {
        Runnable disposeRunnable;
    }
}
