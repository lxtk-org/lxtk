/*******************************************************************************
 * Copyright (c) 2019, 2021 1C-Soft LLC.
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

import org.eclipse.lsp4j.CallHierarchyCapabilities;
import org.eclipse.lsp4j.CodeActionCapabilities;
import org.eclipse.lsp4j.CodeLensCapabilities;
import org.eclipse.lsp4j.CompletionCapabilities;
import org.eclipse.lsp4j.DeclarationCapabilities;
import org.eclipse.lsp4j.DefinitionCapabilities;
import org.eclipse.lsp4j.DocumentHighlightCapabilities;
import org.eclipse.lsp4j.DocumentLinkCapabilities;
import org.eclipse.lsp4j.DocumentSymbolCapabilities;
import org.eclipse.lsp4j.FoldingRangeCapabilities;
import org.eclipse.lsp4j.FormattingCapabilities;
import org.eclipse.lsp4j.HoverCapabilities;
import org.eclipse.lsp4j.ImplementationCapabilities;
import org.eclipse.lsp4j.LinkedEditingRangeCapabilities;
import org.eclipse.lsp4j.RangeFormattingCapabilities;
import org.eclipse.lsp4j.ReferencesCapabilities;
import org.eclipse.lsp4j.RenameCapabilities;
import org.eclipse.lsp4j.SemanticTokensCapabilities;
import org.eclipse.lsp4j.SignatureHelpCapabilities;
import org.eclipse.lsp4j.SymbolCapabilities;
import org.eclipse.lsp4j.TypeDefinitionCapabilities;
import org.lxtk.util.Registry;

/**
 * Provides support for participating in language-specific editing features,
 * like code completion, code actions, rename, etc.
 *
 * @see DefaultLanguageService
 */
public interface LanguageService
{
    /**
     * Returns the default document matcher for this service.
     *
     * @return the default document matcher (never <code>null</code>)
     */
    default DocumentMatcher getDocumentMatcher()
    {
        return DefaultDocumentMatcher.INSTANCE;
    }

    /**
     * Returns call hierarchy capabilities provided by this service.
     *
     * @return call hierarchy capabilities (never <code>null</code>).
     *  Clients may modify the returned object; the objects returned by
     *  previous or subsequent invocations of this method will not be affected
     */
    CallHierarchyCapabilities getCallHierarchyCapabilities();

    /**
     * Returns the registry of call hierarchy providers for this service.
     *
     * @return the registry of call hierarchy providers (never <code>null</code>)
     */
    Registry<CallHierarchyProvider> getCallHierarchyProviders();

    /**
     * Returns code action capabilities provided by this service.
     *
     * @return code action capabilities (never <code>null</code>).
     *  Clients may modify the returned object; the objects returned by
     *  previous or subsequent invocations of this method will not be affected
     */
    CodeActionCapabilities getCodeActionCapabilities();

    /**
     * Returns the registry of code action providers for this service.
     *
     * @return the registry of code action providers (never <code>null</code>)
     */
    Registry<CodeActionProvider> getCodeActionProviders();

    /**
     * Returns code lens capabilities provided by this service.
     *
     * @return code lens capabilities (never <code>null</code>).
     *  Clients may modify the returned object; the objects returned by
     *  previous or subsequent invocations of this method will not be affected
     */
    CodeLensCapabilities getCodeLensCapabilities();

    /**
     * Returns the registry of code lens providers for this service.
     *
     * @return the registry of code lens providers (never <code>null</code>)
     */
    Registry<CodeLensProvider> getCodeLensProviders();

    /**
     * Returns completion capabilities provided by this service.
     *
     * @return completion capabilities (never <code>null</code>).
     *  Clients may modify the returned object; the objects returned by
     *  previous or subsequent invocations of this method will not be affected
     */
    CompletionCapabilities getCompletionCapabilities();

    /**
     * Returns the registry of completion providers for this service.
     *
     * @return the registry of completion providers (never <code>null</code>)
     */
    Registry<CompletionProvider> getCompletionProviders();

    /**
     * Returns declaration capabilities provided by this service.
     *
     * @return declaration capabilities (never <code>null</code>).
     *  Clients may modify the returned object; the objects returned by
     *  previous or subsequent invocations of this method will not be affected
     */
    DeclarationCapabilities getDeclarationCapabilities();

