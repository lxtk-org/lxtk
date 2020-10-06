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
package org.lxtk.lx4e.ui.codeaction;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

/**
 * Default implementation of a code mining provider that computes code minings
 * using a {@link CodeLensProvider}.
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

        LanguageService languageService = target.getLanguageService();
        URI documentUri = target.getDocumentUri();

        CodeLensProvider provider = languageService.getDocumentMatcher().getBestMatch(
            languageService.getCodeLensProviders(), CodeLensProvider::getDocumentSelector,
            documentUri, target.getLanguageId());
        if (provider == null)
            return null;

        return provider.getCodeLenses(
            new CodeLensParams(DocumentUri.toTextDocumentIdentifier(documentUri))).thenApply(
                codeLenses -> getCodeMinings(viewer, codeLenses, provider));
    }

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

    private List<? extends ICodeMining> getCodeMinings(ITextViewer viewer,
        List<? extends CodeLens> codeLenses, CodeLensProvider codeLensProvider)
    {
        if (codeLenses == null || codeLenses.isEmpty())
            return Collections.emptyList();

        List<ICodeMining> result = new ArrayList<>(codeLenses.size());
        for (CodeLens codeLens : codeLenses)
        {
            ICodeMining codeMining = getCodeMining(viewer, codeLens, codeLensProvider);
            if (codeMining != null)
                result.add(codeMining);
        }
        return result;
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
