# LXTK/LX4E FAQ

## Call Hierarchy

### How do I add support for a [call hierarchy](https://microsoft.github.io/language-server-protocol/specifications/specification-current/#textDocument_prepareCallHierarchy)?

First, you need to add a `org.lxtk.client.CallHierarchyFeature` to the list of
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
        features.add(new CallHierarchyFeature(LANGUAGE_SERVICE));
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

Then, you need to contribute a call hierarchy view through an extension in
`plugin.xml` for the plug-in:

```xml
   <extension
         point="org.eclipse.ui.views">
      <view
            id="org.lxtk.lx4e.examples.proto.ProtoCallHierarchyView"
            name="Proto Call Hierarchy"
            class="org.lxtk.lx4e.internal.examples.proto.callhierarchy.ProtoCallHierarchyView"
            allowMultiple="true">
      </view>
   </extension>
```

Here is the sample code for the view and its manager:

```java
public class ProtoCallHierarchyView
    extends AbstractCallHierarchyView
{
    public static final String ID = "org.lxtk.lx4e.examples.proto.ProtoCallHierarchyView";

    @Override
    protected CallHierarchyViewManager getViewManager()
    {
        return ProtoCallHierarchyViewManager.INSTANCE;
    }
}

public class ProtoCallHierarchyViewManager
    extends CallHierarchyViewManager
{
    public static final ProtoCallHierarchyViewManager INSTANCE =
        new ProtoCallHierarchyViewManager();

    private ProtoCallHierarchyViewManager()
    {
    }
}
```

(Make sure to require the `org.eclipse.handly.ui` bundle in `MANIFEST.MF`
for the plug-in.)

Finally, you need to contribute a command handler that opens a call hierarchy
for the current selection in the editor:

```xml
   <extension
         point="org.eclipse.ui.commands">
      <command
            id="org.lxtk.lx4e.examples.proto.editor.openCallHierarchy"
            categoryId="org.eclipse.ui.category.navigate"
            name="Open Call Hierarchy"
            description="Open call hierachy for the selected symbol"
            defaultHandler="org.lxtk.lx4e.internal.examples.proto.editor.OpenCallHierarchyHandler">
      </command>
   </extension>
   <extension
         point="org.eclipse.ui.bindings">
      <key
            sequence="M3+M4+H"
            commandId="org.lxtk.lx4e.examples.proto.editor.openCallHierarchy"
            contextId="org.lxtk.lx4e.examples.proto.editor.scope"
            schemeId="org.eclipse.ui.defaultAcceleratorConfiguration">
      </key>
   </extension>
```

Here is the sample code for the handler:

```java
public class OpenCallHierarchyHandler
    extends AbstractOpenCallHierarchyHandler
{
    @Override
    protected LanguageOperationTarget getLanguageOperationTarget(IEditorPart editor)
    {
        return ProtoOperationTargetProvider.getOperationTarget(editor);
    }

    @Override
    protected CallHierarchyUtility getCallHierarchyUtility(CallHierarchyProvider provider)
    {
        return new CallHierarchyUtility(provider,
            UriHandlers.compose(new TextDocumentUriHandler(ProtoCore.DOCUMENT_SERVICE),
                new ResourceUriHandler(), new EfsUriHandler()));
    }

    @Override
    protected CallHierarchyViewOpener getCallHierarchyViewOpener()
    {
        return new CallHierarchyViewOpener(ProtoCallHierarchyView.ID,
            ProtoCallHierarchyViewManager.INSTANCE);
    }
}
```

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

## File Operations

### How do I add support for file operation events?

Basically, you need to add a `org.lxtk.client.FileOperationsFeature` to the
list of features of your language client. This feature supports sending the
following file operation events to the language server:

