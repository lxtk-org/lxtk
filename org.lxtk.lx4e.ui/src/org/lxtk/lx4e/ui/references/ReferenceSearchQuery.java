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
package org.lxtk.lx4e.ui.references;

import java.net.URI;
import java.text.MessageFormat;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletionException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.handly.snapshot.TextFileSnapshot;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.ReferenceContext;
import org.eclipse.lsp4j.ReferenceParams;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.text.AbstractTextSearchResult;
import org.eclipse.search.ui.text.Match;
import org.lxtk.DocumentUri;
import org.lxtk.LanguageOperationTarget;
import org.lxtk.LanguageService;
import org.lxtk.ReferenceProvider;
import org.lxtk.TextDocument;
import org.lxtk.Workspace;
import org.lxtk.lx4e.DocumentUtil;
import org.lxtk.lx4e.EclipseTextDocument;
import org.lxtk.lx4e.internal.Activator;
import org.lxtk.lx4e.requests.ReferencesRequest;
import org.lxtk.lx4e.util.ResourceUtil;

/**
 * Default implementation of an {@link ISearchQuery} that uses a {@link
 * ReferenceProvider} to find references to the symbol denoted by a
 * given text document position.
 */
@SuppressWarnings("restriction")
public class ReferenceSearchQuery
    extends org.eclipse.search.internal.ui.text.FileSearchQuery
{
    private final LanguageOperationTarget target;
    private final Position position;
    private final String wordAtPosition;
    private final Workspace workspace;
    private final boolean includeDeclaration;
    private final String fileName;

    /**
     * Constructor.
     *
     * @param target the {@link LanguageOperationTarget} for this search query
     *  (not <code>null</code>)
     * @param position the target text document position (not <code>null</code>)
     * @param wordAtPosition not <code>null</code>, not empty
     * @param workspace a {@link Workspace} (not <code>null</code>)
     * @param includeDeclaration whether to include the declaration of the symbol
     *  denoted by the given text document position
     */
    public ReferenceSearchQuery(LanguageOperationTarget target,
        Position position, String wordAtPosition, Workspace workspace,
        boolean includeDeclaration)
    {
        super("", false, false, null); //$NON-NLS-1$
        this.target = Objects.requireNonNull(target);
        this.position = Objects.requireNonNull(position);
        if (wordAtPosition.isEmpty())
            throw new IllegalArgumentException();
        this.wordAtPosition = wordAtPosition;
        this.workspace = Objects.requireNonNull(workspace);
        this.includeDeclaration = includeDeclaration;
        fileName = new Path(target.getDocumentUri().getPath()).lastSegment();
    }

    public boolean canRun()
    {
        return getReferenceProvider() != null;
    }

    @Override
    public IStatus run(IProgressMonitor monitor)
    {
        AbstractTextSearchResult result =
            (AbstractTextSearchResult)getSearchResult();
        result.removeAll();

        ReferenceProvider provider = getReferenceProvider();
        if (provider == null)
            return Status.OK_STATUS;

        ReferenceParams params = new ReferenceParams();
        params.setTextDocument(
            DocumentUri.toTextDocumentIdentifier(target.getDocumentUri()));
        params.setPosition(position);
        params.setContext(new ReferenceContext(includeDeclaration));

        ReferencesRequest request = newReferencesRequest();
        request.setProvider(provider);
        request.setParams(params);
        request.setProgressMonitor(monitor);

        List<? extends Location> locations;
        try
        {
            locations = request.sendAndReceive();
        }
        catch (CompletionException e)
        {
            return Activator.createErrorStatus(request.getErrorMessage(),
                e.getCause());
        }

        if (locations == null)
            return Status.OK_STATUS;

        for (Location location : locations)
        {
            Match match = toMatch(location);
            if (match != null)
                result.addMatch(match);
        }
        return Status.OK_STATUS;
    }

    /**
     * Returns a request for computing references.
     *
     * @return the created request object (not <code>null</code>)
     */
    protected ReferencesRequest newReferencesRequest()
    {
        return new ReferencesRequest();
    }

    private ReferenceProvider getReferenceProvider()
    {
        LanguageService languageService = target.getLanguageService();
        return languageService.getDocumentMatcher().getBestMatch(
            languageService.getReferenceProviders(),
            ReferenceProvider::getDocumentSelector, target.getDocumentUri(),
            target.getLanguageId());
    }

    private Match toMatch(Location location)
    {
        IFile file;
        IDocument document;
        URI locationUri = DocumentUri.convert(location.getUri());
        TextDocument textDocument = workspace.getTextDocument(locationUri);
        if (textDocument == null)
        {
            IFile[] files =
                ResourcesPlugin.getWorkspace().getRoot().findFilesForLocationURI(
                    locationUri);
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
            EclipseTextDocument eclipseTextDocument =
                (EclipseTextDocument)textDocument;
            document = eclipseTextDocument.getUnderlyingDocument();
            file = ResourceUtil.getFile(
                eclipseTextDocument.getCorrespondingElement());
        }
        try
        {
            int lineNumber = location.getRange().getStart().getLine();
            IRegion lineRegion = document.getLineInformation(lineNumber);
            IRegion matchRegion =
                DocumentUtil.toRegion(document, location.getRange());
            return new org.eclipse.search.internal.ui.text.FileMatch(file,
                matchRegion.getOffset(), matchRegion.getLength(),
                new org.eclipse.search.internal.ui.text.LineElement(file,
                    lineNumber + 1, lineRegion.getOffset(), document.get(
                        lineRegion.getOffset(), lineRegion.getLength())));
        }
        catch (BadLocationException e)
        {
            Activator.logError(e);
            return null;
        }
    }

    @Override
    public String getLabel()
    {
        return Messages.ReferenceSearchQuery_Label;
    }

    @Override
    public String getResultLabel(int nMatches)
    {
        String label = MessageFormat.format(
            Messages.ReferenceSearchQuery_Result_label_prefix, wordAtPosition,
            fileName, position.getLine() + 1);

        if (nMatches == 1)
            label += Messages.ReferenceSearchQuery_Result_label_suffix_singular;
        else
            label += MessageFormat.format(
                Messages.ReferenceSearchQuery_Result_label_suffix_plural,
                nMatches);

        return label;
    }

    @Override
    public boolean isFileNameSearch()
    {
        // Return false to display lines where references are found
        return false;
    }

    @Override
    public boolean canRerun()
    {
        // The current implementation doesn't support re-running
        return false;
    }

    private static String readContents(IFile file)
    {
        TextFileSnapshot snapshot =
            new TextFileSnapshot(file, TextFileSnapshot.Layer.FILESYSTEM);
        if (!snapshot.exists())
            return null;
        return snapshot.getContents();
    }
}
