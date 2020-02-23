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
package org.lxtk.lx4e;

import java.net.URI;
import java.util.Collection;
import java.util.Objects;
import java.util.function.BiConsumer;

import org.eclipse.lsp4j.Diagnostic;
import org.lxtk.TextDocument;
import org.lxtk.lx4e.util.ResourceUtil;
import org.lxtk.util.Disposable;

/**
 * TODO JavaDoc
 */
public final class DefaultDiagnosticRequestor
    implements BiConsumer<URI, Collection<Diagnostic>>, Disposable
{
    private final DiagnosticMarkers diagnosticMarkers;
    private final DiagnosticAnnotations diagnosticAnnotations;

    /**
     * TODO JavaDoc
     *
     * @param diagnosticMarkers not <code>null</code>
     * @param diagnosticAnnotations not <code>null</code>
     */
    public DefaultDiagnosticRequestor(DiagnosticMarkers diagnosticMarkers,
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
            diagnosticAnnotations.workspace.getTextDocument(uri);
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
