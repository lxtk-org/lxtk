/*******************************************************************************
 * Copyright (c) 2020 1C-Soft LLC.
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
package org.lxtk.lx4e.ui.refactoring.rename;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.lxtk.lx4e.refactoring.rename.RenameRefactoring;

/**
 * Refactoring wizard for {@link RenameRefactoring}.
 */
public final class RenameRefactoringWizard
    extends RefactoringWizard
{
    /**
     * Constructor.
     *
     * @param refactoring not <code>null</code>
     */
    public RenameRefactoringWizard(RenameRefactoring refactoring)
    {
        super(refactoring, DIALOG_BASED_USER_INTERFACE);
        setDefaultPageTitle(refactoring.getName());
    }

    @Override
    protected void addUserInputPages()
    {
        addPage(new RenameInputWizardPage());
    }

    private static class RenameInputWizardPage
        extends UserInputWizardPage
    {
        private Text nameText;

        RenameInputWizardPage()
        {
            super("RenamePage"); //$NON-NLS-1$
        }

        @Override
        public void createControl(Composite parent)
        {
            Composite composite = new Composite(parent, SWT.NONE);
            composite.setLayout(new GridLayout(2, false));
            composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
            setControl(composite);
            initializeDialogUnits(composite);

            Label label = new Label(composite, SWT.NONE);
            label.setLayoutData(new GridData());
            label.setText(Messages.RenameRefactoringWizard_New_name_label);

            nameText = new Text(composite, SWT.BORDER);
            String name = getRefactoring().getProposedNewName();
            if (name != null)
                nameText.setText(name);
            nameText.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, false));
            nameText.addModifyListener(e -> validatePage());
            nameText.selectAll();

            Dialog.applyDialogFont(composite);
        }

        @Override
        public void setVisible(boolean visible)
        {
            super.setVisible(visible);
            if (visible)
            {
                nameText.setFocus();
                validatePage();
            }
        }

        @Override
        protected RenameRefactoring getRefactoring()
        {
            return (RenameRefactoring)super.getRefactoring();
        }

        private void validatePage()
        {
            String newName = nameText.getText().trim();
            if (newName.isEmpty())
            {
                setPageComplete(false);
                setErrorMessage(null);
                setMessage(null);
                return;
            }

            RenameRefactoring refactoring = getRefactoring();
            if (newName.equals(refactoring.getCurrentName()))
            {
                setPageComplete(false);
                setErrorMessage(null);
                setMessage(null);
                return;
            }

            refactoring.setNewName(newName);
            setPageComplete(refactoring.checkNewName());
        }
    }
}
