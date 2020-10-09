/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Vladimir Piskarev (1C) - adaptation (adapted from
 *          org.eclipse.jdt.internal.ui.text.java.hover.AbstractAnnotationHover)
 *******************************************************************************/
package org.lxtk.lx4e.ui.hover;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.JFacePreferences;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.AbstractInformationControl;
import org.eclipse.jface.text.AbstractReusableInformationControlCreator;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IInformationControlExtension2;
import org.eclipse.jface.text.IInformationControlExtension4;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.IRewriteTarget;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextHoverExtension;
import org.eclipse.jface.text.ITextHoverExtension2;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITextViewerExtension;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension2;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelExtension2;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.texteditor.AnnotationPreference;
import org.eclipse.ui.texteditor.DefaultMarkerAnnotationAccess;
import org.lxtk.lx4e.internal.ui.Activator;
import org.lxtk.lx4e.util.DefaultWordFinder;

/**
 * Default implementation of a hover that shows description of the selected annotation.
 */
//@formatter:off
public class AnnotationHover implements ITextHover, ITextHoverExtension, ITextHoverExtension2 {

	/**
	 * An annotation info contains information about an {@link Annotation}.
	 * It's used as input for the annotation information control.
	 */
	protected static class AnnotationInfo {
		public final Annotation annotation;
		public final Position position;
		public final ITextViewer viewer;

		/**
		 * Constructor.
		 *
		 * @param annotation not <code>null</code>
		 * @param position not <code>null</code>
		 * @param textViewer not <code>null</code>
		 */
		public AnnotationInfo(Annotation annotation, Position position, ITextViewer textViewer) {
			this.annotation= Objects.requireNonNull(annotation);
			this.position= Objects.requireNonNull(position);
			this.viewer= Objects.requireNonNull(textViewer);
		}

		/**
		 * Create completion proposals which can resolve the given annotation at
		 * the given position.
		 *
		 * @return an array of completion proposals or <code>null</code>
		 *  if no proposals are available
		 */
		public ICompletionProposal[] getCompletionProposals() {
			return null;
		}

		/**
		 * Adds actions to the given toolbar.
		 *
		 * @param manager the toolbar manager to add actions to (not <code>null</code>)
		 * @param infoControl the information control (not <code>null</code>)
		 */
		public void fillToolBar(ToolBarManager manager, IInformationControl infoControl) {
			ConfigureAnnotationsAction configureAnnotationsAction= new ConfigureAnnotationsAction(annotation, infoControl);
			manager.add(configureAnnotationsAction);
		}
	}

	/**
	 * The annotation information control shows informations about a given
	 * {@link AnnotationHover.AnnotationInfo}. It can also show a toolbar
	 * and a list of {@link ICompletionProposal}s.
	 */
	private static class AnnotationInformationControl extends AbstractInformationControl implements IInformationControlExtension2 {

		private final DefaultMarkerAnnotationAccess fMarkerAnnotationAccess;
		private Control fFocusControl;
		private AnnotationInfo fInput;
		private Composite fParent;

		public AnnotationInformationControl(Shell parentShell, String statusFieldText) {
			super(parentShell, statusFieldText);

			fMarkerAnnotationAccess= new DefaultMarkerAnnotationAccess();
			create();
		}

		public AnnotationInformationControl(Shell parentShell, ToolBarManager toolBarManager) {
			super(parentShell, toolBarManager);

			fMarkerAnnotationAccess= new DefaultMarkerAnnotationAccess();
			create();
		}

		@Override
		public void setInformation(String information) {
			//replaced by IInformationControlExtension2#setInput
		}

		@Override
		public void setInput(Object input) {
			Assert.isLegal(input instanceof AnnotationInfo);
			fInput= (AnnotationInfo)input;
			disposeDeferredCreatedContent();
			deferredCreateContent();
		}

		@Override
		public boolean hasContents() {
			return fInput != null;
		}

		private AnnotationInfo getAnnotationInfo() {
			return fInput;
		}

		@Override
		public void setFocus() {
			super.setFocus();
			if (fFocusControl != null)
				fFocusControl.setFocus();
		}

		@Override
		public final void setVisible(boolean visible) {
			if (!visible)
				disposeDeferredCreatedContent();
			super.setVisible(visible);
		}

		protected void disposeDeferredCreatedContent() {
			for (Control child : fParent.getChildren()) {
				child.dispose();
			}
			ToolBarManager toolBarManager= getToolBarManager();
			if (toolBarManager != null)
				toolBarManager.removeAll();
		}

