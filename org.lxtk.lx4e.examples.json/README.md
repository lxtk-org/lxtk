Sample Editor for JSON
======================

This example demonstrates a JSON editor based on the [VSCode JSON language server][1].
The server is started when the first JSON file is opened. This example supports
files in the Eclipse workspace as well as files outside the workspace. Initially,
the server knows the JSON schema for `package.json` files; other schemas can be
specified in JSON files using the `$schema` property.

Before you begin, make sure you have [npx][2] installed.
Note that GUI apps on macOS might have a problem with finding `npx`; you can fix
this by running `sudo launchctl config user path "$(npm bin -g):${PATH}"`.

[1]: https://www.npmjs.com/package/vscode-json-languageserver
[2]: https://www.npmjs.com/package/npx
