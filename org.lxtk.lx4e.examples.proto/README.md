Proto Sample Editor
===================

This example demonstrates a bare-bones text editor based on the
[proto-language-server][1]. The server is started as soon as a file with
the filename extension `proto` is opened in the sample editor. A `proto` file
may contain arbitrary text.

Before you begin, install the language server according to its
[installation instructions][2]. Also, make sure you have [npx][3] installed.
Note that GUI apps on macOS might have a problem with finding `npx`; see
[My Mac .apps don’t find /usr/local/bin utilities][4] for how to fix this.

[1]: https://github.com/lxtk-org/proto-language-server
[2]: https://github.com/lxtk-org/proto-language-server#installing
[3]: https://www.npmjs.com/package/npx
[4]: https://docs.brew.sh/FAQ#my-mac-apps-dont-find-usrlocalbin-utilities
