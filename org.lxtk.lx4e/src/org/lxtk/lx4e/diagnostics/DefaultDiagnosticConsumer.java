/*******************************************************************************
 * Copyright (c) 2020 1C-Soft LLC.
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
package org.lxtk.lx4e.diagnostics;

import java.net.URI;
import java.util.Collection;
import java.util.Objects;
import java.util.function.BiConsumer;

import org.eclipse.lsp4j.Diagnostic;
import org.lxtk.TextDocument;
import org.lxtk.lx4e.EclipseTextDocument;
import org.lxtk.lx4e.util.ResourceUtil;
import org.lxtk.util.Disposable;

/**
 * A consumer of LSP diagnostics that delegates processing to a given {@link
 * DiagnosticMarkers} and a given {@link DiagnosticAnnotations}. Calls to the
 * <code>accept</code> method are forwarded to the <code>DiagnosticAnnotations</code>
 * only if the given URI has no corresponding resource in the Eclipse workspace.
 * <p>
 * An instance of this class is <b>not</b> safe for concurrent access by
 * multiple threads.
 * </p>
 * @see org.lxtk.client.BufferingDiagnosticConsumer
 */
public final class DefaultDiagnosticConsumer
    implements BiConsumer<URI, Collection<Diagnostic>>, Disposable
{
    private final DiagnosticMarkers diagnosticMarkers;
    private final DiagnosticAnnotations diagnosticAnnotations;

    /**
     * Constructor.
     *
     * @param diagnosticMarkers not <code>null</code>
     * @param diagnosticAnnotations not <code>null</code>
     */
    public DefaultDiagnosticConsumer(DiagnosticMarkers diagnosticMarkers,
        DiagnosticAnnotations diagnosticAnnotations)
    {
        this.diagnosticMarkers = Objects.requireNonNull(diagnosticMarkers);
        this.diagnosticAnnotations = Objects.requireNonNull(
            diagnosticAnnotations);
    }

    @Override
    public void accept(URI uri, Collection<Diagnostic> diagnostics)
    {
        diagnosticMarkers.accept(uri, diagnostics);

        TextDocument textDocument =
            diagnosticAnnotations.getWorkspace().getTextDocument(uri);
        if (textDocument instanceof EclipseTextDocument
            && ResourceUtil.getResource(
                ((EclipseTextDocument)textDocument).getCorrespondingElement()) == null)
        {
            diagnosticAnnotations.accept(uri, diagnostics);
        }
    }

    @Override
    public void dispose()
    {
        Disposable.disposeAll(diagnosticMarkers, diagnosticAnnotations);
    }
}