- [WillCreateFiles](https://microsoft.github.io/language-server-protocol/specifications/specification-current/#workspace_willCreateFiles)
and [DidCreateFiles](https://microsoft.github.io/language-server-protocol/specifications/specification-current/#workspace_didCreateFiles)
- [WillDeleteFiles](https://microsoft.github.io/language-server-protocol/specifications/specification-current/#workspace_willDeleteFiles)
and [DidDeleteFiles](https://microsoft.github.io/language-server-protocol/specifications/specification-current/#workspace_didDeleteFiles)
- [WillRenameFiles](https://microsoft.github.io/language-server-protocol/specifications/specification-current/#workspace_willRenameFiles)
and [DidRenameFiles](https://microsoft.github.io/language-server-protocol/specifications/specification-current/#workspace_didRenameFiles)

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
        features.add(FileOperationsFeature.newInstance(
            Activator.getDefault().getFileOperationParticipantSupport()));
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

Note that you need to pass an instance of `org.lxtk.lx4e.refactoring.FileOperationParticipantSupport`
to the factory method `newInstance` of the `FileOperationsFeature`. This instance
is created by the activator for the `org.lxtk.lx4e.examples.proto` plug-in:

```java
public class Activator
    extends AbstractUIPlugin
{
    ...
    private WorkspaceEditChangeFactory changeFactory;
    private FileOperationParticipantSupport participantSupport;
    ...
    public WorkspaceEditChangeFactory getWorkspaceEditChangeFactory()
    {
        return changeFactory;
    }

    public FileOperationParticipantSupport getFileOperationParticipantSupport()
    {
        return participantSupport;
    }
    ...
    @Override
    public void start(BundleContext context) throws Exception
    {
        ...
        changeFactory = new WorkspaceEditChangeFactory(DOCUMENT_SERVICE);
        participantSupport = new FileOperationParticipantSupport(changeFactory);
        changeFactory.setFileOperationParticipantSupport(participantSupport);
        ...
    }
    ...
}
```

Finally, you need to contribute a refactoring participant for each file operation
through an extension in `plugin.xml` for the plug-in:

```xml
   <extension
         point="org.eclipse.ltk.core.refactoring.renameParticipants">
      <renameParticipant
            id="org.lxtk.lx4e.examples.proto.renameResourceParticipant"
            name="Proto Rename Resource Participant"
            class="org.lxtk.lx4e.internal.examples.proto.ProtoRenameResourceParticipant">
         <enablement>
            <adapt
                  type="org.eclipse.core.resources.IResource">
            </adapt>
         </enablement>
      </renameParticipant>
   </extension>
   <extension
         point="org.eclipse.ltk.core.refactoring.moveParticipants">
      <moveParticipant
            id="org.lxtk.lx4e.examples.proto.moveResourceParticipant"
            name="Proto Move Resource Participant"
            class="org.lxtk.lx4e.internal.examples.proto.ProtoMoveResourceParticipant">
         <enablement>
            <adapt
                  type="org.eclipse.core.resources.IResource">
            </adapt>
         </enablement>
      </moveParticipant>
   </extension>
   <extension
         point="org.eclipse.ltk.core.refactoring.deleteParticipants">
      <deleteParticipant
            id="org.lxtk.lx4e.examples.proto.deleteResourceParticipant"
            name="Proto Delete Resource Participant"
            class="org.lxtk.lx4e.internal.examples.proto.ProtoDeleteResourceParticipant">
         <enablement>
            <adapt
                  type="org.eclipse.core.resources.IResource">
            </adapt>
         </enablement>
      </deleteParticipant>
   </extension>
```

Here is the sample code for the participants:

```java
public class ProtoRenameResourceParticipant
    extends RenameResourceParticipant
{
    @Override
    public String getName()
    {
        return "Proto Rename Resource Participant";
    }

    @Override
    protected IFileOperationParticipantSupport getFileOperationParticipantSupport()
    {
        return Activator.getDefault().getFileOperationParticipantSupport();
    }
}

public class ProtoMoveResourceParticipant
    extends MoveResourceParticipant
{
    @Override
    public String getName()
    {
        return "Proto Move Resource Participant";
    }

    @Override
    protected IFileOperationParticipantSupport getFileOperationParticipantSupport()
    {
        return Activator.getDefault().getFileOperationParticipantSupport();
    }
}

public class ProtoDeleteResourceParticipant
    extends DeleteResourceParticipant
{
    @Override
    public String getName()
    {
        return "Proto Delete Resource Participant";
    }

    @Override
    protected IFileOperationParticipantSupport getFileOperationParticipantSupport()
    {
        return Activator.getDefault().getFileOperationParticipantSupport();
    }
}
```

Note that support for `createParticipants` and `copyParticipants` is currently
limited, since they don't get called by the base Eclipse Platform. However,
they do get called by the Eclipse JDT for some of its refactorings. See
<https://github.com/lxtk-org/lxtk/discussions/32> for more information.

## Semantic Tokens

### How do I add support for [semantic tokens](https://microsoft.github.io/language-server-protocol/specifications/specification-current/#textDocument_semanticTokens)?

First, you need to add a `org.lxtk.client.DocumentSemanticTokensFeature` to
the list of features of your language client.

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
        features.add(new DocumentSemanticTokensFeature(LANGUAGE_SERVICE));
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

Then, you need to subclass `org.lxtk.lx4e.ui.tokens.PresentationDamagerRepairer`
and implement the `getTokenTextAttribute(Token)` method. For example:

```java
public class ProtoDamagerRepairer
    extends PresentationDamagerRepairer
{
    public ProtoDamagerRepairer(Supplier<LanguageOperationTarget> targetSupplier)
    {
        super(targetSupplier);
    }

    @Override
    protected TextAttribute getTokenTextAttribute(Token token)
    {
        int style = SWT.NORMAL;
        Set<String> modifiers = token.getModifiers();
        if (modifiers.contains("bold"))
            style |= SWT.BOLD;
        if (modifiers.contains("italic"))
            style |= SWT.ITALIC;
        return new TextAttribute(null, null, style, null);
    }
}
```

Finally, an instance of this class needs to be set as both damager and repairer
of the presentation reconciler for the source viewer of your editor:

```java
public class ProtoSourceViewerConfiguration
    extends TextSourceViewerConfiguration
{
    ...
    @Override
    public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer)
    {
        PresentationReconciler reconciler = new PresentationReconciler();
        reconciler.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));
        ProtoDamagerRepairer damagerRepairer =
            new ProtoDamagerRepairer(this::getLanguageOperationTarget);
        reconciler.setDamager(damagerRepairer, IDocument.DEFAULT_CONTENT_TYPE);
        reconciler.setRepairer(damagerRepairer, IDocument.DEFAULT_CONTENT_TYPE);
        return reconciler;
    }
    ...
}
```

### How do I add support for refreshing semantic tokens?

Your editor needs to create and use an instance of `org.lxtk.lx4e.ui.tokens.SemanticTokensRefreshSupport`.

Using `org.lxtk.lx4e.examples.proto` as an example:

```java
public class ProtoEditor
    extends AbstractDecoratedTextEditor
{
    private SemanticTokensRefreshSupport tokensRefreshSupport;
    ...
    @Override
    public void createPartControl(Composite parent)
    {
        super.createPartControl(parent);
        tokensRefreshSupport = new SemanticTokensRefreshSupport(getSourceViewer());
        tokensRefreshSupport.setTarget(ProtoOperationTargetProvider.getOperationTarget(this));
    }

    @Override
    public void dispose()
    {
        super.dispose();
        if (tokensRefreshSupport != null)
            tokensRefreshSupport.dispose();
    }

    @Override
    protected void doSetInput(IEditorInput input) throws CoreException
    {
        super.doSetInput(input);
        if (tokensRefreshSupport != null)
            tokensRefreshSupport.setTarget(ProtoOperationTargetProvider.getOperationTarget(this));
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
