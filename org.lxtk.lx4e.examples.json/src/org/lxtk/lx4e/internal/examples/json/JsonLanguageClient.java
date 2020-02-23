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
package org.lxtk.lx4e.internal.examples.json;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.lsp4j.DocumentFilter;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.services.LanguageServer;
import org.lxtk.client.AbstractLanguageClient;
import org.lxtk.client.AbstractLanguageClientController;
import org.lxtk.client.BufferingDiagnosticRequestor;
import org.lxtk.client.CompletionFeature;
import org.lxtk.client.DocumentSymbolFeature;
import org.lxtk.client.Feature;
import org.lxtk.client.HoverFeature;
import org.lxtk.client.TextDocumentSyncFeature;
import org.lxtk.jsonrpc.AbstractJsonRpcConnectionFactory;
import org.lxtk.jsonrpc.JsonRpcConnectionFactory;
import org.lxtk.lx4e.DefaultDiagnosticRequestor;
import org.lxtk.lx4e.DiagnosticAnnotations;
import org.lxtk.lx4e.DiagnosticMarkers;
import org.lxtk.lx4e.EclipseLog;
import org.lxtk.lx4e.examples.json.JsonCore;
import org.lxtk.lx4e.ui.EclipseLanguageClient;
import org.lxtk.util.Log;
import org.lxtk.util.connect.StdioConnection;
import org.lxtk.util.connect.StreamBasedConnection;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * TODO JavaDoc
 */
public class JsonLanguageClient
    extends AbstractLanguageClientController<LanguageServer>
{
    private static final Log LOG = new EclipseLog(
        Activator.getDefault().getBundle(), "json-language-client"); //$NON-NLS-1$

    private static final List<DocumentFilter> DOCUMENT_SELECTOR =
        Collections.singletonList(new DocumentFilter(JsonCore.LANG_ID, null,
            null));

    private final BufferingDiagnosticRequestor diagnosticRequestor =
        new BufferingDiagnosticRequestor(new DefaultDiagnosticRequestor(
            new DiagnosticMarkers("org.lxtk.lx4e.examples.json.problem"), //$NON-NLS-1$
            new DiagnosticAnnotations(JsonCore.WORKSPACE)));

    @Override
    public void dispose()
    {
        diagnosticRequestor.dispose();
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
        Collection<Feature<? super LanguageServer>> features =
            new ArrayList<>();
        features.add(new TextDocumentSyncFeature(JsonCore.WORKSPACE));
        features.add(new DocumentSymbolFeature(JsonCore.LANG_SERVICE));
        features.add(new CompletionFeature(JsonCore.LANG_SERVICE));
        features.add(new HoverFeature(JsonCore.LANG_SERVICE));
        return new EclipseLanguageClient<LanguageServer>(log(),
            diagnosticRequestor, JsonWorkspaceEditChangeFactory.INSTANCE,
            features)
        {
            @Override
            public void fillInitializeParams(InitializeParams params)
            {
                super.fillInitializeParams(params);

                //params.setProcessId((int)ProcessHandle.current().pid()); // requires Java 9
                params.setInitializationOptions(new Object()); // workaround for a bug in JSON LS
            }

            @Override
            public void initialize(LanguageServer server,
                ServerCapabilities capabilities,
                List<DocumentFilter> documentSelector)
            {
                super.initialize(server, capabilities, documentSelector);

                JsonObject schema = new JsonObject();
                JsonArray fileMatch = new JsonArray();
                fileMatch.add("package.json"); //$NON-NLS-1$
                schema.add("fileMatch", fileMatch); //$NON-NLS-1$
                schema.addProperty("url", //$NON-NLS-1$
                    "http://json.schemastore.org/package"); //$NON-NLS-1$
                JsonArray schemas = new JsonArray();
                schemas.add(schema);
                JsonObject jsonSettings = new JsonObject();
                jsonSettings.add("schemas", schemas); //$NON-NLS-1$
                JsonObject settings = new JsonObject();
                settings.add("json", jsonSettings); //$NON-NLS-1$

                server.getWorkspaceService().didChangeConfiguration(
                    new DidChangeConfigurationParams(settings));
            }

            @Override
            protected String getMessageTitle(MessageParams params)
            {
                return "JSON Language Server";
            }
        };
    }

    @Override
    protected JsonRpcConnectionFactory<LanguageServer> getConnectionFactory()
    {
        return new AbstractJsonRpcConnectionFactory<LanguageServer>()
        {
            @Override
            protected StreamBasedConnection newStreamBasedConnection()
            {
                Process process;
                try
                {
                    process = new ProcessBuilder("vscode-json-languageserver", //$NON-NLS-1$
                        "--stdio").start(); //$NON-NLS-1$
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