    /**
     * Returns the registry of declaration providers for this service.
     *
     * @return the registry of declaration providers (never <code>null</code>)
     */
    Registry<DeclarationProvider> getDeclarationProviders();

    /**
     * Returns definition capabilities provided by this service.
     *
     * @return definition capabilities (never <code>null</code>).
     *  Clients may modify the returned object; the objects returned by
     *  previous or subsequent invocations of this method will not be affected
     */
    DefinitionCapabilities getDefinitionCapabilities();

    /**
     * Returns the registry of definition providers for this service.
     *
     * @return the registry of definition providers (never <code>null</code>)
     */
    Registry<DefinitionProvider> getDefinitionProviders();

    /**
     * Returns document formatting capabilities provided by this service.
     *
     * @return document formatting capabilities (never <code>null</code>).
     *  Clients may modify the returned object; the objects returned by
     *  previous or subsequent invocations of this method will not be affected
     */
    FormattingCapabilities getDocumentFormattingCapabilities();

    /**
     * Returns the registry of document formatting providers for this service.
     *
     * @return the registry of document formatting providers (never <code>null</code>)
     */
    Registry<DocumentFormattingProvider> getDocumentFormattingProviders();

    /**
     * Returns document highlight capabilities provided by this service.
     *
     * @return document highlight capabilities (never <code>null</code>).
     *  Clients may modify the returned object; the objects returned by
     *  previous or subsequent invocations of this method will not be affected
     */
    DocumentHighlightCapabilities getDocumentHighlightCapabilities();

    /**
     * Returns the registry of document highlight providers for this service.
     *
     * @return the registry of document highlight providers (never <code>null</code>)
     */
    Registry<DocumentHighlightProvider> getDocumentHighlightProviders();

    /**
     * Returns document link capabilities provided by this service.
     *
     * @return document link capabilities (never <code>null</code>).
     *  Clients may modify the returned object; the objects returned by
     *  previous or subsequent invocations of this method will not be affected
     */
    DocumentLinkCapabilities getDocumentLinkCapabilities();

    /**
     * Returns the registry of document link providers for this service.
     *
     * @return the registry of document link providers (never <code>null</code>)
     */
    Registry<DocumentLinkProvider> getDocumentLinkProviders();

    /**
     * Returns document range formatting capabilities provided by this service.
     *
     * @return document range formatting capabilities (never <code>null</code>).
     *  Clients may modify the returned object; the objects returned by
     *  previous or subsequent invocations of this method will not be affected
     */
    RangeFormattingCapabilities getDocumentRangeFormattingCapabilities();

    /**
     * Returns the registry of document range formatting providers
     * for this service.
     *
     * @return the registry of document range formatting providers
     *  (never <code>null</code>)
     */
    Registry<DocumentRangeFormattingProvider> getDocumentRangeFormattingProviders();

    /**
     * Returns document semantic tokens capabilities provided by this service.
     *
     * @return document semantic tokens capabilities (never <code>null</code>).
     *  Clients may modify the returned object; the objects returned by
     *  previous or subsequent invocations of this method will not be affected
     */
    SemanticTokensCapabilities getDocumentSemanticTokensCapabilities();

    /**
     * Returns the registry of document semantic tokens' providers for this service.
     *
     * @return the registry of document semantic tokens' providers (never <code>null</code>)
     */
    Registry<DocumentSemanticTokensProvider> getDocumentSemanticTokensProviders();

    /**
     * Returns document symbol capabilities provided by this service.
     *
     * @return document symbol capabilities (never <code>null</code>).
     *  Clients may modify the returned object; the objects returned by
     *  previous or subsequent invocations of this method will not be affected
     */
    DocumentSymbolCapabilities getDocumentSymbolCapabilities();

    /**
     * Returns the registry of document symbol providers for this service.
     *
     * @return the registry of document symbol providers (never <code>null</code>)
     */
    Registry<DocumentSymbolProvider> getDocumentSymbolProviders();

    /**
     * Returns folding range capabilities provided by this service.
     *
     * @return folding range capabilities (never <code>null</code>).
     *  Clients may modify the returned object; the objects returned by
     *  previous or subsequent invocations of this method will not be affected
     */
    FoldingRangeCapabilities getFoldingRangeCapabilities();

