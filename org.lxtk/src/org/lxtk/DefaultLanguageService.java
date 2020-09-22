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
package org.lxtk;

import java.util.Arrays;

import org.eclipse.lsp4j.CodeActionCapabilities;
import org.eclipse.lsp4j.CodeActionKind;
import org.eclipse.lsp4j.CodeActionKindCapabilities;
import org.eclipse.lsp4j.CodeActionLiteralSupportCapabilities;
import org.eclipse.lsp4j.CodeLensCapabilities;
import org.eclipse.lsp4j.CompletionCapabilities;
import org.eclipse.lsp4j.CompletionItemCapabilities;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.CompletionItemKindCapabilities;
import org.eclipse.lsp4j.DeclarationCapabilities;
import org.eclipse.lsp4j.DefinitionCapabilities;
import org.eclipse.lsp4j.DocumentHighlightCapabilities;
import org.eclipse.lsp4j.DocumentSymbolCapabilities;
import org.eclipse.lsp4j.FoldingRangeCapabilities;
import org.eclipse.lsp4j.FormattingCapabilities;
import org.eclipse.lsp4j.HoverCapabilities;
import org.eclipse.lsp4j.ImplementationCapabilities;
import org.eclipse.lsp4j.MarkupKind;
import org.eclipse.lsp4j.ParameterInformationCapabilities;
import org.eclipse.lsp4j.RangeFormattingCapabilities;
import org.eclipse.lsp4j.ReferencesCapabilities;
import org.eclipse.lsp4j.RenameCapabilities;
import org.eclipse.lsp4j.SignatureHelpCapabilities;
import org.eclipse.lsp4j.SignatureInformationCapabilities;
import org.eclipse.lsp4j.SymbolCapabilities;
import org.eclipse.lsp4j.SymbolKind;
import org.eclipse.lsp4j.SymbolKindCapabilities;
import org.eclipse.lsp4j.TypeDefinitionCapabilities;
import org.lxtk.util.Registry;

