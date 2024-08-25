Proto Sample Editor
===================

This example demonstrates a bare-bones text editor based on the
[proto-language-server][1]. The server is started as soon as a file with
the filename extension `proto` is opened in the sample editor. A `proto` file
may contain arbitrary text.

Before you begin, install the language server according to its
[installation instructions][2]. Also, make sure you have [node][3] installed.

You can set the `node.home` system property in the launch configuration
to point to the directory where `node` resides. This is usually necessary on macOS,
where that directory is not in the application `PATH` by default.

If the language client is unable to connect to the language server and the Error Log contains
`java.io.IOException: Cannot run program "npx": error=2, No such file or directory`,
consider setting the `node.home` system property as described above.

[1]: https://github.com/lxtk-org/proto-language-server
[2]: https://github.com/lxtk-org/proto-language-server#installing
[3]: https://nodejs.org
