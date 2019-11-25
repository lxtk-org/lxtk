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
package org.lxtk;

import java.util.Arrays;

import org.eclipse.lsp4j.CompletionCapabilities;
import org.eclipse.lsp4j.CompletionItemCapabilities;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.CompletionItemKindCapabilities;
import org.eclipse.lsp4j.DefinitionCapabilities;
import org.eclipse.lsp4j.DocumentSymbolCapabilities;
import org.eclipse.lsp4j.MarkupKind;
import org.eclipse.lsp4j.SymbolKind;
import org.eclipse.lsp4j.SymbolKindCapabilities;
import org.lxtk.util.Registry;

/**
 * TODO JavaDoc
 */
public class DefaultLanguageService
    implements LanguageService
{
    private final Registry<CompletionProvider> completionProviders =
        Registry.newInstance();
    private final Registry<DefinitionProvider> definitionProviders =
        Registry.newInstance();
    private final Registry<DocumentSymbolProvider> documentSymbolProviders =
        Registry.newInstance();

    @Override
    public CompletionCapabilities getCompletionCapabilities()
    {
        CompletionItemCapabilities completionItem =
            new CompletionItemCapabilities();
        completionItem.setSnippetSupport(true);
        completionItem.setCommitCharactersSupport(true);
        completionItem.setDocumentationFormat(Arrays.asList(new String[] {
            MarkupKind.MARKDOWN, MarkupKind.PLAINTEXT }));
        completionItem.setDeprecatedSupport(true);
        completionItem.setPreselectSupport(true);

        CompletionItemKindCapabilities completionItemKind =
            new CompletionItemKindCapabilities();
        completionItemKind.setValueSet(Arrays.asList(
            CompletionItemKind.values()));

        CompletionCapabilities completion = new CompletionCapabilities();
        completion.setDynamicRegistration(true);
        completion.setContextSupport(true);
        completion.setCompletionItem(completionItem);
        completion.setCompletionItemKind(completionItemKind);
        return completion;
    }

    @Override
    public Registry<CompletionProvider> getCompletionProviders()
    {
        return completionProviders;
    }

    @Override
    public DefinitionCapabilities getDefinitionCapabilities()
    {
        DefinitionCapabilities definition = new DefinitionCapabilities();
        definition.setDynamicRegistration(true);
        definition.setLinkSupport(true);
        return definition;
    }

    @Override
    public Registry<DefinitionProvider> getDefinitionProviders()
    {
        return definitionProviders;
    }

    @Override
    public DocumentSymbolCapabilities getDocumentSymbolCapabilities()
    {
        SymbolKindCapabilities symbolKind = new SymbolKindCapabilities();
        symbolKind.setValueSet(Arrays.asList(SymbolKind.values()));

        DocumentSymbolCapabilities documentSymbol =
            new DocumentSymbolCapabilities();
        documentSymbol.setDynamicRegistration(true);
        documentSymbol.setSymbolKind(symbolKind);
        documentSymbol.setHierarchicalDocumentSymbolSupport(true);
        return documentSymbol;
    }

    @Override
    public Registry<DocumentSymbolProvider> getDocumentSymbolProviders()
    {
        return documentSymbolProviders;
    }
}
