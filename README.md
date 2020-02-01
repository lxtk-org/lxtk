# LXTK: Language Client/Server Toolkit [![Build](https://github.com/1C-Company/lxtk/workflows/lxtk%20CI/badge.svg)](https://github.com/1C-Company/lxtk/actions)

LXTK is an open source toolkit for implementing *language clients* talking
to *language servers* according to the [Language Server Protocol][1]. Currently,
it targets Java and Eclipse IDE, and as such, sits between [Eclipse LSP4J][2]
and [Eclipse LSP4E][3] in its abstraction level: it is built on top of LSP4J,
while LSP4E could in theory have been built on top of LXTK. In other words,
it provides a higher level of abstraction than LSP4J and more flexibility
than LSP4E.

[1]: https://microsoft.github.io/language-server-protocol/
[2]: https://www.eclipse.org/lsp4j/
[3]: https://www.eclipse.org/lsp4e/
