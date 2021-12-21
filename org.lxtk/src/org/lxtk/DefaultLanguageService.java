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

import static org.eclipse.lsp4j.SemanticTokenModifiers.Abstract;
import static org.eclipse.lsp4j.SemanticTokenModifiers.Async;
import static org.eclipse.lsp4j.SemanticTokenModifiers.Declaration;
import static org.eclipse.lsp4j.SemanticTokenModifiers.DefaultLibrary;
import static org.eclipse.lsp4j.SemanticTokenModifiers.Definition;
import static org.eclipse.lsp4j.SemanticTokenModifiers.Deprecated;
import static org.eclipse.lsp4j.SemanticTokenModifiers.Documentation;
import static org.eclipse.lsp4j.SemanticTokenModifiers.Modification;
import static org.eclipse.lsp4j.SemanticTokenModifiers.Readonly;
import static org.eclipse.lsp4j.SemanticTokenModifiers.Static;
import static org.eclipse.lsp4j.SemanticTokenTypes.Class;
import static org.eclipse.lsp4j.SemanticTokenTypes.Comment;
import static org.eclipse.lsp4j.SemanticTokenTypes.Enum;
import static org.eclipse.lsp4j.SemanticTokenTypes.EnumMember;
import static org.eclipse.lsp4j.SemanticTokenTypes.Event;
import static org.eclipse.lsp4j.SemanticTokenTypes.Function;
import static org.eclipse.lsp4j.SemanticTokenTypes.Interface;
import static org.eclipse.lsp4j.SemanticTokenTypes.Keyword;
import static org.eclipse.lsp4j.SemanticTokenTypes.Macro;
import static org.eclipse.lsp4j.SemanticTokenTypes.Method;
import static org.eclipse.lsp4j.SemanticTokenTypes.Modifier;
import static org.eclipse.lsp4j.SemanticTokenTypes.Namespace;
import static org.eclipse.lsp4j.SemanticTokenTypes.Number;
import static org.eclipse.lsp4j.SemanticTokenTypes.Operator;
import static org.eclipse.lsp4j.SemanticTokenTypes.Parameter;
import static org.eclipse.lsp4j.SemanticTokenTypes.Property;
import static org.eclipse.lsp4j.SemanticTokenTypes.Regexp;
import static org.eclipse.lsp4j.SemanticTokenTypes.String;
import static org.eclipse.lsp4j.SemanticTokenTypes.Struct;
import static org.eclipse.lsp4j.SemanticTokenTypes.Type;
import static org.eclipse.lsp4j.SemanticTokenTypes.TypeParameter;
import static org.eclipse.lsp4j.SemanticTokenTypes.Variable;
import static org.eclipse.lsp4j.TokenFormat.Relative;

