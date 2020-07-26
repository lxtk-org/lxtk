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
package org.lxtk.lx4e.internal.examples.typescript;

import static org.lxtk.lx4e.examples.typescript.TypeScriptCore.DOCUMENT_SERVICE;
import static org.lxtk.lx4e.examples.typescript.TypeScriptCore.LANGUAGE_ID;
import static org.lxtk.lx4e.examples.typescript.TypeScriptCore.LANGUAGE_SERVICE;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.lsp4j.DocumentFilter;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.services.LanguageServer;
import org.lxtk.CommandService;
import org.lxtk.DefaultCommandService;
import org.lxtk.DocumentUri;
import org.lxtk.client.AbstractLanguageClient;
import org.lxtk.client.AbstractLanguageClientController;
import org.lxtk.client.BufferingDiagnosticConsumer;
import org.lxtk.client.CodeActionFeature;
import org.lxtk.client.CompletionFeature;
import org.lxtk.client.DefinitionFeature;
import org.lxtk.client.DocumentFormattingFeature;
import org.lxtk.client.DocumentHighlightFeature;
import org.lxtk.client.DocumentRangeFormattingFeature;
import org.lxtk.client.DocumentSymbolFeature;
import org.lxtk.client.ExecuteCommandFeature;
import org.lxtk.client.Feature;
import org.lxtk.client.HoverFeature;
import org.lxtk.client.ImplementationFeature;
import org.lxtk.client.ReferencesFeature;
import org.lxtk.client.RenameFeature;
import org.lxtk.client.SignatureHelpFeature;
import org.lxtk.client.TextDocumentSyncFeature;
import org.lxtk.client.TypeDefinitionFeature;
import org.lxtk.jsonrpc.AbstractJsonRpcConnectionFactory;
import org.lxtk.jsonrpc.JsonRpcConnectionFactory;
import org.lxtk.lx4e.EclipseLog;
import org.lxtk.lx4e.diagnostics.DiagnosticMarkers;
import org.lxtk.lx4e.ui.EclipseLanguageClient;
import org.lxtk.util.Log;
import org.lxtk.util.connect.StdioConnection;
import org.lxtk.util.connect.StreamBasedConnection;

/**
 * Represents a TypeScript language client.
 */
public class TypeScriptLanguageClient
    extends AbstractLanguageClientController<LanguageServer>
{
    static final String MARKER_TYPE = "org.lxtk.lx4e.examples.typescript.problem"; //$NON-NLS-1$

    private final Log log;
    private final List<DocumentFilter> documentSelector;
    private final String rootUri;
    private final BufferingDiagnosticConsumer diagnosticConsumer =
        new BufferingDiagnosticConsumer(new DiagnosticMarkers(MARKER_TYPE));
    private final CommandService commandService = new DefaultCommandService();

    /**
     * Creates a new TypeScript language client with the given {@link IProject}
     * as the client's root folder.
     *
     * @param project not <code>null</code>
     * @throws IllegalStateException if the project's {@link IProject#getLocation()
     *  location} could not be resolved
     */
    public TypeScriptLanguageClient(IProject project)
    {
        log = new EclipseLog(Activator.getDefault().getBundle(),
            "typescript-language-client:" + project.getName());
        IPath location = project.getLocation();
        if (location == null)
            throw new IllegalStateException();
        documentSelector = Collections.singletonList(new DocumentFilter(LANGUAGE_ID, "file", //$NON-NLS-1$
            project.getLocation().append("**").toOSString())); //$NON-NLS-1$
        URI locationURI = project.getLocationURI();
        if (locationURI == null)
            throw new IllegalStateException();
        rootUri = DocumentUri.convert(project.getLocationURI());
    }

    @Override
    public void dispose()
    {
        diagnosticConsumer.dispose();
        super.dispose();
    }

    @Override
    protected Log log()
    {
        return log;
    }

    @Override
    protected List<DocumentFilter> getDocumentSelector()
    {
        return documentSelector;
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
        features.add(new TextDocumentSyncFeature(DOCUMENT_SERVICE));
        features.add(new ExecuteCommandFeature(commandService));
        features.add(new CodeActionFeature(LANGUAGE_SERVICE, commandService));
        features.add(new CompletionFeature(LANGUAGE_SERVICE));
        features.add(new DefinitionFeature(LANGUAGE_SERVICE));
        features.add(new DocumentFormattingFeature(LANGUAGE_SERVICE));
        features.add(new DocumentHighlightFeature(LANGUAGE_SERVICE));
        features.add(new DocumentRangeFormattingFeature(LANGUAGE_SERVICE));
        features.add(new DocumentSymbolFeature(LANGUAGE_SERVICE));
        features.add(new HoverFeature(LANGUAGE_SERVICE));
        features.add(new ImplementationFeature(LANGUAGE_SERVICE));
        features.add(new ReferencesFeature(LANGUAGE_SERVICE));
        features.add(new RenameFeature(LANGUAGE_SERVICE));
        features.add(new SignatureHelpFeature(LANGUAGE_SERVICE));
        features.add(new TypeDefinitionFeature(LANGUAGE_SERVICE));
        return new EclipseLanguageClient<LanguageServer>(log(), diagnosticConsumer,
            TypeScriptWorkspaceEditChangeFactory.INSTANCE, features)
        {
            @Override
            public void fillInitializeParams(InitializeParams params)
            {
                super.fillInitializeParams(params);

                //params.setProcessId((int)ProcessHandle.current().pid()); // requires Java 9
                params.setRootUri(rootUri);
            }

            @Override
            protected String getMessageTitle(MessageParams params)
            {
                return "TypeScript Language Server";
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
                    process = new ProcessBuilder("npx", "typescript-language-server", //$NON-NLS-1$ //$NON-NLS-2$
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
