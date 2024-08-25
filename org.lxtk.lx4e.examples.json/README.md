Sample Editor for JSON
======================

This example demonstrates a JSON editor based on the [VSCode JSON language server][1].
The server is started when the first JSON file is opened. This example supports
files in the Eclipse workspace as well as files outside the workspace. Initially,
the server knows the JSON schema for `package.json` files; other schemas can be
specified in JSON files using the `$schema` property.

Before you begin, make sure you have [node][2] installed.

You can set the `node.home` system property in the launch configuration
to point to the directory where `node` resides. This is usually necessary on macOS,
where that directory is not in the application `PATH` by default.

If the language client is unable to connect to the language server and the Error Log contains
`java.io.IOException: Cannot run program "npx": error=2, No such file or directory`,
 consider setting the `node.home` system property as described above.

[1]: https://www.npmjs.com/package/vscode-json-languageserver
[2]: https://nodejs.org
