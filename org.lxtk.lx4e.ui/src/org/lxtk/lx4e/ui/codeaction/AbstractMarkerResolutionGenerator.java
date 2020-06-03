/*******************************************************************************
 * Copyright (c) 2019, 2020 1C-Soft LLC.
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
package org.lxtk.lx4e.ui.codeaction;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.eclipse.core.resources.IMarker;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionContext;
import org.eclipse.lsp4j.CodeActionKind;
import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator2;
import org.lxtk.CodeActionProvider;
import org.lxtk.CommandService;
import org.lxtk.DocumentUri;
import org.lxtk.LanguageOperationTarget;
import org.lxtk.lx4e.CodeActionRequest;
import org.lxtk.lx4e.diagnostics.DiagnosticMarkers;
import org.lxtk.lx4e.refactoring.WorkspaceEditChangeFactory;

import com.google.gson.Gson;

/**
 * Partial implementation of an {@link IMarkerResolutionGenerator2} that computes
 * resolutions for a diagnostic marker using a {@link CodeActionProvider}.
 *
 * @see DiagnosticMarkers
 */
public abstract class AbstractMarkerResolutionGenerator
    implements IMarkerResolutionGenerator2
{
    private static final IMarkerResolution[] NO_RESOLUTIONS = new IMarkerResolution[0];

    private Gson gson;

    @Override
    public boolean hasResolutions(IMarker marker)
    {
        if (marker.getAttribute(getDiagnosticAttributeName(), null) == null)
            return false;
        LanguageOperationTarget target = getLanguageOperationTarget(marker);
        if (target == null)
            return false;
        return CodeActions.hasCodeActionProvider(target);
    }

    @Override
    public IMarkerResolution[] getResolutions(IMarker marker)
    {
        Diagnostic diagnostic = getDiagnostic(marker, getDiagnosticAttributeName());
        if (diagnostic == null)
            return NO_RESOLUTIONS;

        LanguageOperationTarget target = getLanguageOperationTarget(marker);
        if (target == null)
            return NO_RESOLUTIONS;

        CodeActionProvider provider = CodeActions.getCodeActionProvider(target);
        if (provider == null)
            return NO_RESOLUTIONS;

        CodeActionRequest request = newCodeActionRequest();
        request.setProvider(provider);
        request.setParams(
            new CodeActionParams(DocumentUri.toTextDocumentIdentifier(target.getDocumentUri()),
                diagnostic.getRange(), new CodeActionContext(Collections.singletonList(diagnostic),
                    Collections.singletonList(CodeActionKind.QuickFix))));
        request.setTimeout(getCodeActionTimeout());
        request.setMayThrow(false);

        List<Either<Command, CodeAction>> result = request.sendAndReceive();
        if (result == null || result.isEmpty())
            return NO_RESOLUTIONS;

        CommandService commandService = provider.getCommandService();
        List<IMarkerResolution> resolutions = new ArrayList<>(result.size());
        for (Either<Command, CodeAction> item : result)
        {
            if (item.isLeft())
                resolutions.add(newMarkerResolution(item.getLeft(), commandService));
            else if (item.isRight())
                resolutions.add(newMarkerResolution(item.getRight(), commandService));
        }
        return resolutions.toArray(NO_RESOLUTIONS);
    }

    /**
     * Returns the corresponding {@link LanguageOperationTarget}
     * for the given marker.
     *
     * @param marker never <code>null</code>
     * @return the corresponding <code>LanguageOperationTarget</code>,
     *  or <code>null</code> if none
     */
    protected abstract LanguageOperationTarget getLanguageOperationTarget(IMarker marker);

    /**
     * Returns the {@link WorkspaceEditChangeFactory} for this generator.
     *
     * @return the <code>WorkspaceEditChangeFactory</code> (not <code>null</code>)
     */
    protected abstract WorkspaceEditChangeFactory getWorkspaceEditChangeFactory();

    /**
     * Returns the timeout for computing code actions.
     *
     * @return a positive duration
     */
    protected Duration getCodeActionTimeout()
    {
        return Duration.ofSeconds(2);
    }

    /**
     * Returns a request for computing code actions.
     *
     * @return the created request object (not <code>null</code>)
     */
    protected CodeActionRequest newCodeActionRequest()
    {
        return new CodeActionRequest();
    }

    /**
     * Creates and returns a marker resolution that executes the given
     * {@link Command}.
     *
     * @param command never <code>null</code>
     * @param commandService never <code>null</code>
     * @return the created resolution (not <code>null</code>)
     */
    protected IMarkerResolution newMarkerResolution(Command command, CommandService commandService)
    {
        return new CommandMarkerResolution(command, commandService);
    }

    /**
     * Creates and returns a marker resolution that executes the given
     * {@link CodeAction}.
     *
     * @param codeAction never <code>null</code>
     * @param commandService never <code>null</code>
     * @return the created resolution (not <code>null</code>)
     */
    protected IMarkerResolution newMarkerResolution(CodeAction codeAction,
        CommandService commandService)
    {
        return new CodeActionMarkerResolution(codeAction, commandService);
    }

    /**
     * Returns the name of the marker attribute that contains a diagnostic.
     *
     * @return the diagnostic attribute name (not <code>null</code>)
     */
    protected String getDiagnosticAttributeName()
    {
        return DiagnosticMarkers.DIAGNOSTIC_ATTRIBUTE;
    }

    /**
     * Returns the diagnostic contained in the given marker attribute.
     *
     * @param marker never <code>null</code>
     * @param diagnosticAttributeName never <code>null</code>
     * @return the requested diagnostic, or <code>null</code> if none
     */
    protected Diagnostic getDiagnostic(IMarker marker, String diagnosticAttributeName)
    {
        String value = marker.getAttribute(diagnosticAttributeName, null);
        if (value == null)
            return null;
        if (gson == null)
            gson = new Gson();
        return gson.fromJson(value, Diagnostic.class);
    }

    /**
     * A marker resolution that executes a given {@link Command}.
     */
    protected class CommandMarkerResolution
        implements IMarkerResolution
    {
        /**
         * The associated {@link Command} object (never <code>null</code>).
         */
        protected final Command command;

        private final CommandService commandService;

        /**
         * Constructor.
         *
         * @param command not <code>null</code>
         * @param commandService not <code>null</code>
         */
        public CommandMarkerResolution(Command command, CommandService commandService)
        {
            this.command = Objects.requireNonNull(command);
            this.commandService = Objects.requireNonNull(commandService);
        }

        @Override
        public String getLabel()
        {
            return command.getTitle();
        }

        @Override
        public void run(IMarker marker)
        {
            CodeActions.execute(command, getLabel(), commandService);
        }
    }

    /**
     * A marker resolution that executes a given {@link CodeAction}.
     */
    protected class CodeActionMarkerResolution
        implements IMarkerResolution
    {
        /**
         * The associated {@link CodeAction} object (never <code>null</code>).
         */
        protected final CodeAction codeAction;

        private final CommandService commandService;

        /**
         * Constructor.
         *
         * @param codeAction not <code>null</code>
         * @param commandService not <code>null</code>
         */
        public CodeActionMarkerResolution(CodeAction codeAction, CommandService commandService)
        {
            this.codeAction = Objects.requireNonNull(codeAction);
            this.commandService = Objects.requireNonNull(commandService);
        }

        @Override
        public String getLabel()
        {
            return codeAction.getTitle();
        }

        @Override
        public void run(IMarker marker)
        {
            CodeActions.execute(codeAction, getLabel(), getWorkspaceEditChangeFactory(),
                commandService);
        }
    }
}
