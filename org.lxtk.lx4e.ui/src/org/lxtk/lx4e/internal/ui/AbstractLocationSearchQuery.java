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
package org.lxtk.lx4e.internal.ui;

import java.net.URI;
import java.util.Objects;
import java.util.function.Consumer;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.handly.snapshot.TextFileSnapshot;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.lsp4j.Location;
import org.eclipse.search.ui.text.AbstractTextSearchResult;
import org.eclipse.search.ui.text.Match;
import org.lxtk.DocumentService;
import org.lxtk.DocumentUri;
import org.lxtk.TextDocument;
import org.lxtk.lx4e.DocumentUtil;
import org.lxtk.lx4e.EclipseTextDocument;
import org.lxtk.lx4e.util.ResourceUtil;

@SuppressWarnings("restriction")
public abstract class AbstractLocationSearchQuery
    extends org.eclipse.search.internal.ui.text.FileSearchQuery
{
    private final DocumentService documentService;

    /**
     * Constructor.
     *
     * @param documentService a {@link DocumentService} (not <code>null</code>)
     */
    public AbstractLocationSearchQuery(DocumentService documentService)
    {
        super("", false, false, null); //$NON-NLS-1$
        this.documentService = Objects.requireNonNull(documentService);
    }

    @Override
    public abstract String getLabel();

    @Override
    public abstract String getResultLabel(int nMatches);

    @Override
    public abstract boolean canRerun();

    @Override
    public boolean isFileNameSearch()
    {
        // Return false to display lines where references are found
        return false;
    }

    @Override
    public final IStatus run(IProgressMonitor monitor)
    {
        AbstractTextSearchResult result = (AbstractTextSearchResult)getSearchResult();
        result.removeAll();

        IStatus status = execute(location ->
        {
            Match match = toMatch(location);
            if (match != null)
                result.addMatch(match);
        }, monitor);

        if (status.matches(IStatus.ERROR))
            result.removeAll(); // don't use the provided partial results in case of error (per LSP spec)

        return status;
    }

    /**
     * Internal API.
     *
     * @param acceptor never <code>null</code>
     * @param monitor never <code>null</code>
     * @return the status after completion of the search job
     * @noreference
     */
    protected abstract IStatus execute(Consumer<? super Location> acceptor,
        IProgressMonitor monitor);

    private Match toMatch(Location location)
    {
        IFile file;
        IDocument document;
        URI locationUri = DocumentUri.convert(location.getUri());
        TextDocument textDocument = documentService.getTextDocument(locationUri);
        if (textDocument == null)
        {
            IFile[] files =
                ResourcesPlugin.getWorkspace().getRoot().findFilesForLocationURI(locationUri);
            if (files.length == 0)
                return null;
            file = files[0];
            String contents = readContents(file);
            if (contents == null)
                return null;
            document = new Document(contents);
        }
        else
        {
            if (!(textDocument instanceof EclipseTextDocument))
                return null;
            EclipseTextDocument eclipseTextDocument = (EclipseTextDocument)textDocument;
            document = eclipseTextDocument.getUnderlyingDocument();
            file = ResourceUtil.getFile(eclipseTextDocument.getCorrespondingElement());
        }
        try
        {
            int lineNumber = location.getRange().getStart().getLine();
            IRegion lineRegion = document.getLineInformation(lineNumber);
            IRegion matchRegion = DocumentUtil.toRegion(document, location.getRange());
            return new org.eclipse.search.internal.ui.text.FileMatch(file, matchRegion.getOffset(),
                matchRegion.getLength(),
                new org.eclipse.search.internal.ui.text.LineElement(file, lineNumber + 1,
                    lineRegion.getOffset(),
                    document.get(lineRegion.getOffset(), lineRegion.getLength())));
        }
        catch (BadLocationException e)
        {
            // silently ignore: the document might have changed in the meantime
            return null;
        }
    }

    private static String readContents(IFile file)
    {
        TextFileSnapshot snapshot = new TextFileSnapshot(file, TextFileSnapshot.Layer.FILESYSTEM);
        if (!snapshot.exists())
            return null;
        return snapshot.getContents();
    }
}
