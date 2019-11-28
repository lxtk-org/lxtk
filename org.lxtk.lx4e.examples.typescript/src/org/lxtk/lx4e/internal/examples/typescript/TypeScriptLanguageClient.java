/*******************************************************************************
 * Copyright (c) 2019 1C-Soft LLC.
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
import org.lxtk.DocumentUri;
import org.lxtk.client.AbstractLanguageClient;
import org.lxtk.client.AbstractLanguageClientController;
import org.lxtk.client.BufferingDiagnosticRequestor;
import org.lxtk.client.CompletionFeature;
import org.lxtk.client.DefinitionFeature;
import org.lxtk.client.DocumentSymbolFeature;
import org.lxtk.client.Feature;
import org.lxtk.client.ReferencesFeature;
import org.lxtk.client.TextDocumentSyncFeature;
import org.lxtk.jsonrpc.AbstractJsonRpcConnectionFactory;
import org.lxtk.jsonrpc.JsonRpcConnectionFactory;
import org.lxtk.lx4e.DiagnosticMarkers;
import org.lxtk.lx4e.EclipseLog;
import org.lxtk.lx4e.examples.typescript.TypeScriptCore;
import org.lxtk.lx4e.ui.EclipseLanguageClient;
import org.lxtk.util.Log;
import org.lxtk.util.connect.StdioConnection;
import org.lxtk.util.connect.StreamBasedConnection;

/**
 * TODO JavaDoc
 */
public class TypeScriptLanguageClient
    extends AbstractLanguageClientController<LanguageServer>
{
    private final Log log;
    private final List<DocumentFilter> documentSelector;
    private final String rootUri;
    private final BufferingDiagnosticRequestor diagnosticRequestor =
        new BufferingDiagnosticRequestor(new DiagnosticMarkers(
            "org.lxtk.lx4e.examples.typescript.problem")); //$NON-NLS-1$

    /**
     * TODO JavaDoc
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
        documentSelector = Collections.singletonList(new DocumentFilter(
            TypeScriptCore.LANG_ID, "file", project.getLocation().toString() //$NON-NLS-1$
                + "/**")); //$NON-NLS-1$
        URI locationURI = project.getLocationURI();
        if (locationURI == null)
            throw new IllegalStateException();
        rootUri = DocumentUri.convert(project.getLocationURI());
    }

    @Override
    public void dispose()
    {
        diagnosticRequestor.dispose();
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
        Collection<Feature<? super LanguageServer>> features =
            new ArrayList<>();
        features.add(new TextDocumentSyncFeature(TypeScriptCore.WORKSPACE));
        features.add(new CompletionFeature(TypeScriptCore.LANG_SERVICE));
        features.add(new DefinitionFeature(TypeScriptCore.LANG_SERVICE));
        features.add(new DocumentSymbolFeature(TypeScriptCore.LANG_SERVICE));
        features.add(new ReferencesFeature(TypeScriptCore.LANG_SERVICE));
        return new EclipseLanguageClient<LanguageServer>(log(),
            diagnosticRequestor, features)
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
                    process = new ProcessBuilder("typescript-language-server", //$NON-NLS-1$
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
