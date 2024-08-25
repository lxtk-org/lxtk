Sample Editor for TypeScript
============================

This example demonstrates a TypeScript editor based on the [TypeScript language server][1].
The server is started for a project when the first TypeScript file in the project
is opened. Note that this example does not support external files (i.e. files
outside the Eclipse workspace).

Before you begin, make sure you have [node][2] installed.

You can set the `node.home` system property in the launch configuration
to point to the directory where `node` resides. This is usually necessary on macOS,
where that directory is not in the application `PATH` by default.

If the language client is unable to connect to the language server and the Error Log contains
`java.io.IOException: Cannot run program "npx": error=2, No such file or directory`,
consider setting the `node.home` system property as described above.

[1]: https://github.com/typescript-language-server/typescript-language-server
[2]: https://nodejs.org
