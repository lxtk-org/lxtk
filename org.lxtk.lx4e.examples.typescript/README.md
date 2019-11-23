LXTK Eclipse Integration Example: TypeScript Editor
===================================================

This example demonstrates a TypeScript editor based on the [TypeScript language server][1].
The server is started for a project when the first TypeScript file in the project
is opened. Note that this example does not support external files (files outside
the Eclipse workspace).

To try the example, install the `typescript` and `typescript-language-server`
npm modules:

`npm install -g typescript`

`npm install -g typescript-language-server`

Make sure that `npm bin -g` folder (such as `/usr/local/bin`) is in the `PATH`.

Note that GUI apps on macOS don’t have `/usr/local/bin` in their `PATH` by default.
For details, see [My Mac .apps don’t find /usr/local/bin utilities][2].

[1]: https://github.com/theia-ide/typescript-language-server
[2]: https://docs.brew.sh/FAQ#my-mac-apps-dont-find-usrlocalbin-utilities
