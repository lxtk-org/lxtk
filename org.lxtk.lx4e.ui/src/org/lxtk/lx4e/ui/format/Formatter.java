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
package org.lxtk.lx4e.ui.format;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletionException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.lsp4j.DocumentFormattingParams;
import org.eclipse.lsp4j.DocumentRangeFormattingParams;
import org.eclipse.lsp4j.FormattingOptions;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.ui.PlatformUI;
import org.lxtk.DocumentFormattingProvider;
import org.lxtk.DocumentMatcher;
import org.lxtk.DocumentRangeFormattingProvider;
import org.lxtk.DocumentUri;
import org.lxtk.LanguageOperationTarget;
import org.lxtk.LanguageService;
import org.lxtk.lx4e.DocumentFormattingRequest;
import org.lxtk.lx4e.DocumentRangeFormattingRequest;
import org.lxtk.lx4e.DocumentUtil;
import org.lxtk.lx4e.Request;

/**
 * Formats a document selection using a <i>formatting provider</i> to compute
 * the formatting edits.
 *
 * @see DocumentFormattingProvider
 * @see DocumentRangeFormattingProvider
 */
public final class Formatter
    implements IRunnableWithProgress
{
    private final LanguageOperationTarget target;
    private final IDocument document;
    private final ITextSelection selection;
    private FormattingOptions options;

    /**
     * Constructor.
     *
     * @param target the {@link LanguageOperationTarget} for the formatter
     *  (not <code>null</code>)
     * @param document the target {@link IDocument} for the formatter
     *  (not <code>null</code>)
     * @param selection the text selection to format (not <code>null</code>,
     *  may be empty)
     */
    public Formatter(LanguageOperationTarget target, IDocument document, ITextSelection selection)
    {
        this.target = Objects.requireNonNull(target);
        this.document = Objects.requireNonNull(document);
        this.selection = Objects.requireNonNull(selection);
    }

    /**
     * Sets the formatting options.
     *
     * @param options the formatting options (not <code>null</code>)
     */
    public void setOptions(FormattingOptions options)
    {
        this.options = Objects.requireNonNull(options);
    }

    /**
     * Checks whether this formatter is applicable.
     * <p>
     * Implementation note: This check should be fast.
     * </p>
     *
     * @return <code>true</code> if the formatter is applicable,
     *  and <code>false</code> otherwise
     */
    public boolean isApplicable()
    {
        LanguageService languageService = target.getLanguageService();
        URI documentUri = target.getDocumentUri();
        String languageId = target.getLanguageId();
        DocumentMatcher documentMatcher = languageService.getDocumentMatcher();

        DocumentRangeFormattingProvider documentRangeFormattingProvider =
            documentMatcher.getFirstMatch(languageService.getDocumentRangeFormattingProviders(),
                DocumentRangeFormattingProvider::getDocumentSelector, documentUri, languageId);
        if (documentRangeFormattingProvider != null)
            return true;

        if (selection.getLength() > 0)
            return false;

        DocumentFormattingProvider documentFormattingProvider =
            documentMatcher.getFirstMatch(languageService.getDocumentFormattingProviders(),
                DocumentFormattingProvider::getDocumentSelector, documentUri, languageId);
        return documentFormattingProvider != null;
    }

    @Override
    public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
    {
        Request<List<? extends TextEdit>> request = createFormattingRequest();
        if (request == null)
            return;

        request.setProgressMonitor(monitor);

        List<? extends TextEdit> edits;
        try
        {
            edits = request.sendAndReceive();
        }
        catch (OperationCanceledException e)
        {
            InterruptedException ie = new InterruptedException();
            ie.initCause(e);
            throw ie;
        }
        catch (CompletionException e)
        {
            throw new InvocationTargetException(e.getCause());
        }

        if (edits == null || edits.isEmpty())
            return;

        Throwable[] exception = new Throwable[1];
        PlatformUI.getWorkbench().getDisplay().syncExec(() ->
        {
            try
            {
                DocumentUtil.applyEdits(document, edits);
            }
            catch (Throwable e)
            {
                exception[0] = e;
            }
        });
        if (exception[0] != null)
            throw new InvocationTargetException(exception[0]);
    }

    /**
     * Returns a request for computing document formatting edits.
     *
     * @return the created request object (not <code>null</code>)
     */
    protected DocumentFormattingRequest newDocumentFormattingRequest()
    {
        return new DocumentFormattingRequest();
    }

    /**
     * Returns a request for computing document range formatting edits.
     *
     * @return the created request object (not <code>null</code>)
     */
    protected DocumentRangeFormattingRequest newDocumentRangeFormattingRequest()
    {
        return new DocumentRangeFormattingRequest();
    }

    private Request<List<? extends TextEdit>> createFormattingRequest()
    {
        if (options == null)
            throw new IllegalStateException("Formatting options have not been set"); //$NON-NLS-1$

        LanguageService languageService = target.getLanguageService();
        URI documentUri = target.getDocumentUri();
        String languageId = target.getLanguageId();
        DocumentMatcher documentMatcher = languageService.getDocumentMatcher();

        DocumentRangeFormattingProvider documentRangeFormattingProvider =
            documentMatcher.getBestMatch(languageService.getDocumentRangeFormattingProviders(),
                DocumentRangeFormattingProvider::getDocumentSelector, documentUri, languageId);
        if (documentRangeFormattingProvider != null)
        {
            int offset;
            int length = selection.getLength();
            if (length > 0)
                offset = selection.getOffset();
            else
            {
                offset = 0;
                length = document.getLength();
            }
            Range range;
            try
            {
                range = DocumentUtil.toRange(document, offset, length);
            }
            catch (BadLocationException e)
            {
                return null;
            }

            DocumentRangeFormattingParams params = new DocumentRangeFormattingParams();
            params.setTextDocument(DocumentUri.toTextDocumentIdentifier(documentUri));
            params.setRange(range);
            params.setOptions(options);

            DocumentRangeFormattingRequest request = newDocumentRangeFormattingRequest();
            request.setProvider(documentRangeFormattingProvider);
            request.setParams(params);
            return request;
        }
        else if (selection.getLength() <= 0)
        {
            DocumentFormattingProvider documentFormattingProvider =
                documentMatcher.getBestMatch(languageService.getDocumentFormattingProviders(),
                    DocumentFormattingProvider::getDocumentSelector, documentUri, languageId);
            if (documentFormattingProvider != null)
            {
                DocumentFormattingParams params = new DocumentFormattingParams();
                params.setTextDocument(DocumentUri.toTextDocumentIdentifier(documentUri));
                params.setOptions(options);

                DocumentFormattingRequest request = newDocumentFormattingRequest();
                request.setProvider(documentFormattingProvider);
                request.setParams(params);
                return request;
            }
        }
        return null;
    }
}
