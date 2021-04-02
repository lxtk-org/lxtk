/*******************************************************************************
 * Copyright (c) 2020, 2021 1C-Soft LLC.
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
import org.lxtk.lx4e.CodeActionRequest;
import org.lxtk.lx4e.refactoring.WorkspaceEditChangeFactory;
import org.lxtk.lx4e.ui.DefaultEditorHelper;
import org.lxtk.lx4e.ui.WorkDoneProgressFactory;

/**
 * Partial implementation of a menu consisting of a dynamic list of
 * code actions computed using a {@link CodeActionProvider}.
 */
public abstract class AbstractCodeActionMenu
    extends CompoundContributionItem
    implements IWorkbenchContribution
{
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
            return noItems();

        Range range = getSelectedTextRange();
        if (range == null)
            return noItems();

        CodeActionProvider provider = CodeActions.getCodeActionProvider(target);
        if (provider == null)
            return noItems();

        CodeActionRequest request = newCodeActionRequest();
        request.setProvider(provider);
        request.setParams(
            new CodeActionParams(DocumentUri.toTextDocumentIdentifier(target.getDocumentUri()),
                range, new CodeActionContext(Collections.emptyList(), getCodeActionKinds())));
        request.setTimeout(getCodeActionTimeout());
        request.setMayThrow(false);
        request.setUpWorkDoneProgress(
            () -> WorkDoneProgressFactory.newWorkDoneProgressWithJob(false));

        List<Either<Command, CodeAction>> result = request.sendAndReceive();
        if (result == null || result.isEmpty())
            return noItems();

        List<IContributionItem> items = new ArrayList<>(result.size());
        for (Either<Command, CodeAction> item : result)
        {
            IAction action = null;
            if (item.isLeft())
                action = getAction(item.getLeft(), provider);
            else if (item.isRight())
                action = getAction(item.getRight(), provider);
            if (action != null)
                items.add(new ActionContributionItem(action));
        }
        return items.toArray(new IContributionItem[items.size()]);
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
     * Returns the {@link WorkspaceEditChangeFactory} for this menu.
     *
     * @return the <code>WorkspaceEditChangeFactory</code> (not <code>null</code>)
     */
    protected abstract WorkspaceEditChangeFactory getWorkspaceEditChangeFactory();

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
     * Returns the text to be set for the "no actions available" item.
     *
     * @return the requested text (not <code>null</code>)
     */
    protected String getNoActionsText()
    {
        return Messages.AbstractCodeActionMenu_noActions;
    }

    private IContributionItem[] noItems()
    {
        return new IContributionItem[] { new ActionContributionItem(new NoActionsAction()) };
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

    private class NoActionsAction
        extends Action
    {
        NoActionsAction()
        {
            setText(getNoActionsText());
            setEnabled(false);
        }
    }
}
