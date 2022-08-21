/*******************************************************************************
 * Copyright (c) 2019, 2022 1C-Soft LLC.
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Vladimir Piskarev (1C) - initial API and implementation
 *     Alexander Kozinko (1C)
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
import org.eclipse.lsp4j.CodeActionTriggerKind;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator2;
import org.lxtk.CodeActionProvider;
import org.lxtk.CommandService;
import org.lxtk.DocumentUri;
import org.lxtk.LanguageOperationTarget;
import org.lxtk.jsonrpc.DefaultGson;
import org.lxtk.lx4e.IWorkspaceEditChangeFactory;
import org.lxtk.lx4e.diagnostics.DiagnosticMarkers;
import org.lxtk.lx4e.requests.CodeActionRequest;

/**
 * Partial implementation of an {@link IMarkerResolutionGenerator2} that computes
 * resolutions for a diagnostic marker using {@link CodeActionProvider}(s).
 *
 * @see DiagnosticMarkers
 */
public abstract class AbstractMarkerResolutionGenerator
    implements IMarkerResolutionGenerator2
{
    private static final IMarkerResolution[] NO_RESOLUTIONS = new IMarkerResolution[0];

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

        CodeActionContext context = new CodeActionContext(Collections.singletonList(diagnostic),
            Collections.singletonList(CodeActionKind.QuickFix));
        context.setTriggerKind(CodeActionTriggerKind.Invoked);

        CodeActionResults results = computeCodeActionResults(getCodeActionProviders(target),
            new CodeActionParams(DocumentUri.toTextDocumentIdentifier(target.getDocumentUri()),
                diagnostic.getRange(), context));
        if (results == null)
            return NO_RESOLUTIONS;

        return getResolutions(results, marker);
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
     * Returns the workspace edit change factory for this generator.
     *
     * @return the workspace edit change factory (not <code>null</code>)
     */
    protected abstract IWorkspaceEditChangeFactory getWorkspaceEditChangeFactory();

    /**
     * Returns the timeout for a code action request.
     *
     * @return a positive duration
     */
    protected Duration getCodeActionTimeout()
    {
        return Duration.ofSeconds(2);
    }

    /**
     * Returns a new instance of {@link CodeActionRequest}.
     *
     * @return the created request object (not <code>null</code>)
     */
    protected CodeActionRequest newCodeActionRequest()
    {
        return new CodeActionRequest();
    }

    /**
     * Returns code action providers that match the given target.
     *
     * @param target never <code>null</code>
     * @return the matching code action providers (not <code>null</code>)
     */
    protected CodeActionProvider[] getCodeActionProviders(LanguageOperationTarget target)
    {
        return CodeActions.getCodeActionProviders(target);
    }

    /**
     * Asks each of the given code action providers to compute a result for the given
     * {@link CodeActionParams} and returns the computed results.
     *
     * @param providers never <code>null</code>
     * @param params never <code>null</code>
     * @return the computed code action results, or <code>null</code> if none
     */
    protected CodeActionResults computeCodeActionResults(CodeActionProvider[] providers,
        CodeActionParams params)
    {
        return CodeActions.computeCodeActionResults(providers, params, this::newCodeActionRequest,
            getCodeActionTimeout());
    }

    /**
     * Returns marker resolutions for the given code action results computed for the given marker.
     *
     * @param results never <code>null</code>
     * @param marker never <code>null</code>
     * @return the marker resolutions (not <code>null</code>)
     */
    protected IMarkerResolution[] getResolutions(CodeActionResults results, IMarker marker)
    {
        List<IMarkerResolution> resolutions = new ArrayList<>();

        results.asMap().forEach((provider, result) ->
        {
            if (result != null)
            {
                List<Either<Command, CodeAction>> codeActions = result.getCodeActions();
                if (codeActions != null)
                {
                    for (Either<Command, CodeAction> commandOrCodeAction : codeActions)
                    {
                        if (commandOrCodeAction.isLeft())
                            resolutions.add(
                                newMarkerResolution(commandOrCodeAction.getLeft(), provider));
                        else if (commandOrCodeAction.isRight())
                        {
                            CodeAction codeAction = commandOrCodeAction.getRight();
                            if (codeAction.getDisabled() == null)
                                resolutions.add(newMarkerResolution(codeAction, provider));
                        }
                    }
                }
            }
        });

        return resolutions.toArray(NO_RESOLUTIONS);
    }

    /**
     * Creates and returns a marker resolution that executes the given
     * {@link Command}.
     *
     * @param command never <code>null</code>
     * @param provider never <code>null</code>
     * @return the created resolution (not <code>null</code>)
     */
    protected IMarkerResolution newMarkerResolution(Command command, CodeActionProvider provider)
    {
        return new CommandMarkerResolution(command, provider);
    }

    /**
     * Creates and returns a marker resolution that executes the given
     * {@link CodeAction}.
     *
     * @param codeAction never <code>null</code>
     * @param provider never <code>null</code>
     * @return the created resolution (not <code>null</code>)
     */
    protected IMarkerResolution newMarkerResolution(CodeAction codeAction,
        CodeActionProvider provider)
    {
        return new CodeActionMarkerResolution(codeAction, provider);
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
        return DefaultGson.INSTANCE.fromJson(value, Diagnostic.class);
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
         * @param provider not <code>null</code>
         */
        public CommandMarkerResolution(Command command, CodeActionProvider provider)
        {
            this.command = Objects.requireNonNull(command);
            this.commandService = provider.getCommandService();
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

        private final CodeActionProvider provider;

        /**
         * Constructor.
         *
         * @param codeAction not <code>null</code>
         * @param provider not <code>null</code>
         */
        public CodeActionMarkerResolution(CodeAction codeAction, CodeActionProvider provider)
        {
            this.codeAction = Objects.requireNonNull(codeAction);
            this.provider = Objects.requireNonNull(provider);
        }

        @Override
        public String getLabel()
        {
            return codeAction.getTitle();
        }

        @Override
        public void run(IMarker marker)
        {
            CodeActions.execute(codeAction, getLabel(), getWorkspaceEditChangeFactory(), provider);
        }
    }
}
