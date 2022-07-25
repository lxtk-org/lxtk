Proto Sample Editor
===================

This example demonstrates a bare-bones text editor based on the
[proto-language-server][1]. The server is started as soon as a file with
the filename extension `proto` is opened in the sample editor. A `proto` file
may contain arbitrary text.

Before you begin, install the language server according to its
[installation instructions][2]. Also, make sure you have [npx][3] installed.

Note that GUI apps on macOS might have a problem with finding `npx`;
if the folder outputted by `npm bin -g` is not in the `PATH` reported by
`launchctl getenv PATH`, you can fix this by running
```
sudo launchctl config user path "$(npm bin -g):$(launchctl getenv PATH))"
```
and then rebooting, as documented in `man launchctl`.

[1]: https://github.com/lxtk-org/proto-language-server
[2]: https://github.com/lxtk-org/proto-language-server#installing
[3]: https://www.npmjs.com/package/npx