		@Override
		protected void createContent(Composite parent) {
			fParent= parent;
			GridLayout layout= new GridLayout(1, false);
			layout.verticalSpacing= 0;
			layout.marginWidth= 0;
			layout.marginHeight= 0;
			fParent.setLayout(layout);
		}

		@Override
		public Point computeSizeHint() {
			Point preferedSize= getShell().computeSize(SWT.DEFAULT, SWT.DEFAULT, true);

			Point constrains= getSizeConstraints();
			if (constrains == null)
				return preferedSize;

			int trimWidth= getShell().computeTrim(0, 0, 0, 0).width;
			Point constrainedSize= getShell().computeSize(constrains.x - trimWidth, SWT.DEFAULT, true);

			// get minimum needed width within constrained size including the trim and add a little extra
			// to ensure we don't get unnecessary wrapping of the text (4 is minimum for Windows)
			int width= Math.min(preferedSize.x + trimWidth + 4, constrainedSize.x);
			int height= Math.max(preferedSize.y, constrainedSize.y);

			return new Point(width, height);
		}

		/**
		 * Fills the toolbar actions, if a toolbar is available. This
		 * is called after the input has been set.
		 */
		protected void fillToolbar() {
			ToolBarManager toolBarManager= getToolBarManager();
			if (toolBarManager == null)
				return;
			fInput.fillToolBar(toolBarManager, this);
			toolBarManager.update(true);
		}

		/**
		 * Create content of the hover. This is called after
		 * the input has been set.
		 */
		protected void deferredCreateContent() {
			fillToolbar();

			createAnnotationInformation(fParent, getAnnotationInfo().annotation);

			ColorRegistry colorRegistry= JFaceResources.getColorRegistry();
			Color foreground= colorRegistry.get(JFacePreferences.INFORMATION_FOREGROUND_COLOR);
			if (foreground == null) {
				foreground= fParent.getForeground();
			}
			Color background= colorRegistry.get(JFacePreferences.INFORMATION_BACKGROUND_COLOR);
			if (background == null) {
				background= fParent.getBackground();
			}

			setForegroundColor(foreground); // For main composite.
			setBackgroundColor(background);
			setColorAndFont(fParent, foreground, background, JFaceResources.getDialogFont()); // For child elements.

			ICompletionProposal[] proposals= getAnnotationInfo().getCompletionProposals();
			if (proposals != null && proposals.length > 0)
				createCompletionProposalsControl(fParent, proposals);

			fParent.layout(true);
		}

		private void setColorAndFont(Control control, Color foreground, Color background, Font font) {
			control.setForeground(foreground);
			control.setBackground(background);
			control.setFont(font);

			if (control instanceof Composite) {
				for (Control child : ((Composite) control).getChildren()) {
					setColorAndFont(child, foreground, background, font);
				}
			}
		}

		private void createAnnotationInformation(Composite parent, final Annotation annotation) {
			Composite composite= new Composite(parent, SWT.NONE);
			composite.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
			GridLayout layout= new GridLayout(2, false);
			layout.marginHeight= 2;
			layout.marginWidth= 2;
			layout.horizontalSpacing= 0;
			composite.setLayout(layout);

			final Canvas canvas= new Canvas(composite, SWT.NO_FOCUS);
			GridData gridData= new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false);
			gridData.widthHint= 17;
			gridData.heightHint= 16;
			canvas.setLayoutData(gridData);
			canvas.addPaintListener(e -> {
				e.gc.setFont(null);
				fMarkerAnnotationAccess.paint(annotation, e.gc, canvas, new Rectangle(0, 0, 16, 16));
			});

			StyledText text= new StyledText(composite, SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI | SWT.WRAP | SWT.READ_ONLY);
			GridData data= new GridData(SWT.FILL, SWT.FILL, true, true);
			text.setLayoutData(data);
			text.setAlwaysShowScrollBars(false);
			String annotationText= annotation.getText();
			if (annotationText != null)
				text.setText(annotationText);
		}

		private void createCompletionProposalsControl(Composite parent, ICompletionProposal[] proposals) {
			Composite composite= new Composite(parent, SWT.NONE);
			composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			GridLayout layout2= new GridLayout(1, false);
			layout2.marginHeight= 0;
			layout2.marginWidth= 0;
			layout2.verticalSpacing= 2;
			composite.setLayout(layout2);

			Label separator= new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
			GridData gridData= new GridData(SWT.FILL, SWT.CENTER, true, false);
			separator.setLayoutData(gridData);

			Label quickFixLabel= new Label(composite, SWT.NONE);
			GridData layoutData= new GridData(SWT.BEGINNING, SWT.CENTER, false, false);
			layoutData.horizontalIndent= 4;
			quickFixLabel.setLayoutData(layoutData);
			String text;
			if (proposals.length == 1) {
				text= Messages.AnnotationHover_Single_quick_fix;
			} else {
				text= MessageFormat.format(Messages.AnnotationHover_Multiple_quick_fixes, proposals.length);
			}
			quickFixLabel.setText(text);

			setColorAndFont(composite, parent.getForeground(), parent.getBackground(), JFaceResources.getDialogFont());
			createCompletionProposalsList(composite, proposals);
		}

