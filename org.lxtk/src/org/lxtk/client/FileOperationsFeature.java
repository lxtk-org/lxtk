/*******************************************************************************
 * Copyright (c) 2021 1C-Soft LLC.
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Vladimir Piskarev (1C) - initial API and implementation
 *******************************************************************************/
package org.lxtk.client;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import org.eclipse.lsp4j.ClientCapabilities;
import org.eclipse.lsp4j.CreateFilesParams;
import org.eclipse.lsp4j.DeleteFilesParams;
import org.eclipse.lsp4j.DocumentFilter;
import org.eclipse.lsp4j.FileCreate;
import org.eclipse.lsp4j.FileDelete;
import org.eclipse.lsp4j.FileOperationFilter;
import org.eclipse.lsp4j.FileOperationOptions;
import org.eclipse.lsp4j.FileOperationPattern;
import org.eclipse.lsp4j.FileOperationPatternKind;
import org.eclipse.lsp4j.FileOperationsServerCapabilities;
import org.eclipse.lsp4j.FileOperationsWorkspaceCapabilities;
import org.eclipse.lsp4j.FileRename;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.Registration;
import org.eclipse.lsp4j.RenameFilesParams;
import org.eclipse.lsp4j.Unregistration;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.WorkspaceServerCapabilities;
import org.eclipse.lsp4j.services.LanguageServer;
import org.lxtk.DocumentUri;
import org.lxtk.FileCreateEvent;
import org.lxtk.FileCreateEventSource;
import org.lxtk.FileDeleteEvent;
import org.lxtk.FileDeleteEventSource;
import org.lxtk.FileRenameEvent;
import org.lxtk.FileRenameEventSource;
import org.lxtk.FileWillCreateEventSource;
import org.lxtk.FileWillDeleteEventSource;
import org.lxtk.FileWillRenameEventSource;
import org.lxtk.jsonrpc.DefaultGson;
import org.lxtk.util.Disposable;
import org.lxtk.util.WaitUntilEvent;

import com.google.gson.JsonElement;

/**
 * A language client feature that supports dynamic registration for participation in workspace
 * file operations, as reported by corresponding event sources.
 * <p>
 * This implementation is thread-safe.
 * </p>
 */
