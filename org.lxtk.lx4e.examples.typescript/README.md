Sample Editor for TypeScript
============================

This example demonstrates a TypeScript editor based on the [TypeScript language server][1].
The server is started for a project when the first TypeScript file in the project
is opened. Note that this example does not support external files (i.e. files
outside the Eclipse workspace).

Before you begin, make sure you have [npx][2] installed.
Note that GUI apps on macOS might have a problem with finding `npx`; see
[My Mac .apps don’t find /usr/local/bin utilities][3] for how to fix this.

[1]: https://www.npmjs.com/package/vscode-json-languageserver
[2]: https://www.npmjs.com/package/npx
[3]: https://docs.brew.sh/FAQ#my-mac-apps-dont-find-usrlocalbin-utilities
