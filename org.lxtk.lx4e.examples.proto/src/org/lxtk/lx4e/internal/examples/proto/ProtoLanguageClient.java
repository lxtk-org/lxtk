/*******************************************************************************
 * Copyright (c) 2020, 2024 1C-Soft LLC.
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
import static org.lxtk.lx4e.examples.proto.ProtoCore.LANGUAGE_SERVICE;
import static org.lxtk.lx4e.examples.proto.ProtoCore.WORKSPACE_SERVICE;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.lsp4j.DocumentFilter;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.services.LanguageServer;
import org.lxtk.WorkspaceService;
import org.lxtk.client.AbstractLanguageClient;
import org.lxtk.client.BufferingDiagnosticConsumer;
import org.lxtk.client.CompletionFeature;
import org.lxtk.client.Feature;
import org.lxtk.client.TextDocumentSyncFeature;
import org.lxtk.client.WorkspaceFoldersFeature;
import org.lxtk.jsonrpc.AbstractJsonRpcConnectionFactory;
import org.lxtk.jsonrpc.JsonRpcConnectionFactory;
import org.lxtk.lx4e.EclipseLog;
import org.lxtk.lx4e.diagnostics.DiagnosticMarkers;
import org.lxtk.lx4e.refactoring.WorkspaceEditChangeFactory;
import org.lxtk.lx4e.ui.EclipseLanguageClient;
import org.lxtk.lx4e.ui.EclipseLanguageClientController;
import org.lxtk.lx4e.ui.diagnostics.DefaultDiagnosticConsumer;
import org.lxtk.lx4e.ui.diagnostics.DiagnosticAnnotations;
import org.lxtk.util.Log;
import org.lxtk.util.connect.StdioConnection;
import org.lxtk.util.connect.StreamBasedConnection;

/**
 * Proto language client.
 */
public class ProtoLanguageClient
    extends EclipseLanguageClientController<LanguageServer>
{
    private static final String NODE_HOME = System.getProperty("node.home", ""); //$NON-NLS-1$ //$NON-NLS-2$
    private static final String NPX =
        Platform.getOS().toLowerCase().startsWith("win") ? "npx.cmd" : "npx"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

    private static final Log LOG =
        new EclipseLog(Activator.getDefault().getBundle(), "proto-language-client"); //$NON-NLS-1$

    private static final List<DocumentFilter> DOCUMENT_SELECTOR =
        Collections.singletonList(new DocumentFilter(LANGUAGE_ID, null, null));

    private final BufferingDiagnosticConsumer diagnosticConsumer = new BufferingDiagnosticConsumer(
        new DefaultDiagnosticConsumer(new DiagnosticMarkers("org.lxtk.lx4e.examples.proto.problem"), //$NON-NLS-1$
            new DiagnosticAnnotations(DOCUMENT_SERVICE)));

    @Override
    public void dispose()
    {
        diagnosticConsumer.dispose();
        super.dispose();
    }

    @Override
    protected Log log()
    {
        return LOG;
    }

    @Override
    protected List<DocumentFilter> getDocumentSelector()
    {
        return DOCUMENT_SELECTOR;
    }

    @Override
    protected Class<LanguageServer> getServerInterface()
    {
        return LanguageServer.class;
    }

    @Override
    protected AbstractLanguageClient<LanguageServer> getLanguageClient()
    {
        Collection<Feature<? super LanguageServer>> features = new ArrayList<>();
        features.add(TextDocumentSyncFeature.newInstance(DOCUMENT_SERVICE,
            Activator.getDefault().getDocumentProvider()));
        features.add(new CompletionFeature(LANGUAGE_SERVICE));
        features.add(new WorkspaceFoldersFeature(WORKSPACE_SERVICE));
        return new EclipseLanguageClient<>(log(), diagnosticConsumer,
            new WorkspaceEditChangeFactory(DOCUMENT_SERVICE), features)
        {
            @Override
            public WorkspaceService getWorkspaceService()
            {
                return WORKSPACE_SERVICE;
            }

            @Override
            protected String getMessageTitle(MessageParams params)
            {
                return "Proto Language Server";
            }
        };
    }

    @Override
    protected JsonRpcConnectionFactory<LanguageServer> getConnectionFactory()
    {
        return new AbstractJsonRpcConnectionFactory<>()
        {
            @Override
            protected StreamBasedConnection newStreamBasedConnection()
            {
                String npxPath = new Path(NODE_HOME).append(NPX).toOSString();
                ProcessBuilder processBuilder =
                    new ProcessBuilder(npxPath, "proto-language-server", "--stdio"); //$NON-NLS-1$ //$NON-NLS-2$
                if (!NODE_HOME.isEmpty())
                    processBuilder.environment().merge("PATH", NODE_HOME, //$NON-NLS-1$
                        (oldValue, value) -> value + File.pathSeparator + oldValue);
                Process process;
                try
                {
                    process = processBuilder.start();
                }
                catch (IOException e)
                {
                    throw new UncheckedIOException(e);
                }
                StdioConnection conn = new StdioConnection(process);
                conn.onDispose().thenRun(process::destroy);
                return conn;
            }
        };
    }
}