public final class FileOperationsFeature
    implements DynamicFeature<LanguageServer>
{
    private static final String WILL_CREATE_FILES = "workspace/willCreateFiles"; //$NON-NLS-1$
    private static final String DID_CREATE_FILES = "workspace/didCreateFiles"; //$NON-NLS-1$
    private static final String WILL_RENAME_FILES = "workspace/willRenameFiles"; //$NON-NLS-1$
    private static final String DID_RENAME_FILES = "workspace/didRenameFiles"; //$NON-NLS-1$
    private static final String WILL_DELETE_FILES = "workspace/willDeleteFiles"; //$NON-NLS-1$
    private static final String DID_DELETE_FILES = "workspace/didDeleteFiles"; //$NON-NLS-1$
    private static final Set<String> METHODS = Set.of(WILL_CREATE_FILES, DID_CREATE_FILES,
        WILL_RENAME_FILES, DID_RENAME_FILES, WILL_DELETE_FILES, DID_DELETE_FILES);

    private final FileWillCreateEventSource fileWillCreateEventSource;
    private final FileCreateEventSource fileCreateEventSource;
    private final FileWillRenameEventSource fileWillRenameEventSource;
    private final FileRenameEventSource fileRenameEventSource;
    private final FileWillDeleteEventSource fileWillDeleteEventSource;
    private final FileDeleteEventSource fileDeleteEventSource;
    private LanguageServer languageServer;
    private Map<String, Map<String, FileOperationOptions>> registrations;
    private final Map<String, Disposable> subscriptions = new HashMap<>();

    /**
     * Convenience factory method.
     *
     * @param eventSource may be <code>null</code>
     * @return a new {@link FileOperationsFeature} (never <code>null</code>)
     */
    public static FileOperationsFeature newInstance(Object eventSource)
    {
        FileWillCreateEventSource fileWillCreateEventSource = null;
        if (eventSource instanceof FileWillCreateEventSource)
            fileWillCreateEventSource = (FileWillCreateEventSource)eventSource;

        FileCreateEventSource fileCreateEventSource = null;
        if (eventSource instanceof FileCreateEventSource)
            fileCreateEventSource = (FileCreateEventSource)eventSource;

        FileWillRenameEventSource fileWillRenameEventSource = null;
        if (eventSource instanceof FileWillRenameEventSource)
            fileWillRenameEventSource = (FileWillRenameEventSource)eventSource;

        FileRenameEventSource fileRenameEventSource = null;
        if (eventSource instanceof FileRenameEventSource)
            fileRenameEventSource = (FileRenameEventSource)eventSource;

        FileWillDeleteEventSource fileWillDeleteEventSource = null;
        if (eventSource instanceof FileWillDeleteEventSource)
            fileWillDeleteEventSource = (FileWillDeleteEventSource)eventSource;

        FileDeleteEventSource fileDeleteEventSource = null;
        if (eventSource instanceof FileDeleteEventSource)
            fileDeleteEventSource = (FileDeleteEventSource)eventSource;

        return new FileOperationsFeature(fileWillCreateEventSource, fileCreateEventSource,
            fileWillRenameEventSource, fileRenameEventSource, fileWillDeleteEventSource,
            fileDeleteEventSource);
    }

    /**
     * Constructor.
     *
     * @param fileWillCreateEventSource may be <code>null</code>
     * @param fileCreateEventSource may be <code>null</code>
     * @param fileWillRenameEventSource may be <code>null</code>
     * @param fileRenameEventSource may be <code>null</code>
     * @param fileWillDeleteEventSource may be <code>null</code>
     * @param fileDeleteEventSource may be <code>null</code>
     */
    public FileOperationsFeature(FileWillCreateEventSource fileWillCreateEventSource,
        FileCreateEventSource fileCreateEventSource,
        FileWillRenameEventSource fileWillRenameEventSource,
        FileRenameEventSource fileRenameEventSource,
        FileWillDeleteEventSource fileWillDeleteEventSource,
        FileDeleteEventSource fileDeleteEventSource)
    {
        this.fileWillCreateEventSource = fileWillCreateEventSource;
        this.fileCreateEventSource = fileCreateEventSource;
        this.fileWillRenameEventSource = fileWillRenameEventSource;
        this.fileRenameEventSource = fileRenameEventSource;
        this.fileWillDeleteEventSource = fileWillDeleteEventSource;
        this.fileDeleteEventSource = fileDeleteEventSource;
    }

    @Override
    public Set<String> getMethods()
    {
        return METHODS;
    }

    @Override
    public void fillClientCapabilities(ClientCapabilities capabilities)
    {
        FileOperationsWorkspaceCapabilities fileOperations =
            new FileOperationsWorkspaceCapabilities();

        fileOperations.setDynamicRegistration(true);

        if (fileWillCreateEventSource != null)
            fileOperations.setWillCreate(true);

        if (fileCreateEventSource != null)
            fileOperations.setDidCreate(true);

        if (fileWillRenameEventSource != null)
            fileOperations.setWillRename(true);

        if (fileRenameEventSource != null)
            fileOperations.setDidRename(true);

        if (fileWillDeleteEventSource != null)
            fileOperations.setWillDelete(true);

        if (fileDeleteEventSource != null)
            fileOperations.setDidDelete(true);

        ClientCapabilitiesUtil.getOrCreateWorkspace(capabilities).setFileOperations(fileOperations);
    }

    @Override
    public synchronized void initialize(LanguageServer server, InitializeResult initializeResult,
        List<DocumentFilter> documentSelector)
    {
        languageServer = server;
        registrations = new HashMap<>();

        WorkspaceServerCapabilities workspace = initializeResult.getCapabilities().getWorkspace();
        if (workspace == null)
            return;

        FileOperationsServerCapabilities fileOperations = workspace.getFileOperations();
        if (fileOperations == null)
            return;

        FileOperationOptions willCreateOptions = fileOperations.getWillCreate();
        if (willCreateOptions != null)
        {
            register(new Registration(UUID.randomUUID().toString(), WILL_CREATE_FILES,
                willCreateOptions));
        }

        FileOperationOptions didCreateOptions = fileOperations.getDidCreate();
        if (didCreateOptions != null)
        {
            register(
                new Registration(UUID.randomUUID().toString(), DID_CREATE_FILES, didCreateOptions));
        }

        FileOperationOptions willRenameOptions = fileOperations.getWillRename();
        if (willRenameOptions != null)
        {
            register(new Registration(UUID.randomUUID().toString(), WILL_RENAME_FILES,
                willRenameOptions));
        }

        FileOperationOptions didRenameOptions = fileOperations.getDidRename();
        if (didRenameOptions != null)
        {
            register(
                new Registration(UUID.randomUUID().toString(), DID_RENAME_FILES, didRenameOptions));
        }

        FileOperationOptions willDeleteOptions = fileOperations.getWillDelete();
        if (willDeleteOptions != null)
        {
            register(new Registration(UUID.randomUUID().toString(), WILL_DELETE_FILES,
                willDeleteOptions));
        }

        FileOperationOptions didDeleteOptions = fileOperations.getDidDelete();
        if (didDeleteOptions != null)
        {
            register(
                new Registration(UUID.randomUUID().toString(), DID_DELETE_FILES, didDeleteOptions));
        }
    }

    @Override
    public synchronized void register(Registration registration)
    {
        String registrationMethod = registration.getMethod();
        if (!METHODS.contains(registrationMethod))
            throw new IllegalArgumentException();

        Object rO = registration.getRegisterOptions();
        FileOperationOptions registrationOptions = rO instanceof JsonElement
            ? DefaultGson.INSTANCE.fromJson((JsonElement)rO, FileOperationOptions.class)
            : (FileOperationOptions)rO;
        if (registrationOptions == null)
            return;

        if (registrations == null)
            return;

        Map<String, FileOperationOptions> map = registrations.get(registrationMethod);
        if (map != null && map.containsKey(registration.getId()))
            throw new IllegalArgumentException();

        if (map == null)
        {
            map = new HashMap<>();
            registrations.put(registrationMethod, map);

            Disposable subsription = null;
            if (WILL_CREATE_FILES.equals(registrationMethod) && fileWillCreateEventSource != null)
            {
                subsription = fileWillCreateEventSource.onWillCreateFiles().subscribe(
                    this::onWillCreateFiles);
            }
            else if (DID_CREATE_FILES.equals(registrationMethod) && fileCreateEventSource != null)
            {
                subsription =
                    fileCreateEventSource.onDidCreateFiles().subscribe(this::onDidCreateFiles);
            }
            else if (WILL_RENAME_FILES.equals(registrationMethod)
                && fileWillRenameEventSource != null)
            {
                subsription = fileWillRenameEventSource.onWillRenameFiles().subscribe(
                    this::onWillRenameFiles);
            }
            else if (DID_RENAME_FILES.equals(registrationMethod) && fileRenameEventSource != null)
            {
                subsription =
                    fileRenameEventSource.onDidRenameFiles().subscribe(this::onDidRenameFiles);
            }
            else if (WILL_DELETE_FILES.equals(registrationMethod)
                && fileWillDeleteEventSource != null)
            {
                subsription = fileWillDeleteEventSource.onWillDeleteFiles().subscribe(
                    this::onWillDeleteFiles);
            }
            else if (DID_DELETE_FILES.equals(registrationMethod) && fileDeleteEventSource != null)
            {
                subsription =
                    fileDeleteEventSource.onDidDeleteFiles().subscribe(this::onDidDeleteFiles);
            }
            if (subsription != null)
                subscriptions.put(registrationMethod, subsription);
        }

        map.put(registration.getId(), registrationOptions);
    }

    @Override
    public synchronized void unregister(Unregistration unregistration)
    {
        if (registrations == null)
            return;

        Map<String, FileOperationOptions> map = registrations.get(unregistration.getMethod());
        if (map == null)
            return;

        FileOperationOptions registrationOptions = map.remove(unregistration.getId());
        if (registrationOptions == null)
            return;

        if (map.isEmpty())
        {
            registrations.remove(unregistration.getMethod());

            Disposable subscription = subscriptions.remove(unregistration.getMethod());
            if (subscription != null)
                subscription.dispose();
        }
    }

    @Override
    public synchronized void dispose()
    {
        registrations = null;
        try
        {
            Disposable.disposeAll(subscriptions.values());
        }
        finally
        {
            subscriptions.clear();
        }
    }

    private synchronized void onWillCreateFiles(
        WaitUntilEvent<FileCreateEvent, WorkspaceEdit> waitUntilEvent)
    {
        CreateFilesParams params =
            toCreateFilesParams(waitUntilEvent.get(), getFileOperationFilters(WILL_CREATE_FILES));
        if (!params.getFiles().isEmpty())
            waitUntilEvent.accept(languageServer.getWorkspaceService().willCreateFiles(params));
    }

    private synchronized void onDidCreateFiles(FileCreateEvent event)
    {
        CreateFilesParams params =
            toCreateFilesParams(event, getFileOperationFilters(DID_CREATE_FILES));
        if (!params.getFiles().isEmpty())
            languageServer.getWorkspaceService().didCreateFiles(params);
    }

    private synchronized void onWillRenameFiles(
        WaitUntilEvent<FileRenameEvent, WorkspaceEdit> waitUntilEvent)
    {
        RenameFilesParams params =
            toRenameFilesParams(waitUntilEvent.get(), getFileOperationFilters(WILL_RENAME_FILES));
        if (!params.getFiles().isEmpty())
            waitUntilEvent.accept(languageServer.getWorkspaceService().willRenameFiles(params));
    }

    private synchronized void onDidRenameFiles(FileRenameEvent event)
    {
        RenameFilesParams params =
            toRenameFilesParams(event, getFileOperationFilters(DID_RENAME_FILES));
        if (!params.getFiles().isEmpty())
            languageServer.getWorkspaceService().didRenameFiles(params);
    }

    private synchronized void onWillDeleteFiles(
        WaitUntilEvent<FileDeleteEvent, WorkspaceEdit> waitUntilEvent)
    {
        DeleteFilesParams params =
            toDeleteFilesParams(waitUntilEvent.get(), getFileOperationFilters(WILL_DELETE_FILES));
        if (!params.getFiles().isEmpty())
            waitUntilEvent.accept(languageServer.getWorkspaceService().willDeleteFiles(params));
    }

    private synchronized void onDidDeleteFiles(FileDeleteEvent event)
    {
        DeleteFilesParams params =
            toDeleteFilesParams(event, getFileOperationFilters(DID_DELETE_FILES));
        if (!params.getFiles().isEmpty())
            languageServer.getWorkspaceService().didDeleteFiles(params);
    }

    private synchronized List<FileOperationFilter> getFileOperationFilters(String method)
    {
        Map<String, FileOperationOptions> map = registrations.get(method);
        if (map == null)
            return Collections.emptyList();
        List<FileOperationFilter> result = new ArrayList<>();
        for (FileOperationOptions options : map.values())
        {
            result.addAll(options.getFilters());
        }
        return result;
    }

    private static CreateFilesParams toCreateFilesParams(FileCreateEvent event,
        List<FileOperationFilter> filters)
    {
        return new CreateFilesParams(
            event.getFiles().stream().filter(file -> isMatch(file.getUri(), filters)).map(
                file -> new FileCreate(DocumentUri.convert(file.getUri()))).collect(
                    Collectors.toList()));
    }

    private static RenameFilesParams toRenameFilesParams(FileRenameEvent event,
        List<FileOperationFilter> filters)
    {
        return new RenameFilesParams(
            event.getFiles().stream().filter(file -> isMatch(file.getOldUri(), filters)).map(
                file -> new FileRename(DocumentUri.convert(file.getOldUri()),
                    DocumentUri.convert(file.getNewUri()))).collect(Collectors.toList()));
    }

    private static DeleteFilesParams toDeleteFilesParams(FileDeleteEvent event,
        List<FileOperationFilter> filters)
    {
        return new DeleteFilesParams(
            event.getFiles().stream().filter(file -> isMatch(file.getUri(), filters)).map(
                file -> new FileDelete(DocumentUri.convert(file.getUri()))).collect(
                    Collectors.toList()));
    }

    // Note: `FileOperationPatternOptions.ignoreCase` option is currently ignored while matching
    private static boolean isMatch(URI uri, List<FileOperationFilter> filters)
    {
        Path path;
        try
        {
            path = Path.of(uri);
        }
        catch (RuntimeException e)
        {
            return false; // no match, as `uri` cannot be converted to a path
        }

        Optional<BasicFileAttributes> fileAttributes = null;

        for (FileOperationFilter filter : filters)
        {
            String scheme = filter.getScheme();
            if (scheme != null && !scheme.equals(uri.getScheme()))
                continue;

            FileOperationPattern pattern = filter.getPattern();
            PathMatcher matcher;
            try
            {
                matcher = path.getFileSystem().getPathMatcher("glob:" + pattern.getGlob()); //$NON-NLS-1$
            }
            catch (PatternSyntaxException | UnsupportedOperationException e)
            {
                continue; // ignore invalid or unknown patterns
            }

            if (!matcher.matches(path))
                continue;

            String matchKind = pattern.getMatches();
            if (matchKind == null)
                return true;

            if (fileAttributes == null)
            {
                // lazily initialize `fileAttributes`
                try
                {
                    fileAttributes =
                        Optional.of(Files.readAttributes(path, BasicFileAttributes.class));
                }
                catch (IOException e)
                {
                    fileAttributes = Optional.empty();
                }
            }

            if (fileAttributes.isEmpty()) // `path` represents a non-existing or inaccessible file
                continue;

            if ((matchKind.equals(FileOperationPatternKind.File)
                && fileAttributes.get().isRegularFile())
                || (matchKind.equals(FileOperationPatternKind.Folder)
                    && fileAttributes.get().isDirectory()))
                return true;
        }
        return false;
    }
}
