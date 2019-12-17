/*******************************************************************************
 * Copyright (c) 2019 1C-Soft LLC.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.eclipse.lsp4j.ClientCapabilities;
import org.eclipse.lsp4j.DocumentFilter;
import org.eclipse.lsp4j.ExecuteCommandCapabilities;
import org.eclipse.lsp4j.ExecuteCommandOptions;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.lsp4j.ExecuteCommandRegistrationOptions;
import org.eclipse.lsp4j.Registration;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.Unregistration;
import org.eclipse.lsp4j.services.LanguageServer;
import org.lxtk.CommandService;
import org.lxtk.util.Disposable;

/**
 * TODO JavaDoc
 */
public final class ExecuteCommandFeature
    implements DynamicFeature<LanguageServer>
{
    private static final String METHOD = "workspace/executeCommand"; //$NON-NLS-1$
    private static final Set<String> METHODS = Collections.singleton(METHOD);

    private final CommandService commandService;
    private LanguageServer languageServer;
    private Map<String, Disposable> registrations;

    /**
     * TODO JavaDoc
     *
     * @param commandService not <code>null</code>
     */
    public ExecuteCommandFeature(CommandService commandService)
    {
        this.commandService = Objects.requireNonNull(commandService);
    }

    @Override
    public Set<String> getMethods()
    {
        return METHODS;
    }

    @Override
    public void fillClientCapabilities(ClientCapabilities capabilities)
    {
        ExecuteCommandCapabilities executeCommand =
            new ExecuteCommandCapabilities();
        executeCommand.setDynamicRegistration(true);
        ClientCapabilitiesUtil.getOrCreateWorkspace(
            capabilities).setExecuteCommand(executeCommand);

    }

    @Override
    public synchronized void initialize(LanguageServer server,
        ServerCapabilities capabilities, List<DocumentFilter> documentSelector)
    {
        languageServer = server;
        registrations = new HashMap<>();

        ExecuteCommandOptions options =
            capabilities.getExecuteCommandProvider();
        if (options == null)
            return;

        register(new Registration(UUID.randomUUID().toString(), METHOD,
            new ExecuteCommandRegistrationOptions(options.getCommands())));
    }

    @Override
    public synchronized void register(Registration registration)
    {
        if (!getMethods().contains(registration.getMethod()))
            throw new IllegalArgumentException();

        ExecuteCommandRegistrationOptions options =
            (ExecuteCommandRegistrationOptions)registration.getRegisterOptions();
        if (options == null)
            return;

        List<String> commands = options.getCommands();
        if (commands.isEmpty())
            return;

        if (registrations == null)
            return;

        if (registrations.containsKey(registration.getId()))
            throw new IllegalArgumentException();

        List<Disposable> registeredCommands = new ArrayList<>(commands.size());
        for (String command : commands)
        {
            registeredCommands.add(commandService.addCommand(command,
                arguments -> languageServer.getWorkspaceService().executeCommand(
                    new ExecuteCommandParams(command, arguments))));
        }

        registrations.put(registration.getId(), () -> Disposable.disposeAll(
            registeredCommands));
    }

    @Override
    public synchronized void unregister(Unregistration unregistration)
    {
        if (registrations == null)
            return;

        Disposable registration = registrations.remove(unregistration.getId());
        if (registration != null)
            registration.dispose();
    }

    @Override
    public synchronized void dispose()
    {
        if (registrations == null)
            return;
        try
        {
            Disposable.disposeAll(registrations.values());
        }
        finally
        {
            registrations = null;
        }
    }
}
