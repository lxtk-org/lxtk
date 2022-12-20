# LXTK/LX4E FAQ

## Diagnostics

### How do I add support for [diagnostic pulling](https://microsoft.github.io/language-server-protocol/specifications/specification-current/#textDocument_pullDiagnostics)?

Basically, you need to add a `org.lxtk.client.DiagnosticFeature` to the list of
features of your language client.

Using `org.lxtk.lx4e.examples.proto` as an example:

```java
public class ProtoLanguageClient
    extends EclipseLanguageClientController<LanguageServer>
{
    ...
    @Override
    protected AbstractLanguageClient<LanguageServer> getLanguageClient()
    {
        Collection<Feature<? super LanguageServer>> features = new ArrayList<>();

        TextDocumentSyncFeature textDocumentSyncFeature = TextDocumentSyncFeature.newInstance(
            DOCUMENT_SERVICE, Activator.getDefault().getDocumentProvider());
        features.add(textDocumentSyncFeature);

        DiagnosticFeature diagnosticFeature =
            new DiagnosticFeature(Activator.getDefault().getUiDocumentService(),
                textDocumentSyncFeature, diagnosticProvider ->
                {
                    return SafeRun.runWithResult(rollback ->
                    {
                        DiagnosticAnnotations diagnosticAnnotations =
                            new DiagnosticAnnotations(DOCUMENT_SERVICE);
                        rollback.add(diagnosticAnnotations::dispose);

                        DefaultDiagnosticRequestor diagnosticRequestor =
                            new DefaultDiagnosticRequestor(diagnosticProvider,
                                (uri, report) -> diagnosticAnnotations.accept(
                                    uri, report.getItems()),
                                log());
                        rollback.add(diagnosticRequestor::dispose);

                        diagnosticRequestor.onDispose().thenRun(diagnosticAnnotations::dispose);
                        return diagnosticRequestor;
                    });
                }, diagnosticProvider ->
                {
                    return SafeRun.runWithResult(rollback ->
                    {
                        DiagnosticMarkers diagnosticMarkers =
                            new DiagnosticMarkers("org.lxtk.lx4e.examples.proto.problem");
                        rollback.add(diagnosticMarkers::dispose);

                        DefaultWorkspaceDiagnosticRequestor diagnosticRequestor =
                            new DefaultWorkspaceDiagnosticRequestor(diagnosticProvider,
                                report -> diagnosticMarkers.accept(
                                    DocumentUri.convert(report.getUri()), report.getItems()),
                                log());
                        rollback.add(diagnosticRequestor::dispose);

                        diagnosticRequestor.onDispose().thenRun(diagnosticMarkers::dispose);
                        return diagnosticRequestor;
                    });
                });
        features.add(diagnosticFeature);

        IResourceChangeListener resourceChangeListener = new IResourceChangeListener()
        {
            @Override
            public void resourceChanged(IResourceChangeEvent event)
            {
                if (isAffectedBy(event.getDelta()))
                    diagnosticFeature.triggerWorkspacePull();
            }

            private static boolean isAffectedBy(IResourceDelta delta)
            {
                int kind = delta.getKind();
                int flags = delta.getFlags();
                IResource resource = delta.getResource();
                if (resource.getType() == IResource.FILE
                    && "proto".equals(resource.getFileExtension()))
                {
                    if (kind == IResourceDelta.ADDED || (kind == IResourceDelta.CHANGED
                        && (flags & IResourceDelta.CONTENT) != 0))
                        return true;
                }
                IResourceDelta[] children = delta.getAffectedChildren();
                for (IResourceDelta child : children)
                {
                    if (isAffectedBy(child))
                        return true;
                }
                return false;
            }
        };

        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        workspace.addResourceChangeListener(resourceChangeListener,
            IResourceChangeEvent.POST_CHANGE);

        return new EclipseLanguageClient<>(log(), diagnosticConsumer,
            Activator.getDefault().getWorkspaceEditChangeFactory(), features)
        {
            @Override
            public void dispose()
            {
                workspace.removeResourceChangeListener(resourceChangeListener);
                super.dispose();
            }
            ...
        }
    }
    ...
}
```