		private void createCompletionProposalsList(Composite parent, ICompletionProposal[] proposals) {
			final ScrolledComposite scrolledComposite= new ScrolledComposite(parent, SWT.V_SCROLL | SWT.H_SCROLL);
			GridData gridData= new GridData(SWT.FILL, SWT.FILL, true, true);
			scrolledComposite.setLayoutData(gridData);
			scrolledComposite.setExpandVertical(false);
			scrolledComposite.setExpandHorizontal(false);

			Composite composite= new Composite(scrolledComposite, SWT.NONE);
			composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			GridLayout layout= new GridLayout(2, false);
			layout.marginLeft= 5;
			layout.verticalSpacing= 2;
			composite.setLayout(layout);

			List<Link> list= new ArrayList<>();
			for (ICompletionProposal prop : proposals) {
				list.add(createCompletionProposalLink(composite, prop, 1)); // Original link for single fix, hence pass 1 for count
//				if (prop instanceof FixCorrectionProposal) {
//					FixCorrectionProposal proposal= (FixCorrectionProposal) prop;
//					int count= proposal.computeNumberOfFixesForCleanUp(proposal.getCleanUp());
//					if (count > 1) {
//						list.add(createCompletionProposalLink(composite, prop, count));
//					}
//				}
			}
			final Link[] links= list.toArray(new Link[list.size()]);

			scrolledComposite.setContent(composite);
			setColorAndFont(scrolledComposite, parent.getForeground(), parent.getBackground(), JFaceResources.getDialogFont());

			Point contentSize= composite.computeSize(SWT.DEFAULT, SWT.DEFAULT);
			composite.setSize(contentSize);

			Point constraints= getSizeConstraints();
			if (constraints != null && contentSize.x < constraints.x) {
				ScrollBar horizontalBar= scrolledComposite.getHorizontalBar();

				int scrollBarHeight;
				if (horizontalBar == null) {
					Point scrollSize= scrolledComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT);
					scrollBarHeight= scrollSize.y - contentSize.y;
				} else {
					scrollBarHeight= horizontalBar.getSize().y;
				}
				gridData.heightHint= contentSize.y - scrollBarHeight;
			}

