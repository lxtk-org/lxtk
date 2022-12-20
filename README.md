# LXTK: Language Client/Server Toolkit [![Build](https://github.com/lxtk-org/lxtk/workflows/build/badge.svg)](https://github.com/lxtk-org/lxtk/actions)

LXTK is an open source toolkit for implementing *language clients* talking
to *language servers* according to the [Language Server Protocol][1] (LSP).
Currently, it targets Java and Eclipse IDE, and as such, sits between
[Eclipse LSP4J][2] and [Eclipse LSP4E][3] in its abstraction level:
it is built using LSP4J, while LSP4E could in theory have been built
using LXTK. In other words, it provides a higher level of abstraction
than LSP4J and more flexibility than LSP4E.

## Why LXTK?

- *Object-oriented*: Can be incrementally specialized in numerous ways
to cater for different requirements.

- *Not opinionated*: Provides a set of bricks combinable to a complete
structure that is right for you.

- *API centric*: Every aspect can be controlled, extended or replaced as needed,
reliably.

- *Robust*: Shown to be reliable and effective by being used in a widely
distributed commercial product.

- *Fully dynamic*: Support for dynamic capability registration built right
into the core.

- *Integrated with* [Eclipse Handly][4]: Can be used to build a handle-based
model as a pillar for an Eclipse-based IDE.

- *Comes with realistic examples*: Includes JSON and TypeScript sample editors.

## Features

The current LXTK version (0.3) provides client-side support for the following
LSP features:

- [x] [Call Hierarchy](https://microsoft.github.io/language-server-protocol/specifications/specification-current/#textDocument_prepareCallHierarchy)
- [x] [Change Annotations](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.17/specification/#changeAnnotation)
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
- [x] Events for File Operations
  - [x] [WillCreateFiles](https://microsoft.github.io/language-server-protocol/specifications/specification-current/#workspace_willCreateFiles)
and [DidCreateFiles](https://microsoft.github.io/language-server-protocol/specifications/specification-current/#workspace_didCreateFiles)
  - [x] [WillDeleteFiles](https://microsoft.github.io/language-server-protocol/specifications/specification-current/#workspace_willDeleteFiles)
and [DidDeleteFiles](https://microsoft.github.io/language-server-protocol/specifications/specification-current/#workspace_didDeleteFiles)
  - [x] [WillRenameFiles](https://microsoft.github.io/language-server-protocol/specifications/specification-current/#workspace_willRenameFiles)
and [DidRenameFiles](https://microsoft.github.io/language-server-protocol/specifications/specification-current/#workspace_didRenameFiles)
- [x] [Find References](https://microsoft.github.io/language-server-protocol/specifications/specification-current/#textDocument_references)
- [x] [Folding Range](https://microsoft.github.io/language-server-protocol/specifications/specification-current/#textDocument_foldingRange)
- [x] [Go to Declaration](https://microsoft.github.io/language-server-protocol/specifications/specification-current/#textDocument_declaration)
- [x] [Go to Definition](https://microsoft.github.io/language-server-protocol/specifications/specification-current/#textDocument_definition)
- [x] [Go to Implementation](https://microsoft.github.io/language-server-protocol/specifications/specification-current/#textDocument_implementation)
- [x] [Go to Type Definition](https://microsoft.github.io/language-server-protocol/specifications/specification-current/#textDocument_typeDefinition)
- [x] [Hover](https://microsoft.github.io/language-server-protocol/specifications/specification-current/#textDocument_hover)
- [x] [Linked Editing](https://microsoft.github.io/language-server-protocol/specifications/specification-current/#textDocument_linkedEditingRange)
- [x] [Progress Support](https://microsoft.github.io/language-server-protocol/specifications/specification-current/#progress)
   - [x] [Work Done Progress](https://microsoft.github.io/language-server-protocol/specifications/specification-current/#workDoneProgress)
   - [x] [Partial Result Progress](https://microsoft.github.io/language-server-protocol/specifications/specification-current/#partialResults)
- [x] [Rename](https://microsoft.github.io/language-server-protocol/specifications/specification-current/#textDocument_rename),
including [Prepare Rename](https://microsoft.github.io/language-server-protocol/specifications/specification-current/#textDocument_prepareRename)
- [x] [Semantic Tokens](https://microsoft.github.io/language-server-protocol/specifications/specification-current/#textDocument_semanticTokens)
- [x] [Signature Help](https://microsoft.github.io/language-server-protocol/specifications/specification-current/#textDocument_signatureHelp)
- [x] [Workspace Edit](https://microsoft.github.io/language-server-protocol/specifications/specification-current/#workspace_applyEdit),
including [Resource Operations](https://microsoft.github.io/language-server-protocol/specifications/specification-current/#resourceChanges)
- [x] [Workspace Folders](https://microsoft.github.io/language-server-protocol/specifications/specification-current/#workspace_workspaceFolders)
- [x] [Workspace Symbols](https://microsoft.github.io/language-server-protocol/specifications/specification-current/#workspace_symbol)

LXTK 0.3 does not support features introduced in LSP 3.17 or above.

## Architectural Overview

Currently, there are two main architectural layers in LXTK:

- LXTK Core Framework

- LXTK Integration for Eclipse (LX4E)

LXTK Core Framework (`org.lxtk` bundle) is a Java class library for building
LSP clients. It is built on top of LSP4J. Rather than exposing LSP4J services
directly, LXTK provides its own service layer. This layer is somewhat similar
to the Extension API of Visual Studio Code, but is tailored specifically to LSP.
In particular, its API directly reuses protocol data types of LSP4J. The
Core Framework can be used in any Java-based client (Eclipse, IntelliJ, etc.)

LXTK Integration for Eclipse (LX4E) is subdivided into the Core layer
(`org.lxtk.lx4e` bundle) and the UI layer (`org.lxtk.lx4e.ui` bundle).
LX4E is not a tool, i.e. it does not provide a generic LSP client for Eclipse.
Instead, it provides a platform for building Eclipse-based development tools
using LSP-based services provided by the Core Framework. To that end, it extends
the Core Framework and integrates it with the Eclipse Platform. Although LX4E
can be used for building a generic LSP client for Eclipse, its main goal is to
facilitate tight integration of specific language servers in a full-featured
custom Eclipse-based IDE.

LX4E includes several examples (`org.lxtk.lx4e.examples.*` bundles) that
demonstrate main aspects of its usage.

For additional information on how to use some of the features of LXTK Core
Framework and LX4E, see [FAQ](FAQ.md).

For an overview of the API provided by LXTK Core Framework and LX4E, please see
the Javadocs for the exported packages of the respective bundles.

## Developer Resources

- [Building](BUILDING.md): How to build LXTK locally.

- [Contributing](CONTRIBUTING.md): How to report bugs, set up a developer
workspace, issue pull requests, etc.

## Contacts

- Website: <https://lxtk.org>

- Community Discussions: <https://github.com/lxtk-org/lxtk/discussions>

- General Inquiries: *info* (at) *lxtk.org*

## License

LXTK is licensed under the Eclipse Public License 2.0. See [LICENSE](LICENSE)
for the full license text.

[1]: https://microsoft.github.io/language-server-protocol/
[2]: https://www.eclipse.org/lsp4j/
[3]: https://www.eclipse.org/lsp4e/
[4]: https://www.eclipse.org/handly/