import java.util.List;
import java.util.function.Consumer;

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
import org.eclipse.lsp4j.SemanticTokensClientCapabilitiesRequests;
import org.eclipse.lsp4j.SignatureHelpCapabilities;
import org.eclipse.lsp4j.SymbolCapabilities;
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
    private final Registry<CallHierarchyProvider> callHierarchyProviders = newRegistry();
    private final Registry<CodeActionProvider> codeActionProviders = newRegistry();
    private final Registry<CodeLensProvider> codeLensProviders = newRegistry();
    private final Registry<CompletionProvider> completionProviders = newRegistry();
    private final Registry<DeclarationProvider> declarationProviders = newRegistry();
    private final Registry<DefinitionProvider> definitionProviders = newRegistry();
    private final Registry<DocumentFormattingProvider> documentFormattingProviders = newRegistry();
    private final Registry<DocumentHighlightProvider> documentHighlightProviders = newRegistry();
    private final Registry<DocumentLinkProvider> documentLinkProviders = newRegistry();
    private final Registry<DocumentRangeFormattingProvider> documentRangeFormattingProviders =
        newRegistry();
    private final Registry<DocumentSemanticTokensProvider> documentSemanticTokensProviders =
        newRegistry();
    private final Registry<DocumentSymbolProvider> documentSymbolProviders = newRegistry();
    private final Registry<FoldingRangeProvider> foldingRangeProviders = newRegistry();
    private final Registry<HoverProvider> hoverProviders = newRegistry();
    private final Registry<ImplementationProvider> implementationProviders = newRegistry();
    private final Registry<LinkedEditingRangeProvider> linkedEditingRangeProviders = newRegistry();
    private final Registry<ReferenceProvider> referenceProviders = newRegistry();
    private final Registry<RenameProvider> renameProviders = newRegistry();
    private final Registry<SignatureHelpProvider> signatureHelpProviders = newRegistry();
    private final Registry<TypeDefinitionProvider> typeDefinitionProviders = newRegistry();
    private final Registry<WorkspaceSymbolProvider> workspaceSymbolProviders = newRegistry();

    @Override
    public CallHierarchyCapabilities getCallHierarchyCapabilities()
    {
        return new CallHierarchyCapabilities();
    }

    @Override
    public Registry<CallHierarchyProvider> getCallHierarchyProviders()
    {
        return callHierarchyProviders;
    }

    @Override
    public CodeActionCapabilities getCodeActionCapabilities()
    {
        return new CodeActionCapabilities();
    }

    @Override
    public Registry<CodeActionProvider> getCodeActionProviders()
    {
        return codeActionProviders;
    }

    @Override
    public CodeLensCapabilities getCodeLensCapabilities()
    {
        return new CodeLensCapabilities();
    }

    @Override
    public Registry<CodeLensProvider> getCodeLensProviders()
    {
        return codeLensProviders;
    }

    @Override
    public CompletionCapabilities getCompletionCapabilities()
    {
        return new CompletionCapabilities();
    }

    @Override
    public Registry<CompletionProvider> getCompletionProviders()
    {
        return completionProviders;
    }

    @Override
    public DeclarationCapabilities getDeclarationCapabilities()
    {
        return new DeclarationCapabilities();
    }

    @Override
    public Registry<DeclarationProvider> getDeclarationProviders()
    {
        return declarationProviders;
    }

    @Override
    public DefinitionCapabilities getDefinitionCapabilities()
    {
        return new DefinitionCapabilities();
    }

    @Override
    public Registry<DefinitionProvider> getDefinitionProviders()
    {
        return definitionProviders;
    }

    @Override
    public FormattingCapabilities getDocumentFormattingCapabilities()
    {
        return new FormattingCapabilities();
    }

    @Override
    public Registry<DocumentFormattingProvider> getDocumentFormattingProviders()
    {
        return documentFormattingProviders;
    }

    @Override
    public DocumentHighlightCapabilities getDocumentHighlightCapabilities()
    {
        return new DocumentHighlightCapabilities();
    }

    @Override
    public Registry<DocumentHighlightProvider> getDocumentHighlightProviders()
    {
        return documentHighlightProviders;
    }

    @Override
    public DocumentLinkCapabilities getDocumentLinkCapabilities()
    {
        return new DocumentLinkCapabilities();
    }

    @Override
    public Registry<DocumentLinkProvider> getDocumentLinkProviders()
    {
        return documentLinkProviders;
    }

    @Override
    public RangeFormattingCapabilities getDocumentRangeFormattingCapabilities()
    {
        return new RangeFormattingCapabilities();
    }

    @Override
    public Registry<DocumentRangeFormattingProvider> getDocumentRangeFormattingProviders()
    {
        return documentRangeFormattingProviders;
    }

    @Override
    public SemanticTokensCapabilities getDocumentSemanticTokensCapabilities()
    {
        return new SemanticTokensCapabilities(new SemanticTokensClientCapabilitiesRequests(),
            List.of(Namespace, Type, Class, Enum, Interface, Struct, TypeParameter, Parameter,
                Variable, Property, EnumMember, Event, Function, Method, Macro, Keyword, Modifier,
                Comment, String, Number, Regexp, Operator),
            List.of(Declaration, Definition, Readonly, Static, Deprecated, Abstract, Async,
                Modification, Documentation, DefaultLibrary),
            List.of(Relative));
    }

    @Override
    public Registry<DocumentSemanticTokensProvider> getDocumentSemanticTokensProviders()
    {
        return documentSemanticTokensProviders;
    }

    @Override
    public DocumentSymbolCapabilities getDocumentSymbolCapabilities()
    {
        return new DocumentSymbolCapabilities();
    }

    @Override
    public Registry<DocumentSymbolProvider> getDocumentSymbolProviders()
    {
        return documentSymbolProviders;
    }

    @Override
    public FoldingRangeCapabilities getFoldingRangeCapabilities()
    {
        return new FoldingRangeCapabilities();
    }

    @Override
    public Registry<FoldingRangeProvider> getFoldingRangeProviders()
    {
        return foldingRangeProviders;
    }

    @Override
    public HoverCapabilities getHoverCapabilities()
    {
        return new HoverCapabilities();
    }

    @Override
    public Registry<HoverProvider> getHoverProviders()
    {
        return hoverProviders;
    }

    @Override
    public ImplementationCapabilities getImplementationCapabilities()
    {
        return new ImplementationCapabilities();
    }

    @Override
    public Registry<ImplementationProvider> getImplementationProviders()
    {
        return implementationProviders;
    }

    @Override
    public LinkedEditingRangeCapabilities getLinkedEditingRangeCapabilities()
    {
        return new LinkedEditingRangeCapabilities();
    }

    @Override
    public Registry<LinkedEditingRangeProvider> getLinkedEditingRangeProviders()
    {
        return linkedEditingRangeProviders;
    }

    @Override
    public ReferencesCapabilities getReferencesCapabilities()
    {
        return new ReferencesCapabilities();
    }

    @Override
    public Registry<ReferenceProvider> getReferenceProviders()
    {
        return referenceProviders;
    }

    @Override
    public RenameCapabilities getRenameCapabilities()
    {
        return new RenameCapabilities();
    }

    @Override
    public Registry<RenameProvider> getRenameProviders()
    {
        return renameProviders;
    }

    @Override
    public SignatureHelpCapabilities getSignatureHelpCapabilities()
    {
        return new SignatureHelpCapabilities();
    }

    @Override
    public Registry<SignatureHelpProvider> getSignatureHelpProviders()
    {
        return signatureHelpProviders;
    }

    @Override
    public TypeDefinitionCapabilities getTypeDefinitionCapabilities()
    {
        return new TypeDefinitionCapabilities();
    }

    @Override
    public Registry<TypeDefinitionProvider> getTypeDefinitionProviders()
    {
        return typeDefinitionProviders;
    }

    @Override
    public SymbolCapabilities getWorkspaceSymbolCapabilities()
    {
        return new SymbolCapabilities();
    }

    @Override
    public Registry<WorkspaceSymbolProvider> getWorkspaceSymbolProviders()
    {
        return workspaceSymbolProviders;
    }

    /**
     * Returns an exception logger for this service.
     *
     * @return a logger instance (may be <code>null</code>)
     */
    protected Consumer<Throwable> getLogger()
    {
        return null;
    }

    private <E> Registry<E> newRegistry()
    {
        return Registry.newInstance(getLogger());
    }
}
