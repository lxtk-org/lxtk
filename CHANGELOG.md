# LXTK Changelog

The format of this file is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).
The LXTK project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

This release adds client-side support for new features of LSP 3.17, including:

- [x] [Lazy Resolution for Workspace Symbols](https://microsoft.github.io/language-server-protocol/specifications/specification-current/#workspace_symbolResolve)
- [x] [Type Hierarchy](https://microsoft.github.io/language-server-protocol/specifications/specification-current/#textDocument_prepareTypeHierarchy)

Breaking changes:

- Return type of `WorkspaceSymbolProvider.getWorkspaceSymbols` method changed
  from `CompletableFuture<List<? extends SymbolInformation>>` to
  `CompletableFuture<Either<List<? extends SymbolInformation>, List<? extends WorkspaceSymbol>>>`

- Result type for `WorkspaceSymbolRequest` changed from `List<? extends SymbolInformation>`
  to `Either<List<? extends SymbolInformation>, List<? extends WorkspaceSymbol>>`

- Type of elements returned by `WorkspaceSymbolSelectionDialog.getResult` method
  changed from `SymbolInformation` to `WorkspaceSymbolItem`

- Added a required parameter of type `CompletionList` to constructors for
  `BaseCompletionProposal` and `CompletionProposal`

- Parameter type of `WordFinder.isWordPart` method changed from `char` to `int`

Deprecated API elements:

- `SymbolLabelProvider` has been marked as deprecated and will be removed
   in a next release

## [0.3.0] - 2022-06-07

This release adds client-side support for new features of LSP 3.15 and 3.16,
including:

- [x] [Progress Support](https://microsoft.github.io/language-server-protocol/specifications/specification-current/#progress)
   - [x] [Work Done Progress](https://microsoft.github.io/language-server-protocol/specifications/specification-current/#workDoneProgress)
   - [x] [Partial Result Progress](https://microsoft.github.io/language-server-protocol/specifications/specification-current/#partialResults)
- [x] [Call Hierarchy](https://microsoft.github.io/language-server-protocol/specifications/specification-current/#textDocument_prepareCallHierarchy)
- [x] [Change Annotations](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.17/specification/#changeAnnotation)
- [x] Events for File Operations
  - [x] [WillCreateFiles](https://microsoft.github.io/language-server-protocol/specifications/specification-current/#workspace_willCreateFiles)
and [DidCreateFiles](https://microsoft.github.io/language-server-protocol/specifications/specification-current/#workspace_didCreateFiles)
  - [x] [WillDeleteFiles](https://microsoft.github.io/language-server-protocol/specifications/specification-current/#workspace_willDeleteFiles)
and [DidDeleteFiles](https://microsoft.github.io/language-server-protocol/specifications/specification-current/#workspace_didDeleteFiles)
  - [x] [WillRenameFiles](https://microsoft.github.io/language-server-protocol/specifications/specification-current/#workspace_willRenameFiles)
and [DidRenameFiles](https://microsoft.github.io/language-server-protocol/specifications/specification-current/#workspace_didRenameFiles)
- [x] [Linked Editing](https://microsoft.github.io/language-server-protocol/specifications/specification-current/#textDocument_linkedEditingRange)
- [x] [Semantic Tokens](https://microsoft.github.io/language-server-protocol/specifications/specification-current/#textDocument_semanticTokens)

It also adds client-side support for the following LSP features:

- [x] [Resource Operations](https://microsoft.github.io/language-server-protocol/specifications/specification-current/#resourceChanges)
in a [Workspace Edit](https://microsoft.github.io/language-server-protocol/specifications/specification-current/#workspaceEdit)
- [x] Events for Text Documents
  - [x] [WillSave](https://microsoft.github.io/language-server-protocol/specifications/specification-current/#textDocument_willSave)
and [WillSaveWaitUntil](https://microsoft.github.io/language-server-protocol/specifications/specification-current/#textDocument_willSaveWaitUntil)
- [x] [Document Links](https://microsoft.github.io/language-server-protocol/specifications/specification-current/#textDocument_documentLink)

LXTK Integration for Eclipse (LX4E) now supports aggregation of results of
multiple providers for a language feature (previously, the best matching
provider was used).

Also, code completion support has been reworked and improved. In particular:

- The previous implementation, which was derived from LSP4E,
has been replaced completely
- Support for completion snippets has been significantly improved
and is now on par with existing support in VS Code

Note that __LXTK now requires at least Java 11__ (previously, Java 8 was sufficient).

Other notable changes:

- Request classes have been moved from `org.lxtk.lx4e` to a new package,
`org.lxtk.lx4e.requests`

- Class `org.lxtk.lx4e.refactoring.WorkspaceEditChangeFactory` has been redesigned
from the ground up to support resource operations and change annotations in
workspace edits; a new interface, `IWorkspaceEditChangeFactory`,
has been extracted from this class to the `org.lxtk.lx4e` package.

  Breaking changes in `WorkspaceEditChangeFactory`:
  - changed `createChange(String, WorkspaceEdit, RefactoringStatus, IProgressMonitor)`
    to `createChange(String, WorkspaceEdit, IProgressMonitor)`
  - removed `addTextChange(CompositeChange, TextDocumentEdit)`
  - removed `addResourceChange(CompositeChange, ResourceOperation)`

  Related breaking changes in `IUriHandler`:
  - added `getCreateFileChange(URI, CreateFileOptions)`
  - added `getDeleteFileChange(URI, DeleteFileOptions)`
  - added `getRenameFileChange(URI, RenameFileOptions)`

- Support for `DidSave` has been redesigned in line with support for `WillSave`
and `WillSaveUntil`;  a new constructor,
`TextDocumentSyncFeature(DocumentService, TextDocumentWillSaveEventSource, TextDocumentWillSaveWaitUntilEventSource, TextDocumentSaveEventSource)`,
has been introduced to uniformly support all of these events.

  Related breaking changes:
  - removed `DocumentService.onDidSaveTextDocument()`
  - removed `TextDocument.onDidSave()`

- Support for client-side merging of change events for a text document has been
introduced via
`TextDocumentSyncFeature.setChangeEventMergeStrategy(TextDocumentChangeEventMergeStrategy)`
method.

  Related breaking changes:
  - added `DocumentService.onWillChangeTextDocument()`
  - added `TextDocument.onWillChange()`

- `org.lxtk.CommandHandler` and `org.lxtk.CommandService` interfaces
have been redesigned to support progress.

  Breaking changes in `CommandHandler`:
  - changed `execute(List<Object>)` to `execute(ExecuteCommandParams)`
  - added `getRegistrationOptions()`

  Breaking changes in `CommandService`:
  - removed `executeCommand(String, List<Object>)`
  - added `getCommandHandler(String)`

## [0.2.0] - 2021-01-22

This is the first public release of LXTK. It provides client-side support for
the following LSP features:

- [x] [Code Action](https://microsoft.github.io/language-server-protocol/specifications/specification-current/#textDocument_codeAction)
- [x] [Code Lens](https://microsoft.github.io/language-server-protocol/specifications/specification-current/#textDocument_codeLens),
including [Code Lens Resolve](https://microsoft.github.io/language-server-protocol/specifications/specification-current/#codeLens_resolve)
- [x] [Completion](https://microsoft.github.io/language-server-protocol/specifications/specification-current/#textDocument_completion),
including [Completion Item Resolve](https://microsoft.github.io/language-server-protocol/specifications/specification-current/#completionItem_resolve)
- [x] [Diagnostics](https://microsoft.github.io/language-server-protocol/specifications/specification-current/#textDocument_publishDiagnostics)
- [x] [Document Formatting](https://microsoft.github.io/language-server-protocol/specifications/specification-current/#textDocument_formatting),
including [Range Formatting](https://microsoft.github.io/language-server-protocol/specifications/specification-current/#textDocument_rangeFormatting)
- [x] [Document Highlights](https://microsoft.github.io/language-server-protocol/specifications/specification-current/#textDocument_documentHighlight)
- [x] [Document Symbols](https://microsoft.github.io/language-server-protocol/specifications/specification-current/#textDocument_documentSymbol)
- [x] [Find References](https://microsoft.github.io/language-server-protocol/specifications/specification-current/#textDocument_references)
- [x] [Folding Range](https://microsoft.github.io/language-server-protocol/specifications/specification-current/#textDocument_foldingRange)
- [x] [Go to Declaration](https://microsoft.github.io/language-server-protocol/specifications/specification-current/#textDocument_declaration)
- [x] [Go to Definition](https://microsoft.github.io/language-server-protocol/specifications/specification-current/#textDocument_definition)
- [x] [Go to Implementation](https://microsoft.github.io/language-server-protocol/specifications/specification-current/#textDocument_implementation)
- [x] [Go to Type Definition](https://microsoft.github.io/language-server-protocol/specifications/specification-current/#textDocument_typeDefinition)
- [x] [Hover](https://microsoft.github.io/language-server-protocol/specifications/specification-current/#textDocument_hover)
- [x] [Rename](https://microsoft.github.io/language-server-protocol/specifications/specification-current/#textDocument_rename),
including [Prepare Rename](https://microsoft.github.io/language-server-protocol/specifications/specification-current/#textDocument_prepareRename)
- [x] [Signature Help](https://microsoft.github.io/language-server-protocol/specifications/specification-current/#textDocument_signatureHelp)
- [x] [Workspace Edit](https://microsoft.github.io/language-server-protocol/specifications/specification-current/#workspace_applyEdit),
except for resource operations
- [x] [Workspace Folders](https://microsoft.github.io/language-server-protocol/specifications/specification-current/#workspace_workspaceFolders)
- [x] [Workspace Symbols](https://microsoft.github.io/language-server-protocol/specifications/specification-current/#workspace_symbol)

LXTK 0.2 does not support features introduced in LSP 3.15 and above.

[Unreleased]: https://github.com/lxtk-org/lxtk/compare/v0.3...HEAD
[0.3.0]: https://github.com/lxtk-org/lxtk/releases/tag/v0.3
[0.2.0]: https://github.com/lxtk-org/lxtk/releases/tag/v0.2
