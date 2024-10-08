/*******************************************************************************
 * Copyright (c) 2019, 2024 1C-Soft LLC.
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

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.lsp4j.DocumentFilter;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.services.LanguageServer;
import org.lxtk.CommandService;
import org.lxtk.DocumentUri;
import org.lxtk.client.AbstractLanguageClient;
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
import org.lxtk.client.FoldingRangeFeature;
import org.lxtk.client.HoverFeature;
import org.lxtk.client.ImplementationFeature;
import org.lxtk.client.ReferencesFeature;
import org.lxtk.client.RenameFeature;
import org.lxtk.client.SignatureHelpFeature;
import org.lxtk.client.TextDocumentSyncFeature;
import org.lxtk.client.TypeDefinitionFeature;
import org.lxtk.client.WorkspaceSymbolFeature;
import org.lxtk.jsonrpc.AbstractJsonRpcConnectionFactory;
import org.lxtk.jsonrpc.JsonRpcConnectionFactory;
import org.lxtk.lx4e.EclipseCommandService;
import org.lxtk.lx4e.EclipseLog;
import org.lxtk.lx4e.EclipseTextDocumentChangeEventMergeStrategy;
import org.lxtk.lx4e.diagnostics.DiagnosticMarkers;
import org.lxtk.lx4e.ui.EclipseLanguageClient;
import org.lxtk.lx4e.ui.EclipseLanguageClientController;
import org.lxtk.util.Log;
import org.lxtk.util.connect.StdioConnection;
import org.lxtk.util.connect.StreamBasedConnection;

/**
 * Represents a TypeScript language client.
 */
public class TypeScriptLanguageClient
    extends EclipseLanguageClientController<LanguageServer>
{
    private static final String NODE_HOME = System.getProperty("node.home", ""); //$NON-NLS-1$ //$NON-NLS-2$
    private static final String NPX =
        Platform.getOS().toLowerCase().startsWith("win") ? "npx.cmd" : "npx"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

    static final String MARKER_TYPE = "org.lxtk.lx4e.examples.typescript.problem"; //$NON-NLS-1$

    private final IProject project;
    private final Log log;
    private final List<DocumentFilter> documentSelector;
    private final String rootUri;
    private final BufferingDiagnosticConsumer diagnosticConsumer =
        new BufferingDiagnosticConsumer(new DiagnosticMarkers(MARKER_TYPE));
    private final CommandService commandService = new EclipseCommandService();

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
        this.project = Objects.requireNonNull(project);
        log = new EclipseLog(Activator.getDefault().getBundle(),
            "typescript-language-client:" + project.getName());
        IPath location = project.getLocation();
        if (location == null)
            throw new IllegalStateException();
        documentSelector = Collections.singletonList(new DocumentFilter(LANGUAGE_ID, "file", //$NON-NLS-1$
            project.getLocation().append("**").toString())); //$NON-NLS-1$
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
        TextDocumentSyncFeature textDocumentSyncFeature =
            new TextDocumentSyncFeature(DOCUMENT_SERVICE);
        textDocumentSyncFeature.setChangeEventMergeStrategy(
            new EclipseTextDocumentChangeEventMergeStrategy());
        features.add(textDocumentSyncFeature);
        features.add(new ExecuteCommandFeature(commandService));
        features.add(new CodeActionFeature(LANGUAGE_SERVICE, commandService));
        features.add(new CompletionFeature(LANGUAGE_SERVICE, commandService));
        features.add(new DefinitionFeature(LANGUAGE_SERVICE));
        features.add(new DocumentFormattingFeature(LANGUAGE_SERVICE));
        features.add(new DocumentHighlightFeature(LANGUAGE_SERVICE));
        features.add(new DocumentRangeFormattingFeature(LANGUAGE_SERVICE));
        features.add(new DocumentSymbolFeature(LANGUAGE_SERVICE));
        features.add(new FoldingRangeFeature(LANGUAGE_SERVICE));
        features.add(new HoverFeature(LANGUAGE_SERVICE));
        features.add(new ImplementationFeature(LANGUAGE_SERVICE));
        features.add(new ReferencesFeature(LANGUAGE_SERVICE));
        features.add(new RenameFeature(LANGUAGE_SERVICE));
        features.add(new SignatureHelpFeature(LANGUAGE_SERVICE));
        features.add(new TypeDefinitionFeature(LANGUAGE_SERVICE));
        features.add(new WorkspaceSymbolFeature(LANGUAGE_SERVICE, project));
        return new EclipseLanguageClient<>(log(), diagnosticConsumer,
            TypeScriptWorkspaceEditChangeFactory.INSTANCE, features)
        {
            @SuppressWarnings("deprecation")
            @Override
            public void fillInitializeParams(InitializeParams params)
            {
                super.fillInitializeParams(params);

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
        return new AbstractJsonRpcConnectionFactory<>()
        {
            @Override
            protected StreamBasedConnection newStreamBasedConnection()
            {
                String npxPath = new Path(NODE_HOME).append(NPX).toOSString();
                ProcessBuilder processBuilder =
                    new ProcessBuilder(npxPath, "typescript-language-server", "--stdio"); //$NON-NLS-1$ //$NON-NLS-2$
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
