/*******************************************************************************
 * Copyright (c) 2020, 2022 1C-Soft LLC.
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
import java.util.Map;
import java.util.Objects;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionContext;
import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartService;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.eclipse.ui.menus.IWorkbenchContribution;
import org.eclipse.ui.services.IServiceLocator;
import org.lxtk.CodeActionProvider;
import org.lxtk.CommandService;
import org.lxtk.DocumentUri;
import org.lxtk.LanguageOperationTarget;
import org.lxtk.lx4e.IWorkspaceEditChangeFactory;
import org.lxtk.lx4e.requests.CodeActionRequest;
import org.lxtk.lx4e.ui.DefaultEditorHelper;

/**
 * Partial implementation of a menu consisting of a dynamic list of
 * code actions computed using {@link CodeActionProvider}(s).
 */
public abstract class AbstractCodeActionMenu
    extends CompoundContributionItem
    implements IWorkbenchContribution
{
    private static final IContributionItem[] NO_ITEMS = new IContributionItem[0];

    private IServiceLocator serviceLocator;

    @Override
    public void initialize(IServiceLocator serviceLocator)
    {
        this.serviceLocator = serviceLocator;
    }

    /**
     * Returns the service locator for this contribution.
     *
     * @return the service locator
     * @see #initialize(IServiceLocator)
     */
    protected final IServiceLocator getServiceLocator()
    {
        return serviceLocator;
    }

    @Override
    protected IContributionItem[] getContributionItems()
    {
        LanguageOperationTarget target = getLanguageOperationTarget();
        if (target == null)
            return NO_ITEMS;

        Range range = getSelectedTextRange();
        if (range == null)
            return NO_ITEMS;

        CodeActionResults results = computeCodeActionResults(getCodeActionProviders(target),
            new CodeActionParams(DocumentUri.toTextDocumentIdentifier(target.getDocumentUri()),
                range, new CodeActionContext(Collections.emptyList(), getCodeActionKinds())));
        if (results == null)
            return NO_ITEMS;

        return getContributionItems(results);
    }

    /**
     * Returns the currently selected text range.
     *
     * @return the selected text range, or <code>null</code> if none
     */
    protected Range getSelectedTextRange()
    {
        IWorkbenchPart part = getActivePart();
        if (!(part instanceof IEditorPart))
            return null;
        return DefaultEditorHelper.INSTANCE.getSelectedTextRange((IEditorPart)part);
    }

    /**
     * Returns the active workbench part.
     *
     * @return the active workbench part, or <code>null</code> if none
     */
    protected IWorkbenchPart getActivePart()
    {
        if (serviceLocator == null)
            return null;
        IPartService partService = serviceLocator.getService(IPartService.class);
        if (partService == null)
            return null;
        return partService.getActivePart();
    }

    /**
     * Returns the current {@link LanguageOperationTarget}.
     *
     * @return the current <code>LanguageOperationTarget</code>,
     *  or <code>null</code> if none
     */
    protected abstract LanguageOperationTarget getLanguageOperationTarget();

    /**
     * Returns the workspace edit change factory for this menu.
     *
     * @return the workspace edit change factory (not <code>null</code>)
     */
    protected abstract IWorkspaceEditChangeFactory getWorkspaceEditChangeFactory();

    /**
     * Returns the kinds of code actions to request.
     *
     * @return the kinds of code actions to request,
     *  or <code>null</code> for all possible kinds
     */
    protected abstract List<String> getCodeActionKinds();

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
     * Returns contribution items for the given code action results.
     *
     * @param results never <code>null</code>
     * @return the contribution items (not <code>null</code>)
     */
    protected IContributionItem[] getContributionItems(CodeActionResults results)
    {
        List<IContributionItem> items = new ArrayList<>();

        for (Map.Entry<CodeActionProvider, CodeActionResult> entry : results.asMap().entrySet())
        {
            CodeActionResult result = entry.getValue();
            if (result != null)
            {
                List<Either<Command, CodeAction>> codeActions = result.getCodeActions();
                if (codeActions != null)
                {
                    CodeActionProvider provider = entry.getKey();

                    for (Either<Command, CodeAction> commandOrCodeAction : codeActions)
                    {
                        IAction action = null;
                        if (commandOrCodeAction.isLeft())
                            action = getAction(commandOrCodeAction.getLeft(), provider);
                        else if (commandOrCodeAction.isRight())
                            action = getAction(commandOrCodeAction.getRight(), provider);
                        if (action != null)
                            items.add(new ActionContributionItem(action));
                    }
                }
            }
        }

        return items.toArray(NO_ITEMS);
    }

    /**
     * Returns an {@link IAction} that executes the given {@link Command}.
     *
     * @param command never <code>null</code>
     * @param provider never <code>null</code>
     * @return the requested action, or <code>null</code> if none
     */
    protected IAction getAction(Command command, CodeActionProvider provider)
    {
        return new CommandAction(command, provider);
    }

    /**
     * Returns an {@link IAction} that executes the given {@link CodeAction}.
     *
     * @param codeAction never <code>null</code>
     * @param provider never <code>null</code>
     * @return the requested action, or <code>null</code> if none
     */
    protected IAction getAction(CodeAction codeAction, CodeActionProvider provider)
    {
        return new CodeActionAction(codeAction, provider);
    }

    /**
     * An action that executes a given {@link Command}.
     */
    protected class CommandAction
        extends Action
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
        public CommandAction(Command command, CodeActionProvider provider)
        {
            this.command = Objects.requireNonNull(command);
            this.commandService = provider.getCommandService();
            setText(command.getTitle());
        }

        @Override
        public void run()
        {
            CodeActions.execute(command, getText(), commandService);
        }
    }

    /**
     * An action that executes a given {@link CodeAction}.
     */
    protected class CodeActionAction
        extends Action
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
        public CodeActionAction(CodeAction codeAction, CodeActionProvider provider)
        {
            this.codeAction = Objects.requireNonNull(codeAction);
            this.provider = Objects.requireNonNull(provider);
            setText(codeAction.getTitle());
            setEnabled(codeAction.getDisabled() == null);
        }

        @Override
        public void run()
        {
            CodeActions.execute(codeAction, getText(), getWorkspaceEditChangeFactory(), provider);
        }
    }
}