Here, [document diagnostics](https://microsoft.github.io/language-server-protocol/specifications/specification-current/#textDocument_diagnostic)
are pulled with a `org.lxtk.client.DefaultDiagnosticRequestor` and converted to
diagnostic annotations when the user opens/edits a `.proto` file.

Also, [workspace diagnostics](https://microsoft.github.io/language-server-protocol/specifications/specification-current/#workspace_diagnostic)
are pulled with a `org.lxtk.client.DefaultWorkspaceDiagnosticRequestor`
and converted to diagnostic markers for `.proto` files in the workspace.
A resource change listener is used to (re-)trigger workspace diagnostic pull
when a `.proto` file is added or changed.

Note that you need to pass a `CompletableFuture` with the result type
`org.lxtk.UiDocumentService` to the constructor of the diagnostic feature.
The activator for the `org.lxtk.lx4e.examples.proto` bundle creates an instance
of the `org.lxtk.lx4e.ui.EclipseUiDocumentService` when the bundle is started:

```java
public class Activator
    extends AbstractUIPlugin
{
    ...
    private final CompletableFuture<EclipseUiDocumentService> uiDocumentService =
        new CompletableFuture<>();

    public CompletableFuture<? extends UiDocumentService> getUiDocumentService()
    {
        return uiDocumentService;
    }

    @Override
    public void start(BundleContext context) throws Exception
    {
        super.start(context);
        plugin = this;

        SafeRun.run(rollback ->
        {
            ...
            uiDocumentService.completeAsync(
                () -> new EclipseUiDocumentService(DOCUMENT_SERVICE, LOGGER),
                Activator::asyncExec).whenComplete((o, e) ->
                {
                    if (e != null)
                        logError(e);
                });
            rollback.add(() ->
            {
                uiDocumentService.thenAcceptAsync(EclipseUiDocumentService::dispose,
                    Activator::asyncExec);
            });
            ...
        });
    }

    private static void asyncExec(Runnable runnable)
    {
        Display display = PlatformUI.getWorkbench().getDisplay();
        if (!display.isDisposed())
            display.asyncExec(runnable);
    }
    ...
}
```

(Note that an instance of `EclipseUiDocumentService` may only be created and
disposed in the UI thread.)

Finally, you need to register a `org.lxtk.lx4e.ui.diagnostics.DiagnosticAnnotationModelListener`
and use a special `org.lxtk.lx4e.ui.diagnostics.DiagnosticAnnotationAccess`
to ensure that those marker annotations for workspace diagnostics that are
overlayed with document diagnostic annotations are marked as deleted and
their images are grayed out:

```java
public class ProtoDocumentProvider
    extends TextFileDocumentProvider
{
    ...
    @Override
    protected FileInfo createFileInfo(Object element) throws CoreException
    {
        XFileInfo info = (XFileInfo)super.createFileInfo(element);
        if (info == null || info.fTextFileBuffer == null)
            return null;

        try (
            TextFileBuffer buffer = info.fTextFileBufferLocationKind == null
                ? TextFileBuffer.forFileStore(info.fTextFileBuffer.getFileStore())
                : TextFileBuffer.forLocation(info.fTextFileBuffer.getLocation(),
                    info.fTextFileBufferLocationKind);)
        {
            SafeRun.run(rollback ->
            {
                ...
                if (info.fModel != null)
                {
                    IAnnotationModelListener annotationModelListener =
                        new DiagnosticAnnotationModelListener();
                    info.fModel.addAnnotationModelListener(annotationModelListener);
                    rollback.add(
                        () -> info.fModel.removeAnnotationModelListener(annotationModelListener));
                }
                ...
            });
        }
        return info;
    }
    ...
}

public class ProtoEditor
    extends AbstractDecoratedTextEditor
{
    ...
    @Override
    protected IAnnotationAccess createAnnotationAccess()
    {
        return new DiagnosticAnnotationAccess();
    }
    ...
}

public class ProtoSourceViewerConfiguration
    extends TextSourceViewerConfiguration
{
    ...
    @Override
    protected boolean isShownInText(Annotation annotation)
    {
        if (annotation.isMarkedDeleted())
            return false;

        return super.isShownInText(annotation);
    }
    ...
}
```

## Type Hierarchy

### How do I add support for a [type hierarchy](https://microsoft.github.io/language-server-protocol/specifications/specification-current/#textDocument_prepareTypeHierarchy)?

First, you need to add a `org.lxtk.client.TypeHierarchyFeature` to the list of
features of your language client.

Using `org.lxtk.lx4e.examples.proto` as an example:

```java
public class ProtoLanguageClient
    extends EclipseLanguageClientController<LanguageServer>
{
    ...
    @Override
    protected AbstractLanguageClient<LanguageServer> getLanguageClient()
    {
        Collection<Feature<? super LanguageServer>> features = new ArrayList<>();
        ...
        features.add(new TypeHierarchyFeature(LANGUAGE_SERVICE));
        ...
        return new EclipseLanguageClient<>(log(), diagnosticConsumer,
            Activator.getDefault().getWorkspaceEditChangeFactory(), features)
        {
            ...
        }
    }
    ...
}
```

Then, you need to contribute a type hierarchy view through an extension in
`plugin.xml` for the plug-in:

```xml
   <extension
         point="org.eclipse.ui.views">
      <view
            id="org.lxtk.lx4e.examples.proto.ProtoTypeHierarchyView"
            name="Proto Type Hierarchy"
            class="org.lxtk.lx4e.ui.typehierarchy.DefaultTypeHierarchyView">
      </view>
   </extension>
```

Finally, you need to contribute a command handler that opens a type hierarchy
for the current selection in the editor:

```xml
   <extension
         point="org.eclipse.ui.commands">
      <command
            id="org.lxtk.lx4e.examples.proto.editor.openTypeHierarchy"
            categoryId="org.eclipse.ui.category.navigate"
            name="Open Type Hierarchy"
            description="Open type hierachy for the selected symbol"
            defaultHandler="org.lxtk.lx4e.internal.examples.proto.editor.OpenTypeHierarchyHandler">
      </command>
   </extension>
   <extension
         point="org.eclipse.ui.bindings">
      <key
            sequence="F4"
            commandId="org.lxtk.lx4e.examples.proto.editor.openTypeHierarchy"
            contextId="org.lxtk.lx4e.examples.proto.editor.scope"
            schemeId="org.eclipse.ui.defaultAcceleratorConfiguration">
      </key>
   </extension>
```

Here is the sample code for the handler:

```java
public class OpenTypeHierarchyHandler
    extends AbstractOpenTypeHierarchyHandler
{
    @Override
    protected LanguageOperationTarget getLanguageOperationTarget(IEditorPart editor)
    {
        return ProtoOperationTargetProvider.getOperationTarget(editor);
    }

    @Override
    protected TypeHierarchyUtility getTypeHierarchyUtility(TypeHierarchyProvider provider)
    {
        return new TypeHierarchyUtility(provider,
            UriHandlers.compose(new TextDocumentUriHandler(ProtoCore.DOCUMENT_SERVICE),
                new ResourceUriHandler(), new EfsUriHandler()));
    }

    @Override
    protected String getTypeHierarchyViewId()
    {
        return "org.lxtk.lx4e.examples.proto.ProtoTypeHierarchyView";
    }
}
```
