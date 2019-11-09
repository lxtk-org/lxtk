LXTK Eclipse Integration Example: JSON Editor
=============================================

This example demonstrates a simple JSON editor based on the [VSCode JSON language server][1].
The server is started when the first JSON file is opened. Note that this example
does not support external files (files outside the Eclipse workspace). Initially,
the server knows the JSON schema for `package.json` files; other schemas can be
specified in JSON files using a `$schema` property.

To try the example, install the `vscode-json-languageserver` npm module:

`npm install -g vscode-json-languageserver`

Make sure that `npm bin -g` folder (such as `/usr/local/bin`) is in the `PATH`.

Note that GUI apps on macOS don’t have `/usr/local/bin` in their `PATH` by default.
For details, see [My Mac .apps don’t find /usr/local/bin utilities][2].

[1]: https://www.npmjs.com/package/vscode-json-languageserver
[2]: https://docs.brew.sh/FAQ#my-mac-apps-dont-find-usrlocalbin-utilities