/**
 * Default implementation of the {@link LanguageService} interface.
 * <p>
 * This implementation is thread-safe.
 * </p>
*/
public class DefaultLanguageService
    implements LanguageService
{
    private final Registry<CodeActionProvider> codeActionProviders = Registry.newInstance();
    private final Registry<CodeLensProvider> codeLensProviders = Registry.newInstance();
    private final Registry<CompletionProvider> completionProviders = Registry.newInstance();
    private final Registry<DeclarationProvider> declarationProviders = Registry.newInstance();
    private final Registry<DefinitionProvider> definitionProviders = Registry.newInstance();
    private final Registry<DocumentFormattingProvider> documentFormattingProviders =
        Registry.newInstance();
    private final Registry<DocumentHighlightProvider> documentHighlightProviders =
        Registry.newInstance();
    private final Registry<DocumentRangeFormattingProvider> documentRangeFormattingProviders =
        Registry.newInstance();
    private final Registry<DocumentSymbolProvider> documentSymbolProviders = Registry.newInstance();
    private final Registry<FoldingRangeProvider> foldingRangeProviders = Registry.newInstance();
    private final Registry<HoverProvider> hoverProviders = Registry.newInstance();
    private final Registry<ImplementationProvider> implementationProviders = Registry.newInstance();
    private final Registry<ReferenceProvider> referenceProviders = Registry.newInstance();
    private final Registry<RenameProvider> renameProviders = Registry.newInstance();
    private final Registry<SignatureHelpProvider> signatureHelpProviders = Registry.newInstance();
    private final Registry<TypeDefinitionProvider> typeDefinitionProviders = Registry.newInstance();
    private final Registry<WorkspaceSymbolProvider> workspaceSymbolProviders =
        Registry.newInstance();

    @Override
    public CodeActionCapabilities getCodeActionCapabilities()
    {
        CodeActionKindCapabilities codeActionKind = new CodeActionKindCapabilities();
        codeActionKind.setValueSet(Arrays.asList(CodeActionKind.QuickFix, CodeActionKind.Refactor,
            CodeActionKind.RefactorExtract, CodeActionKind.RefactorInline,
            CodeActionKind.RefactorRewrite, CodeActionKind.Source,
            CodeActionKind.SourceOrganizeImports));

        CodeActionLiteralSupportCapabilities codeActionLiteralSupport =
            new CodeActionLiteralSupportCapabilities();
        codeActionLiteralSupport.setCodeActionKind(codeActionKind);

        CodeActionCapabilities codeAction = new CodeActionCapabilities();
        codeAction.setDynamicRegistration(true);
        codeAction.setCodeActionLiteralSupport(codeActionLiteralSupport);
        return codeAction;
    }

    @Override
    public Registry<CodeActionProvider> getCodeActionProviders()
    {
        return codeActionProviders;
    }

    @Override
    public CodeLensCapabilities getCodeLensCapabilities()
    {
        CodeLensCapabilities codeLens = new CodeLensCapabilities();
        codeLens.setDynamicRegistration(true);
        return codeLens;
    }

    @Override
    public Registry<CodeLensProvider> getCodeLensProviders()
    {
        return codeLensProviders;
    }

    @Override
    public CompletionCapabilities getCompletionCapabilities()
    {
        CompletionItemCapabilities completionItem = new CompletionItemCapabilities();
        completionItem.setSnippetSupport(true);
        completionItem.setCommitCharactersSupport(true);
        completionItem.setDocumentationFormat(
            Arrays.asList(MarkupKind.MARKDOWN, MarkupKind.PLAINTEXT));
        completionItem.setDeprecatedSupport(true);
        completionItem.setPreselectSupport(true);

        CompletionItemKindCapabilities completionItemKind = new CompletionItemKindCapabilities();
        completionItemKind.setValueSet(Arrays.asList(CompletionItemKind.values()));

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
    public DeclarationCapabilities getDeclarationCapabilities()
    {
        DeclarationCapabilities declaration = new DeclarationCapabilities();
        declaration.setDynamicRegistration(true);
        declaration.setLinkSupport(true);
        return declaration;
    }

    @Override
    public Registry<DeclarationProvider> getDeclarationProviders()
    {
        return declarationProviders;
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
    public FormattingCapabilities getDocumentFormattingCapabilities()
    {
        FormattingCapabilities formatting = new FormattingCapabilities();
        formatting.setDynamicRegistration(true);
        return formatting;
    }

    @Override
    public Registry<DocumentFormattingProvider> getDocumentFormattingProviders()
    {
        return documentFormattingProviders;
    }

    @Override
    public DocumentHighlightCapabilities getDocumentHighlightCapabilities()
    {
        DocumentHighlightCapabilities documentHighlight = new DocumentHighlightCapabilities();
        documentHighlight.setDynamicRegistration(true);
        return documentHighlight;
    }

    @Override
    public Registry<DocumentHighlightProvider> getDocumentHighlightProviders()
    {
        return documentHighlightProviders;
    }

    @Override
    public RangeFormattingCapabilities getDocumentRangeFormattingCapabilities()
    {
        RangeFormattingCapabilities rangeFormatting = new RangeFormattingCapabilities();
        rangeFormatting.setDynamicRegistration(true);
        return rangeFormatting;
    }

    @Override
    public Registry<DocumentRangeFormattingProvider> getDocumentRangeFormattingProviders()
    {
        return documentRangeFormattingProviders;
    }

    @Override
    public DocumentSymbolCapabilities getDocumentSymbolCapabilities()
    {
        SymbolKindCapabilities symbolKind = new SymbolKindCapabilities();
        symbolKind.setValueSet(Arrays.asList(SymbolKind.values()));

        DocumentSymbolCapabilities documentSymbol = new DocumentSymbolCapabilities();
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

    @Override
    public FoldingRangeCapabilities getFoldingRangeCapabilities()
    {
        FoldingRangeCapabilities foldingRange = new FoldingRangeCapabilities();
        foldingRange.setDynamicRegistration(true);
        return foldingRange;
    }

    @Override
    public Registry<FoldingRangeProvider> getFoldingRangeProviders()
    {
        return foldingRangeProviders;
    }

    @Override
    public HoverCapabilities getHoverCapabilities()
    {
        HoverCapabilities hover = new HoverCapabilities();
        hover.setDynamicRegistration(true);
        hover.setContentFormat(Arrays.asList(MarkupKind.MARKDOWN, MarkupKind.PLAINTEXT));
        return hover;
    }

    @Override
    public Registry<HoverProvider> getHoverProviders()
    {
        return hoverProviders;
    }

    @Override
    public ImplementationCapabilities getImplementationCapabilities()
    {
        ImplementationCapabilities implementation = new ImplementationCapabilities();
        implementation.setDynamicRegistration(true);
        implementation.setLinkSupport(true);
        return implementation;
    }

    @Override
    public Registry<ImplementationProvider> getImplementationProviders()
    {
        return implementationProviders;
    }

    @Override
    public ReferencesCapabilities getReferencesCapabilities()
    {
        ReferencesCapabilities references = new ReferencesCapabilities();
        references.setDynamicRegistration(true);
        return references;
    }

    @Override
    public Registry<ReferenceProvider> getReferenceProviders()
    {
        return referenceProviders;
    }

    @Override
    public RenameCapabilities getRenameCapabilities()
    {
        RenameCapabilities rename = new RenameCapabilities();
        rename.setDynamicRegistration(true);
        rename.setPrepareSupport(true);
        return rename;
    }

    @Override
    public Registry<RenameProvider> getRenameProviders()
    {
        return renameProviders;
    }

    @Override
    public SignatureHelpCapabilities getSignatureHelpCapabilities()
    {
        ParameterInformationCapabilities parameterInformation =
            new ParameterInformationCapabilities();
        parameterInformation.setLabelOffsetSupport(true);

        SignatureInformationCapabilities signatureInformation =
            new SignatureInformationCapabilities();
        signatureInformation.setDocumentationFormat(
            Arrays.asList(MarkupKind.MARKDOWN, MarkupKind.PLAINTEXT));
        signatureInformation.setParameterInformation(parameterInformation);

        SignatureHelpCapabilities signatureHelp = new SignatureHelpCapabilities();
        signatureHelp.setDynamicRegistration(true);
        signatureHelp.setContextSupport(true);
        signatureHelp.setSignatureInformation(signatureInformation);
        return signatureHelp;
    }

    @Override
    public Registry<SignatureHelpProvider> getSignatureHelpProviders()
    {
        return signatureHelpProviders;
    }

    @Override
    public TypeDefinitionCapabilities getTypeDefinitionCapabilities()
    {
        TypeDefinitionCapabilities typeDefinition = new TypeDefinitionCapabilities();
        typeDefinition.setDynamicRegistration(true);
        typeDefinition.setLinkSupport(true);
        return typeDefinition;
    }

    @Override
    public Registry<TypeDefinitionProvider> getTypeDefinitionProviders()
    {
        return typeDefinitionProviders;
    }

    @Override
    public SymbolCapabilities getWorkspaceSymbolCapabilities()
    {
        SymbolKindCapabilities symbolKind = new SymbolKindCapabilities();
        symbolKind.setValueSet(Arrays.asList(SymbolKind.values()));

        SymbolCapabilities workspaceSymbol = new SymbolCapabilities();
        workspaceSymbol.setDynamicRegistration(true);
        workspaceSymbol.setSymbolKind(symbolKind);
        return workspaceSymbol;
    }

    @Override
    public Registry<WorkspaceSymbolProvider> getWorkspaceSymbolProviders()
    {
        return workspaceSymbolProviders;
    }
}
