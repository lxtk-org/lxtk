/*******************************************************************************
 * Copyright (c) 2020, 2022 1C-Soft LLC.
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
import org.lxtk.lx4e.DocumentUtil;
import org.lxtk.lx4e.requests.DocumentFormattingRequest;
import org.lxtk.lx4e.requests.DocumentRangeFormattingRequest;
import org.lxtk.lx4e.ui.WorkDoneProgressFactory;

/**
 * Formats a document selection using a <i>formatting provider</i> to compute
 * the formatting edits.
 *
 * @see DocumentFormattingProvider
 * @see DocumentRangeFormattingProvider
 */
public class Formatter
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
     * Returns the {@link LanguageOperationTarget} for this formatter.
     *
     * @return the <code>LanguageOperationTarget</code> for the formatter
     *  (never <code>null</code>)
     */
    public final LanguageOperationTarget getLanguageOperationTarget()
    {
        return target;
    }

    /**
     * Returns the target {@link IDocument} for this formatter.
     *
     * @return the target document for the formatter (never <code>null</code>)
     */
    public final IDocument getDocument()
    {
        return document;
    }

    /**
     * Returns the target document selection for this formatter.
     *
     * @return the target document selection for the formatter (never <code>null</code>,
     *  may be empty)
     */
    public final ITextSelection getSelection()
    {
        return selection;
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
     * Returns the formatting options.
     *
     * @return the formatting options, or <code>null</code> if no options have been set
     * @see #setOptions(FormattingOptions)
     */
    public final FormattingOptions getOptions()
    {
        return options;
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
        List<? extends TextEdit> edits;
        try
        {
            edits = computeFormattingEdits(monitor);
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
     * Computes the formatting edits.
     *
     * @param monitor a progress monitor
     * @return the computed edits (may be <code>null</code> or empty)
     */
    protected List<? extends TextEdit> computeFormattingEdits(IProgressMonitor monitor)
    {
        if (options == null)
            throw new IllegalStateException("Formatting options have not been set"); //$NON-NLS-1$

        if (selection.getLength() <= 0)
        {
            DocumentFormattingProvider documentFormattingProvider = getDocumentFormattingProvider();
            if (documentFormattingProvider != null)
            {
                DocumentFormattingParams params = new DocumentFormattingParams();
                params.setTextDocument(
                    DocumentUri.toTextDocumentIdentifier(target.getDocumentUri()));
                params.setOptions(options);

                DocumentFormattingRequest request = newDocumentFormattingRequest();
                request.setProvider(documentFormattingProvider);
                request.setParams(params);
                request.setUpWorkDoneProgress(WorkDoneProgressFactory::newWorkDoneProgress);
                request.setProgressMonitor(monitor);
                return request.sendAndReceive();
            }
        }

        DocumentRangeFormattingProvider documentRangeFormattingProvider =
            getDocumentRangeFormattingProvider();
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
            params.setTextDocument(DocumentUri.toTextDocumentIdentifier(target.getDocumentUri()));
            params.setRange(range);
            params.setOptions(options);

            DocumentRangeFormattingRequest request = newDocumentRangeFormattingRequest();
            request.setProvider(documentRangeFormattingProvider);
            request.setParams(params);
            request.setUpWorkDoneProgress(WorkDoneProgressFactory::newWorkDoneProgress);
            request.setProgressMonitor(monitor);
            return request.sendAndReceive();
        }

        return null;
    }

    /**
     * Returns the document formatting provider for this formatter.
     *
     * @return the document formatting provider, or <code>null</code> if none
     */
    protected DocumentFormattingProvider getDocumentFormattingProvider()
    {
        LanguageService languageService = target.getLanguageService();
        return languageService.getDocumentMatcher().getBestMatch(
            languageService.getDocumentFormattingProviders(),
            DocumentFormattingProvider::getDocumentSelector, target.getDocumentUri(),
            target.getLanguageId());
    }

    /**
     * Returns the document range formatting provider for this formatter.
     *
     * @return the document range formatting provider, or <code>null</code> if none
     */
    protected DocumentRangeFormattingProvider getDocumentRangeFormattingProvider()
    {
        LanguageService languageService = target.getLanguageService();
        return languageService.getDocumentMatcher().getBestMatch(
            languageService.getDocumentRangeFormattingProviders(),
            DocumentRangeFormattingProvider::getDocumentSelector, target.getDocumentUri(),
            target.getLanguageId());
    }

    /**
     * Returns a new instance of {@link DocumentFormattingRequest}.
     *
     * @return the created request object (not <code>null</code>)
     */
    protected DocumentFormattingRequest newDocumentFormattingRequest()
    {
        return new DocumentFormattingRequest();
    }

    /**
     * Returns a new instance of {@link DocumentRangeFormattingRequest}.
     *
     * @return the created request object (not <code>null</code>)
     */
    protected DocumentRangeFormattingRequest newDocumentRangeFormattingRequest()
    {
        return new DocumentRangeFormattingRequest();
    }
}