			fFocusControl= links[0];
			for (int i= 0; i < links.length; i++) {
				final int index= i;
				final Link link= links[index];
				link.addKeyListener(new KeyListener() {
					@Override
					public void keyPressed(KeyEvent e) {
						switch (e.keyCode) {
							case SWT.ARROW_DOWN:
								if (index + 1 < links.length) {
									links[index + 1].setFocus();
								}
								break;
							case SWT.ARROW_UP:
								if (index > 0) {
									links[index - 1].setFocus();
								}
								break;
							default:
								break;
						}
					}

					@Override
					public void keyReleased(KeyEvent e) {
					}
				});

				link.addFocusListener(new FocusListener() {
					@Override
					public void focusGained(FocusEvent e) {
						int currentPosition= scrolledComposite.getOrigin().y;
						int hight= scrolledComposite.getSize().y;
						int linkPosition= link.getLocation().y;

						if (linkPosition < currentPosition) {
							if (linkPosition < 10)
								linkPosition= 0;

							scrolledComposite.setOrigin(0, linkPosition);
						} else if (linkPosition + 20 > currentPosition + hight) {
							scrolledComposite.setOrigin(0, linkPosition - hight + link.getSize().y);
						}
					}

					@Override
					public void focusLost(FocusEvent e) {
					}
				});
			}
		}

		private Link createCompletionProposalLink(Composite parent, final ICompletionProposal proposal, int count) {
//			final boolean isMultiFix= count > 1;
//			if (isMultiFix) {
//				new Label(parent, SWT.NONE); // spacer to fill image cell
//				parent= new Composite(parent, SWT.NONE); // indented composite for multi-fix
//				GridLayout layout= new GridLayout(2, false);
//				layout.marginWidth= 0;
//				layout.marginHeight= 0;
//				parent.setLayout(layout);
//			}

			Label proposalImage= new Label(parent, SWT.NONE);
			proposalImage.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
			Image image= /*isMultiFix ? JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_MULTI_FIX) :*/ proposal.getImage();
			if (image != null) {
				proposalImage.setImage(image);

				proposalImage.addMouseListener(new MouseListener() {

					@Override
					public void mouseDoubleClick(MouseEvent e) {
					}

					@Override
					public void mouseDown(MouseEvent e) {
					}

					@Override
					public void mouseUp(MouseEvent e) {
						if (e.button == 1) {
							apply(proposal, fInput.viewer, fInput.position.offset/*, isMultiFix*/);
						}
					}

				});
			}

			Link proposalLink= new Link(parent, SWT.NONE);
			GridData layoutData= new GridData(SWT.FILL, SWT.CENTER, true, false);
			String linkText;
//			if (isMultiFix) {
//				linkText= MessageFormat.format("Fix {0} problems of same category in file", count);
//			} else {
				linkText= proposal.getDisplayString();
//			}
			proposalLink.setText("<a>" + linkText + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$
			proposalLink.setLayoutData(layoutData);
			proposalLink.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					apply(proposal, fInput.viewer, fInput.position.offset/*, isMultiFix*/);
				}
			});
			return proposalLink;
		}

		private void apply(ICompletionProposal p, ITextViewer viewer, int offset/*, boolean isMultiFix*/) {
			//Focus needs to be in the text viewer, otherwise linked mode does not work
			dispose();

			IRewriteTarget target= null;
			try {
				IDocument document= viewer.getDocument();

				if (viewer instanceof ITextViewerExtension) {
					ITextViewerExtension extension= (ITextViewerExtension) viewer;
					target= extension.getRewriteTarget();
				}

				if (target != null)
					target.beginCompoundChange();

				if (p instanceof ICompletionProposalExtension2) {
					ICompletionProposalExtension2 e= (ICompletionProposalExtension2) p;
					e.apply(viewer, (char) 0, /*isMultiFix ? SWT.CONTROL :*/ SWT.NONE, offset);
				} else if (p instanceof ICompletionProposalExtension) {
					ICompletionProposalExtension e= (ICompletionProposalExtension) p;
					e.apply(document, (char) 0, offset);
				} else {
					p.apply(document);
				}

				Point selection= p.getSelection(document);
				if (selection != null) {
					viewer.setSelectedRange(selection.x, selection.y);
					viewer.revealRange(selection.x, selection.y);
				}
			} finally {
				if (target != null)
					target.endCompoundChange();
			}
		}
	}

	/**
	 * Presenter control creator.
	 */
	private static final class PresenterControlCreator extends AbstractReusableInformationControlCreator {
		@Override
		public IInformationControl doCreateInformationControl(Shell parent) {
			return new AnnotationInformationControl(parent, new ToolBarManager(SWT.FLAT));
		}
	}


	/**
	 * Hover control creator.
	 */
	private static final class HoverControlCreator extends AbstractReusableInformationControlCreator {
		private final IInformationControlCreator fPresenterControlCreator;

		public HoverControlCreator(IInformationControlCreator presenterControlCreator) {
			fPresenterControlCreator= presenterControlCreator;
		}

		@Override
		public IInformationControl doCreateInformationControl(Shell parent) {
			return new AnnotationInformationControl(parent, EditorsUI.getTooltipAffordanceString()) {
				@Override
				public IInformationControlCreator getInformationPresenterControlCreator() {
					return fPresenterControlCreator;
				}
			};
		}

		@Override
		public boolean canReuse(IInformationControl control) {
			if (!super.canReuse(control))
				return false;

			if (control instanceof IInformationControlExtension4)
				((IInformationControlExtension4) control).setStatusText(EditorsUI.getTooltipAffordanceString());

			return true;
		}
	}

	/**
	 * Action to configure the annotation preferences.
	 */
	private static final class ConfigureAnnotationsAction extends Action {

		private final Annotation fAnnotation;
		private final IInformationControl fInfoControl;

		public ConfigureAnnotationsAction(Annotation annotation, IInformationControl infoControl) {
			super();
			fAnnotation= Objects.requireNonNull(annotation);
			fInfoControl= Objects.requireNonNull(infoControl);
			setImageDescriptor(Activator.getImageDescriptor(Activator.IMG_ELCL_CONFIGURE_ANNOTATIONS));
			setDisabledImageDescriptor(Activator.getImageDescriptor(Activator.IMG_DLCL_CONFIGURE_ANNOTATIONS));
			setToolTipText(Messages.AnnotationHover_Configure_annotation_preferences);
		}

		@Override
		public void run() {
			Shell shell= PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();

			Object data= null;
			AnnotationPreference preference= getAnnotationPreference(fAnnotation);
			if (preference != null)
				data= preference.getPreferenceLabel();

			fInfoControl.dispose(); //FIXME: should have protocol to hide, rather than dispose
			PreferencesUtil.createPreferenceDialogOn(shell, "org.eclipse.ui.editors.preferencePages.Annotations", null, data).open(); //$NON-NLS-1$
		}
	}

	private final IPreferenceStore fStore;
	private final DefaultMarkerAnnotationAccess fAnnotationAccess= new DefaultMarkerAnnotationAccess();
	private IInformationControlCreator fHoverControlCreator;
	private IInformationControlCreator fPresenterControlCreator;

	/**
	 * Constructor.
	 *
	 * @param store a preference store (not <code>null</code>)
	 */
	public AnnotationHover(IPreferenceStore store) {
	    fStore= Objects.requireNonNull(store);
	}

    @Override
    public IRegion getHoverRegion(ITextViewer textViewer, int offset)
    {
        return DefaultWordFinder.INSTANCE.findWord(textViewer.getDocument(), offset);
    }

	@Override
	public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {
		return null;
	}

	@Override
	public Object getHoverInfo2(ITextViewer textViewer, IRegion hoverRegion) {
		IAnnotationModel model= null;
		if (textViewer instanceof ISourceViewer) {
			model= ((ISourceViewer)textViewer).getAnnotationModel();
		}
		if (model == null)
			return null;

		Iterator<Annotation> it;
		if (model instanceof IAnnotationModelExtension2)
			it= ((IAnnotationModelExtension2)model).getAnnotationIterator(hoverRegion.getOffset(), hoverRegion.getLength(), true, true);
		else
			it= model.getAnnotationIterator();

		int layer= -1;
		Annotation annotation= null;
		Position position= null;
		while (it.hasNext()) {
			Annotation a= it.next();

			if (!isIncluded(a))
			    continue;

			AnnotationPreference preference= getAnnotationPreference(a);
			if (preference == null
					|| (((preference.getTextPreferenceKey() == null) || !fStore.getBoolean(preference.getTextPreferenceKey())) && ((preference.getHighlightPreferenceKey() == null) || !fStore.getBoolean(preference.getHighlightPreferenceKey()))))
				continue;

			Position p= model.getPosition(a);

			int l= fAnnotationAccess.getLayer(a);

			if (l > layer && p != null && p.overlapsWith(hoverRegion.getOffset(), hoverRegion.getLength())) {
				String msg= a.getText();
				if (msg != null && msg.trim().length() > 0) {
					layer= l;
					annotation= a;
					position= p;
				}
			}
		}
		if (layer > -1)
			return createAnnotationInfo(annotation, position, textViewer);

		return null;
	}

    /**
     * Tells whether the given annotation should be included in the computation.
     *
     * @param annotation the annotation to test (never <code>null</code>)
     * @return <code>true</code> if the annotation is included in the computation,
     *  and <code>false</code> otherwise
     */
	protected boolean isIncluded(Annotation annotation) {
	    return !annotation.isMarkedDeleted();
	}

	/**
	 * Creates and returns an annotation info object for the given parameters.
	 *
	 * @param annotation not <code>null</code>
	 * @param position not <code>null</code>
	 * @param textViewer not <code>null</code>
	 * @return the created info object (never <code>null</code>)
	 */
	protected AnnotationInfo createAnnotationInfo(Annotation annotation, Position position, ITextViewer textViewer) {
		return new AnnotationInfo(annotation, position, textViewer);
	}

	@Override
	public IInformationControlCreator getHoverControlCreator() {
		if (fHoverControlCreator == null)
			fHoverControlCreator= new HoverControlCreator(getInformationPresenterControlCreator());
		return fHoverControlCreator;
	}

	public IInformationControlCreator getInformationPresenterControlCreator() {
		if (fPresenterControlCreator == null)
			fPresenterControlCreator= new PresenterControlCreator();
		return fPresenterControlCreator;
	}

	/**
	 * Returns the annotation preference for the given annotation.
	 *
	 * @param annotation the annotation
	 * @return the annotation preference or <code>null</code> if none
	 */
	private static AnnotationPreference getAnnotationPreference(Annotation annotation) {

		if (annotation.isMarkedDeleted())
			return null;
		return EditorsUI.getAnnotationPreferenceLookup().getAnnotationPreference(annotation);
	}
}
