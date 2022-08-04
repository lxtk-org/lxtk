/*******************************************************************************
 * Copyright (c) 2019, 2022 1C-Soft LLC.
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

import java.util.List;
import java.util.function.Consumer;

import org.eclipse.lsp4j.CodeActionCapabilities;
import org.eclipse.lsp4j.CodeActionKind;
import org.eclipse.lsp4j.CodeActionKindCapabilities;
import org.eclipse.lsp4j.CodeActionLiteralSupportCapabilities;
import org.eclipse.lsp4j.CodeActionResolveSupportCapabilities;
import org.eclipse.lsp4j.CompletionCapabilities;
import org.eclipse.lsp4j.CompletionItemCapabilities;
import org.eclipse.lsp4j.CompletionItemInsertTextModeSupportCapabilities;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.CompletionItemKindCapabilities;
import org.eclipse.lsp4j.CompletionItemTag;
import org.eclipse.lsp4j.CompletionItemTagSupportCapabilities;
import org.eclipse.lsp4j.CompletionListCapabilities;
import org.eclipse.lsp4j.DeclarationCapabilities;
import org.eclipse.lsp4j.DefinitionCapabilities;
import org.eclipse.lsp4j.DocumentLinkCapabilities;
import org.eclipse.lsp4j.DocumentSymbolCapabilities;
import org.eclipse.lsp4j.FoldingRangeCapabilities;
import org.eclipse.lsp4j.HoverCapabilities;
import org.eclipse.lsp4j.ImplementationCapabilities;
import org.eclipse.lsp4j.InsertTextMode;
import org.eclipse.lsp4j.MarkupKind;
import org.eclipse.lsp4j.ParameterInformationCapabilities;
import org.eclipse.lsp4j.RenameCapabilities;
import org.eclipse.lsp4j.SemanticTokensCapabilities;
import org.eclipse.lsp4j.SignatureHelpCapabilities;
import org.eclipse.lsp4j.SignatureInformationCapabilities;
import org.eclipse.lsp4j.SymbolCapabilities;
import org.eclipse.lsp4j.SymbolKind;
import org.eclipse.lsp4j.SymbolKindCapabilities;
import org.eclipse.lsp4j.SymbolTag;
import org.eclipse.lsp4j.SymbolTagSupportCapabilities;
import org.eclipse.lsp4j.TypeDefinitionCapabilities;
import org.eclipse.lsp4j.WorkspaceSymbolResolveSupportCapabilities;
import org.lxtk.DefaultLanguageService;
import org.lxtk.LanguageService;
import org.lxtk.lx4e.internal.Activator;

/**
 * LX4E-specific implementation of {@link LanguageService}.
 * <p>
 * This implementation is thread-safe.
 * </p>
 */
