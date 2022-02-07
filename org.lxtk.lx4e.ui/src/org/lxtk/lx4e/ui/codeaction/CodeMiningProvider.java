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
package org.lxtk.lx4e.ui.codeaction;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.codemining.AbstractCodeMiningProvider;
import org.eclipse.jface.text.codemining.ICodeMining;
import org.eclipse.jface.text.codemining.ICodeMiningProvider;
import org.eclipse.jface.text.codemining.LineHeaderCodeMining;
import org.eclipse.lsp4j.CodeLens;
import org.eclipse.lsp4j.CodeLensParams;
import org.eclipse.lsp4j.Command;
import org.eclipse.swt.events.MouseEvent;
import org.lxtk.CodeLensProvider;
import org.lxtk.DocumentUri;
import org.lxtk.LanguageOperationTarget;
import org.lxtk.LanguageService;
import org.lxtk.ProgressService;
import org.lxtk.WorkDoneProgress;
import org.lxtk.jsonrpc.JsonUtil;
import org.lxtk.lx4e.internal.ui.Activator;
import org.lxtk.lx4e.ui.WorkDoneProgressFactory;

/**
 * Default implementation of a code mining provider that computes code minings
 * using {@link CodeLensProvider}(s).
 */
public class CodeMiningProvider
    extends AbstractCodeMiningProvider
{
    @Override
    public CompletableFuture<List<? extends ICodeMining>> provideCodeMinings(ITextViewer viewer,
        IProgressMonitor monitor)
    {
        LanguageOperationTarget target = getLanguageOperationTarget();
        if (target == null)
            return null;

        return provideCodeLensResults(getCodeLensProviders(target), new CodeLensParams(
            DocumentUri.toTextDocumentIdentifier(target.getDocumentUri()))).thenApply(results ->
            {
                if (results == null)
                    return null;

                return getCodeMinings(results, viewer);
            });
    };

    /**
     * Returns the current {@link LanguageOperationTarget}.
     *
     * @return the current <code>LanguageOperationTarget</code>,
     *  or <code>null</code> if none
     */
    protected LanguageOperationTarget getLanguageOperationTarget()
    {
        return getAdapter(LanguageOperationTarget.class);
    }

    /**
     * Returns code lens providers that match the given target.
     *
     * @param target never <code>null</code>
     * @return the matching code lens providers (not <code>null</code>)
     */
    protected CodeLensProvider[] getCodeLensProviders(LanguageOperationTarget target)
    {
        LanguageService languageService = target.getLanguageService();
        return languageService.getDocumentMatcher().getSortedMatches(
            languageService.getCodeLensProviders(), CodeLensProvider::getDocumentSelector,
            target.getDocumentUri(), target.getLanguageId()).toArray(CodeLensProvider[]::new);
    }

    /**
     * Asks each of the given code lens providers to compute a result for the given
     * {@link CodeLensParams} and returns a future of the code lens results.
     *
     * @param providers never <code>null</code>
     * @param params never <code>null</code>
     * @return a future of the code lens results (not <code>null</code>)
     */
    protected CompletableFuture<CodeLensResults> provideCodeLensResults(
        CodeLensProvider[] providers, CodeLensParams params)
    {
        Map<CodeLensProvider, CompletableFuture<CodeLensResult>> futures = new LinkedHashMap<>();

        for (CodeLensProvider provider : providers)
        {
            // note that request params can get modified as part of request processing
            // (e.g. a progress token can be set); therefore we need to copy the given params
            futures.put(provider,
                provideCodeLensResult(provider, JsonUtil.deepCopy(params)).exceptionally(e ->
                {
                    e = Activator.unwrap(e);
                    if (!Activator.isCancellation(e))
                        Activator.logError(e);
                    return null;
                }));
        }

        return CompletableFuture.allOf(
            futures.values().toArray(CompletableFuture[]::new)).thenApply(x ->
            {
                Map<CodeLensProvider, CodeLensResult> results = new LinkedHashMap<>();
                futures.forEach((provider, future) -> results.put(provider, future.join()));
                return new CodeLensResults(results);
            });
    }

    /**
     * Ask the given code lens provider to compute a result for the given {@link CodeLensParams}
     * and returns a future of the result.
     *
     * @param provider never <code>null</code>
     * @param params never <code>null</code>
     * @return a future of the code lens result (not <code>null</code>)
     */
    protected CompletableFuture<CodeLensResult> provideCodeLensResult(CodeLensProvider provider,
        CodeLensParams params)
    {
        WorkDoneProgress workDoneProgress = null;
        if (Boolean.TRUE.equals(provider.getRegistrationOptions().getWorkDoneProgress()))
        {
            ProgressService progressService = provider.getProgressService();
            if (progressService != null)
            {
                workDoneProgress = WorkDoneProgressFactory.newWorkDoneProgressWithJob(false);
                progressService.attachProgress(workDoneProgress);
                params.setWorkDoneToken(workDoneProgress.getToken());
            }
        }

        CompletableFuture<List<? extends CodeLens>> future = provider.getCodeLenses(params);

        if (workDoneProgress != null)
            workDoneProgress.connectWith(future);

        return future.thenApply(codeLenses -> new CodeLensResult(codeLenses));
    }

    /**
     * Returns code minings for the given code lens results.
     *
     * @param results never <code>null</code>
     * @param viewer never <code>null</code>
     * @return the code minings
     */
    protected List<? extends ICodeMining> getCodeMinings(CodeLensResults results,
        ITextViewer viewer)
    {
        List<ICodeMining> codeMinings = new ArrayList<>();

        results.asMap().forEach((provider, result) ->
        {
            if (result != null)
            {
                List<? extends CodeLens> codeLenses = result.getCodeLenses();
                if (codeLenses != null)
                {
                    for (CodeLens codeLens : codeLenses)
                    {
                        ICodeMining codeMining = getCodeMining(viewer, codeLens, provider);
                        if (codeMining != null)
                            codeMinings.add(codeMining);
                    }
                }
            }
        });

        return codeMinings;
    }

    /**
     * Returns an {@link ICodeMining} that corresponds to the given {@link CodeLens}.
     *
     * @param viewer never <code>null</code>
     * @param codeLens never <code>null</code>
     * @param codeLensProvider never <code>null</code>
     * @return the corresponding code mining, or <code>null</code> if none
     */
    protected ICodeMining getCodeMining(ITextViewer viewer, CodeLens codeLens,
        CodeLensProvider codeLensProvider)
    {
        try
        {
            return new CodeMining(codeLens, codeLensProvider, viewer.getDocument(), this);
        }
        catch (BadLocationException e)
        {
            // silently ignore: the document might have changed in the meantime
            return null;
        }
    }

    /**
     * Represents a group of {@link CodeLensResult}s.
     */
    protected static class CodeLensResults
    {
        private final Map<CodeLensProvider, CodeLensResult> results;

        /**
         * Constructor.
         *
         * @param results not <code>null</code>
         */
        public CodeLensResults(Map<CodeLensProvider, CodeLensResult> results)
        {
            this.results = Objects.requireNonNull(results);
        }

        /**
         * Returns the code lens results as a map.
         *
         * @return the code lens results as a map (never <code>null</code>)
         */
        public Map<CodeLensProvider, CodeLensResult> asMap()
        {
            return results;
        }
    }

    /**
     * Represents the result of a code lens request.
     */
    protected static class CodeLensResult
    {
        private final List<? extends CodeLens> codeLenses;

        /**
         * Constructor.
         *
         * @param codeLenses may be <code>null</code>
         */
        public CodeLensResult(List<? extends CodeLens> codeLenses)
        {
            this.codeLenses = codeLenses;
        }

        /**
         * Returns a list of code lenses.
         *
         * @return a list of code lenses, or <code>null</code> if none
         */
        public List<? extends CodeLens> getCodeLenses()
        {
            return codeLenses;
        }
    }

    private static class CodeMining
        extends LineHeaderCodeMining
    {
        private CodeLens codeLens;
        private final CodeLensProvider codeLensProvider;

        CodeMining(CodeLens codeLens, CodeLensProvider codeLensProvider, IDocument document,
            ICodeMiningProvider provider) throws BadLocationException
        {
            super(codeLens.getRange().getStart().getLine(), document, provider);
            this.codeLens = Objects.requireNonNull(codeLens);
            this.codeLensProvider = Objects.requireNonNull(codeLensProvider);
            setLabel(getTitle(codeLens.getCommand()));
        }

        @Override
        public Consumer<MouseEvent> getAction()
        {
            Command command = codeLens.getCommand();
            if (command == null)
                return null;
            return event -> CodeActions.execute(command, getLabel(),
                codeLensProvider.getCommandService());
        }

        @Override
        public boolean isResolved()
        {
            return codeLens.getCommand() != null || super.isResolved();
        }

        @Override
        protected CompletableFuture<Void> doResolve(ITextViewer viewer, IProgressMonitor monitor)
        {
            return codeLensProvider.resolveCodeLens(codeLens).thenAccept(resolved ->
            {
                codeLens = resolved;
                setLabel(getTitle(resolved.getCommand()));
            });
        }

        private static String getTitle(Command command)
        {
            if (command == null)
                return null;
            return command.getTitle();
        }
    }
}