    /**
     * Returns the registry of folding range providers for this service.
     *
     * @return the registry of folding range providers (never <code>null</code>)
     */
    Registry<FoldingRangeProvider> getFoldingRangeProviders();

    /**
     * Returns hover capabilities provided by this service.
     *
     * @return hover capabilities (never <code>null</code>).
     *  Clients may modify the returned object; the objects returned by
     *  previous or subsequent invocations of this method will not be affected
     */
    HoverCapabilities getHoverCapabilities();

    /**
     * Returns the registry of hover providers for this service.
     *
     * @return the registry of hover providers (never <code>null</code>)
     */
    Registry<HoverProvider> getHoverProviders();

    /**
     * Returns implementation capabilities provided by this service.
     *
     * @return implementation capabilities (never <code>null</code>).
     *  Clients may modify the returned object; the objects returned by
     *  previous or subsequent invocations of this method will not be affected
     */
    ImplementationCapabilities getImplementationCapabilities();

    /**
     * Returns the registry of implementation providers for this service.
     *
     * @return the registry of implementation providers (never <code>null</code>)
     */
    Registry<ImplementationProvider> getImplementationProviders();

    /**
     * Returns linked editing range capabilities provided by this service.
     *
     * @return linked editing range capabilities (never <code>null</code>).
     *  Clients may modify the returned object; the objects returned by
     *  previous or subsequent invocations of this method will not be affected
     */
    LinkedEditingRangeCapabilities getLinkedEditingRangeCapabilities();

    /**
     * Returns the registry of linked editing range providers for this service.
     *
     * @return the registry of linked editing range providers (never <code>null</code>)
     */
    Registry<LinkedEditingRangeProvider> getLinkedEditingRangeProviders();

    /**
     * Returns references capabilities provided by this service.
     *
     * @return references capabilities (never <code>null</code>).
     *  Clients may modify the returned object; the objects returned by
     *  previous or subsequent invocations of this method will not be affected
     */
    ReferencesCapabilities getReferencesCapabilities();

    /**
     * Returns the registry of reference providers for this service.
     *
     * @return the registry of reference providers (never <code>null</code>)
     */
    Registry<ReferenceProvider> getReferenceProviders();

    /**
     * Returns rename capabilities provided by this service.
     *
     * @return rename capabilities (never <code>null</code>).
     *  Clients may modify the returned object; the objects returned by
     *  previous or subsequent invocations of this method will not be affected
     */
    RenameCapabilities getRenameCapabilities();

    /**
     * Returns the registry of rename providers for this service.
     *
     * @return the registry of rename providers (never <code>null</code>)
     */
    Registry<RenameProvider> getRenameProviders();

    /**
     * Returns signature help capabilities provided by this service.
     *
     * @return signature help capabilities (never <code>null</code>).
     *  Clients may modify the returned object; the objects returned by
     *  previous or subsequent invocations of this method will not be affected
     */
    SignatureHelpCapabilities getSignatureHelpCapabilities();

    /**
     * Returns the registry of signature help providers for this service.
     *
     * @return the registry of signature help providers (never <code>null</code>)
     */
    Registry<SignatureHelpProvider> getSignatureHelpProviders();

    /**
     * Returns type definition capabilities provided by this service.
     *
     * @return type definition capabilities (never <code>null</code>).
     *  Clients may modify the returned object; the objects returned by
     *  previous or subsequent invocations of this method will not be affected
     */
    TypeDefinitionCapabilities getTypeDefinitionCapabilities();

    /**
     * Returns the registry of type definition providers for this service.
     *
     * @return the registry of type definition providers (never <code>null</code>)
     */
    Registry<TypeDefinitionProvider> getTypeDefinitionProviders();

    /**
     * Returns workspace symbol capabilities provided by this service.
     *
     * @return workspace symbol capabilities (never <code>null</code>).
     *  Clients may modify the returned object; the objects returned by
     *  previous or subsequent invocations of this method will not be affected
     */
    SymbolCapabilities getWorkspaceSymbolCapabilities();

    /**
     * Returns the registry of workspace symbol providers for this service.
     *
     * @return the registry of workspace symbol providers (never <code>null</code>)
     */
    Registry<WorkspaceSymbolProvider> getWorkspaceSymbolProviders();
}