public class EclipseLanguageService
    extends DefaultLanguageService
{
    @Override
    public CodeActionCapabilities getCodeActionCapabilities()
    {
        CodeActionKindCapabilities codeActionKind = new CodeActionKindCapabilities();
        codeActionKind.setValueSet(List.of(CodeActionKind.QuickFix, CodeActionKind.Refactor,
            CodeActionKind.RefactorExtract, CodeActionKind.RefactorInline,
            CodeActionKind.RefactorRewrite, CodeActionKind.Source,
            CodeActionKind.SourceOrganizeImports));

        CodeActionLiteralSupportCapabilities codeActionLiteralSupport =
            new CodeActionLiteralSupportCapabilities();
        codeActionLiteralSupport.setCodeActionKind(codeActionKind);

        CodeActionCapabilities codeAction = new CodeActionCapabilities();
        codeAction.setCodeActionLiteralSupport(codeActionLiteralSupport);
        codeAction.setDisabledSupport(true);
        codeAction.setResolveSupport(new CodeActionResolveSupportCapabilities(List.of("edit"))); //$NON-NLS-1$
        codeAction.setDataSupport(true);
        codeAction.setHonorsChangeAnnotations(true);
        return codeAction;
    }

    @Override
    public CompletionCapabilities getCompletionCapabilities()
    {
        CompletionItemTagSupportCapabilities tagSupport =
            new CompletionItemTagSupportCapabilities();
        tagSupport.setValueSet(List.of(CompletionItemTag.values()));

        CompletionItemCapabilities completionItem = new CompletionItemCapabilities();
        completionItem.setSnippetSupport(true);
        completionItem.setDocumentationFormat(List.of(MarkupKind.MARKDOWN, MarkupKind.PLAINTEXT));
        completionItem.setDeprecatedSupport(true);
        completionItem.setTagSupport(tagSupport);
        completionItem.setCommitCharactersSupport(true);
        completionItem.setInsertReplaceSupport(true);
        completionItem.setInsertTextModeSupport(new CompletionItemInsertTextModeSupportCapabilities(
            List.of(InsertTextMode.AsIs, InsertTextMode.AdjustIndentation)));
        completionItem.setLabelDetailsSupport(true);

        CompletionItemKindCapabilities completionItemKind = new CompletionItemKindCapabilities();
        completionItemKind.setValueSet(List.of(CompletionItemKind.values()));

        CompletionListCapabilities completionList = new CompletionListCapabilities();
        completionList.setItemDefaults(List.of("commitCharacters", "editRange", //$NON-NLS-1$ //$NON-NLS-2$
            "insertTextFormat", "insertTextMode")); //$NON-NLS-1$ //$NON-NLS-2$

        CompletionCapabilities completion = new CompletionCapabilities();
        completion.setCompletionItem(completionItem);
        completion.setCompletionItemKind(completionItemKind);
        completion.setCompletionList(completionList);
        completion.setInsertTextMode(InsertTextMode.AsIs);
        return completion;
    }

    @Override
    public DeclarationCapabilities getDeclarationCapabilities()
    {
        DeclarationCapabilities declaration = new DeclarationCapabilities();
        declaration.setLinkSupport(true);
        return declaration;
    }

    @Override
    public DefinitionCapabilities getDefinitionCapabilities()
    {
        DefinitionCapabilities definition = new DefinitionCapabilities();
        definition.setLinkSupport(true);
        return definition;
    }

    @Override
    public DocumentLinkCapabilities getDocumentLinkCapabilities()
    {
        DocumentLinkCapabilities documentLink = new DocumentLinkCapabilities();
        documentLink.setTooltipSupport(true);
        return documentLink;
    }

    @Override
    public SemanticTokensCapabilities getDocumentSemanticTokensCapabilities()
    {
        SemanticTokensCapabilities semanticTokens = super.getDocumentSemanticTokensCapabilities();
        semanticTokens.setMultilineTokenSupport(true);
        return semanticTokens;
    }

    @Override
    public DocumentSymbolCapabilities getDocumentSymbolCapabilities()
    {
        SymbolKindCapabilities symbolKind = new SymbolKindCapabilities();
        symbolKind.setValueSet(List.of(SymbolKind.values()));

        SymbolTagSupportCapabilities tagSupport = new SymbolTagSupportCapabilities();
        tagSupport.setValueSet(List.of(SymbolTag.values()));

        DocumentSymbolCapabilities documentSymbol = new DocumentSymbolCapabilities();
        documentSymbol.setSymbolKind(symbolKind);
        documentSymbol.setHierarchicalDocumentSymbolSupport(true);
        documentSymbol.setTagSupport(tagSupport);
        return documentSymbol;
    }

    @Override
    public FoldingRangeCapabilities getFoldingRangeCapabilities()
    {
        FoldingRangeCapabilities capabilities = new FoldingRangeCapabilities();
        capabilities.setLineFoldingOnly(true);
        return capabilities;
    }

    @Override
    public HoverCapabilities getHoverCapabilities()
    {
        HoverCapabilities hover = new HoverCapabilities();
        hover.setContentFormat(List.of(MarkupKind.MARKDOWN, MarkupKind.PLAINTEXT));
        return hover;
    }

    @Override
    public ImplementationCapabilities getImplementationCapabilities()
    {
        ImplementationCapabilities implementation = new ImplementationCapabilities();
        implementation.setLinkSupport(true);
        return implementation;
    }

    @Override
    public RenameCapabilities getRenameCapabilities()
    {
        RenameCapabilities rename = new RenameCapabilities();
        rename.setPrepareSupport(true);
        rename.setHonorsChangeAnnotations(true);
        return rename;
    }

    @Override
    public SignatureHelpCapabilities getSignatureHelpCapabilities()
    {
        ParameterInformationCapabilities parameterInformation =
            new ParameterInformationCapabilities();
        parameterInformation.setLabelOffsetSupport(true);

        SignatureInformationCapabilities signatureInformation =
            new SignatureInformationCapabilities();
        signatureInformation.setParameterInformation(parameterInformation);
        signatureInformation.setActiveParameterSupport(true);

        SignatureHelpCapabilities signatureHelp = new SignatureHelpCapabilities();
        signatureHelp.setContextSupport(true);
        signatureHelp.setSignatureInformation(signatureInformation);
        return signatureHelp;
    }

    @Override
    public TypeDefinitionCapabilities getTypeDefinitionCapabilities()
    {
        TypeDefinitionCapabilities typeDefinition = new TypeDefinitionCapabilities();
        typeDefinition.setLinkSupport(true);
        return typeDefinition;
    }

    @Override
    public SymbolCapabilities getWorkspaceSymbolCapabilities()
    {
        SymbolKindCapabilities symbolKind = new SymbolKindCapabilities();
        symbolKind.setValueSet(List.of(SymbolKind.values()));

        SymbolTagSupportCapabilities tagSupport = new SymbolTagSupportCapabilities();
        tagSupport.setValueSet(List.of(SymbolTag.values()));

        WorkspaceSymbolResolveSupportCapabilities resolveSupport =
            new WorkspaceSymbolResolveSupportCapabilities();
        resolveSupport.setProperties(List.of("location.range")); //$NON-NLS-1$

        SymbolCapabilities workspaceSymbol = new SymbolCapabilities();
        workspaceSymbol.setSymbolKind(symbolKind);
        workspaceSymbol.setTagSupport(tagSupport);
        workspaceSymbol.setResolveSupport(resolveSupport);
        return workspaceSymbol;
    }

    @Override
    protected Consumer<Throwable> getLogger()
    {
        return Activator.LOGGER;
    }
}
