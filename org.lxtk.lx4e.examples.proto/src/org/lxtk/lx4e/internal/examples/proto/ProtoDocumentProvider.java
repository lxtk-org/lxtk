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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.handly.buffer.TextFileBuffer;
import org.eclipse.ui.editors.text.TextFileDocumentProvider;
import org.lxtk.TextDocument;
import org.lxtk.TextDocumentSaveEvent;
import org.lxtk.TextDocumentSaveEventSource;
import org.lxtk.lx4e.EclipseTextDocument;
import org.lxtk.util.Disposable;
import org.lxtk.util.EventEmitter;
import org.lxtk.util.EventStream;
import org.lxtk.util.SafeRun;

/**
 * Proto document provider.
 */
public class ProtoDocumentProvider
    extends TextFileDocumentProvider
    implements TextDocumentSaveEventSource
{
    private final EventEmitter<TextDocumentSaveEvent> onDidSaveTextDocument = new EventEmitter<>();

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

                rollback.setLogger(e -> Activator.logError(e));
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

        super.commitFileBuffer(monitor, info, overwrite);

        if (document != null)
            onDidSaveTextDocument.fire(
                new TextDocumentSaveEvent(document, info.fTextFileBuffer.getDocument().get()),
                e -> Activator.logError(e));
    }

    private static class XFileInfo
        extends FileInfo
    {
        Runnable disposeRunnable;
    }
}
