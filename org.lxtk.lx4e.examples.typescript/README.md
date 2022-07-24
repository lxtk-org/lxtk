Sample Editor for TypeScript
============================

This example demonstrates a TypeScript editor based on the [TypeScript language server][1].
The server is started for a project when the first TypeScript file in the project
is opened. Note that this example does not support external files (i.e. files
outside the Eclipse workspace).

Before you begin, make sure you have [npx][2] installed.
Note that GUI apps on macOS might have a problem with finding `npx`; you can fix
this by running `sudo launchctl config user path "$(npm bin -g):${PATH}"`.

[1]: https://github.com/typescript-language-server/typescript-language-server
[2]: https://www.npmjs.com/package/npx
