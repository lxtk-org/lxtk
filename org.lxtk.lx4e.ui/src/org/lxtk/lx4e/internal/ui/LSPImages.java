/*******************************************************************************
 * Copyright (c) 2016, 2022 Rogue Wave Software Inc. and others.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Michał Niewrzał (Rogue Wave Software Inc.) - initial implementation
 *******************************************************************************/
package org.lxtk.lx4e.internal.ui;

import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.SymbolKind;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.Bundle;

@SuppressWarnings("javadoc")
//@formatter:off
public class LSPImages {


    private LSPImages() {
        // this class shouldn't be instantiated
    }

    private static ImageRegistry imageRegistry;
//    private static final Map<java.awt.Color, Image> colorToImageCache = new HashMap<>();
    private static final String ICONS_PATH = "$nl$/icons/full/"; //$NON-NLS-1$
    private static final String OBJECT = ICONS_PATH + "obj16/"; // basic colors - size 16x16 //$NON-NLS-1$
    private static final Image EMPTY_IMAGE;
    private static final Image ERROR_IMAGE_GRAY;
    private static final Image WARN_IMAGE_GRAY;
    private static final Image INFO_IMAGE_GRAY;
    static {
        IWorkbench workbench = PlatformUI.getWorkbench();
        Display display = workbench.getDisplay();
        ISharedImages sharedImages= workbench.getSharedImages();
        EMPTY_IMAGE = new Image(display, 16, 16);
        ERROR_IMAGE_GRAY = new Image(display,
            sharedImages.getImage(ISharedImages.IMG_OBJS_ERROR_TSK), SWT.IMAGE_GRAY);
        WARN_IMAGE_GRAY = new Image(display,
            sharedImages.getImage(ISharedImages.IMG_OBJS_WARN_TSK), SWT.IMAGE_GRAY);
        INFO_IMAGE_GRAY = new Image(display,
            sharedImages.getImage(ISharedImages.IMG_OBJS_INFO_TSK), SWT.IMAGE_GRAY);
    }

    public static final String IMG_MODULE = "IMG_MODULE"; //$NON-NLS-1$
    public static final String IMG_NAMESPACE = "IMG_NAMESPACE"; //$NON-NLS-1$
    public static final String IMG_PACKAGE = "IMG_PACKAGE"; //$NON-NLS-1$
    public static final String IMG_CLASS = "IMG_CLASS"; //$NON-NLS-1$
    public static final String IMG_METHOD = "IMG_METOHD"; //$NON-NLS-1$
    public static final String IMG_PROPERTY = "IMG_PROPERTY"; //$NON-NLS-1$
    public static final String IMG_FIELD = "IMG_FIELD"; //$NON-NLS-1$
    public static final String IMG_CONSTRUCTOR = "IMG_CONSTRUCTOR"; //$NON-NLS-1$
    public static final String IMG_ENUM = "IMG_ENUM"; //$NON-NLS-1$
    public static final String IMG_INTERACE = "IMG_INTERFACE"; //$NON-NLS-1$
    public static final String IMG_FUNCTION = "IMG_FUNCTION"; //$NON-NLS-1$
    public static final String IMG_VARIABLE = "IMG_VARIABLE"; //$NON-NLS-1$
    public static final String IMG_CONSTANT = "IMG_CONSTANT"; //$NON-NLS-1$
    public static final String IMG_TEXT = "IMG_TEXT"; //$NON-NLS-1$
    public static final String IMG_STRING = IMG_TEXT;
    public static final String IMG_NUMBER = "IMG_NUMBER"; //$NON-NLS-1$
    public static final String IMG_BOOLEAN = "IMG_BOOLEAN"; //$NON-NLS-1$
    public static final String IMG_ARRAY = "IMG_ARRAY"; //$NON-NLS-1$
    public static final String IMG_UNIT = "IMG_UNIT"; //$NON-NLS-1$
    public static final String IMG_VALUE = "IMG_VALUE"; //$NON-NLS-1$
    public static final String IMG_KEYWORD = "IMG_KEYWORD"; //$NON-NLS-1$
    public static final String IMG_SNIPPET = "IMG_SNIPPET"; //$NON-NLS-1$
    public static final String IMG_COLOR = "IMG_COLOR"; //$NON-NLS-1$
    public static final String IMG_REFERENCE = "IMG_REFERENCE"; //$NON-NLS-1$

    static void initialize(ImageRegistry registry) {
        imageRegistry = registry;

        declareRegistryImage(IMG_MODULE, OBJECT + "module.png"); //$NON-NLS-1$
        declareRegistryImage(IMG_NAMESPACE, OBJECT + "namespace.png"); //$NON-NLS-1$
        declareRegistryImage(IMG_PACKAGE, OBJECT + "package.png"); //$NON-NLS-1$
        declareRegistryImage(IMG_CLASS, OBJECT + "class.png"); //$NON-NLS-1$
        declareRegistryImage(IMG_METHOD, OBJECT + "method.png"); //$NON-NLS-1$
        declareRegistryImage(IMG_PROPERTY, OBJECT + "property.png"); //$NON-NLS-1$
        declareRegistryImage(IMG_FIELD, OBJECT + "field.png"); //$NON-NLS-1$
        declareRegistryImage(IMG_CONSTRUCTOR, OBJECT + "constructor.png"); //$NON-NLS-1$
        declareRegistryImage(IMG_ENUM, OBJECT + "enum.png"); //$NON-NLS-1$
        declareRegistryImage(IMG_INTERACE, OBJECT + "interface.png"); //$NON-NLS-1$
        declareRegistryImage(IMG_FUNCTION, OBJECT + "function.png"); //$NON-NLS-1$
        declareRegistryImage(IMG_VARIABLE, OBJECT + "variable.png"); //$NON-NLS-1$
        declareRegistryImage(IMG_CONSTANT, OBJECT + "constant.png"); //$NON-NLS-1$
        declareRegistryImage(IMG_STRING, OBJECT + "string.png"); //$NON-NLS-1$
        declareRegistryImage(IMG_NUMBER, OBJECT + "number.png"); //$NON-NLS-1$
        declareRegistryImage(IMG_BOOLEAN, OBJECT + "boolean.png"); //$NON-NLS-1$
        declareRegistryImage(IMG_ARRAY, OBJECT + "array.png"); //$NON-NLS-1$

        declareRegistryImage(IMG_TEXT, OBJECT + "text.png"); //$NON-NLS-1$
        declareRegistryImage(IMG_UNIT, OBJECT + "unit.png"); //$NON-NLS-1$
        declareRegistryImage(IMG_VALUE, OBJECT + "value.png"); //$NON-NLS-1$
        declareRegistryImage(IMG_KEYWORD, OBJECT + "keyword.png"); //$NON-NLS-1$
        declareRegistryImage(IMG_SNIPPET, OBJECT + "snippet.png"); //$NON-NLS-1$
        declareRegistryImage(IMG_COLOR, OBJECT + "color.png"); //$NON-NLS-1$
        declareRegistryImage(IMG_REFERENCE, OBJECT + "reference.png"); //$NON-NLS-1$
    }

    private static final void declareRegistryImage(String key, String path) {
        ImageDescriptor desc = ImageDescriptor.getMissingImageDescriptor();
        Bundle bundle = Platform.getBundle(Activator.PLUGIN_ID);
        URL url = null;
        if (bundle != null) {
            url = FileLocator.find(bundle, new Path(path), null);
            if (url != null) {
                desc = ImageDescriptor.createFromURL(url);
            }
        }
        imageRegistry.put(key, desc);
    }

    /**
     * Returns the <code>Image</code> identified by the given key, or <code>null</code> if it does not exist.
     */
    public static Image getImage(String key) {
        return getImageRegistry().get(key);
    }

    /**
     * Returns the <code>ImageDescriptor</code> identified by the given key, or <code>null</code> if it does not exist.
     */
    public static ImageDescriptor getImageDescriptor(String key) {
        return getImageRegistry().getDescriptor(key);
    }

    public static ImageRegistry getImageRegistry() {
        if (imageRegistry == null) {
            imageRegistry = Activator.getDefault().getImageRegistry();
        }
        return imageRegistry;
    }

    public static Image getDiagnosticImageGray(DiagnosticSeverity severity)
    {
        if (severity == null)
            severity = DiagnosticSeverity.Error;
        switch (severity) {
        case Error:
            return ERROR_IMAGE_GRAY;
        case Warning:
            return WARN_IMAGE_GRAY;
        default:
            return INFO_IMAGE_GRAY;
        }
    }

    public static Image imageFromSymbolKind(SymbolKind kind) {
        if (kind == null) {
            return EMPTY_IMAGE;
        }
        switch (kind) {
        case Array:
            return getImage(IMG_ARRAY);
        case Boolean:
            return getImage(IMG_BOOLEAN);
        case Class:
            return getImage(IMG_CLASS);
        case Constant:
            return getImage(IMG_CONSTANT);
        case Constructor:
            return getImage(IMG_CONSTRUCTOR);
        case Enum:
            return getImage(IMG_ENUM);
        case Field:
            return getImage(IMG_FIELD);
        case File:
            return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FILE);
        case Function:
            return getImage(IMG_FUNCTION);
        case Interface:
            return getImage(IMG_INTERACE);
        case Method:
            return getImage(IMG_METHOD);
        case Module:
            return getImage(IMG_MODULE);
        case Namespace:
            return getImage(IMG_NAMESPACE);
        case Number:
            return getImage(IMG_NUMBER);
        case Package:
            return getImage(IMG_PACKAGE);
        case Property:
            return getImage(IMG_PROPERTY);
        case String:
            return getImage(IMG_STRING);
        case Variable:
            return getImage(IMG_VARIABLE);
        default:
            // when the SymbolKind is out the cases above
            return EMPTY_IMAGE;
        }
    }

    public static ImageDescriptor imageDescriptorFromSymbolKind(SymbolKind kind) {
        if (kind == null) {
            return null;
        }
        switch (kind) {
        case Array:
            return getImageDescriptor(IMG_ARRAY);
        case Boolean:
            return getImageDescriptor(IMG_BOOLEAN);
        case Class:
            return getImageDescriptor(IMG_CLASS);
        case Constant:
            return getImageDescriptor(IMG_CONSTANT);
        case Constructor:
            return getImageDescriptor(IMG_CONSTRUCTOR);
        case Enum:
            return getImageDescriptor(IMG_ENUM);
        case Field:
            return getImageDescriptor(IMG_FIELD);
        case File:
            return PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_OBJ_FILE);
        case Function:
            return getImageDescriptor(IMG_FUNCTION);
        case Interface:
            return getImageDescriptor(IMG_INTERACE);
        case Method:
            return getImageDescriptor(IMG_METHOD);
        case Module:
            return getImageDescriptor(IMG_MODULE);
        case Namespace:
            return getImageDescriptor(IMG_NAMESPACE);
        case Number:
            return getImageDescriptor(IMG_NUMBER);
        case Package:
            return getImageDescriptor(IMG_PACKAGE);
        case Property:
            return getImageDescriptor(IMG_PROPERTY);
        case String:
            return getImageDescriptor(IMG_STRING);
        case Variable:
            return getImageDescriptor(IMG_VARIABLE);
        default:
            // when the SymbolKind is out the cases above
            return null;
        }
    }

    public static Image imageFromCompletionItem(CompletionItem completionItem) {
        CompletionItemKind kind = completionItem.getKind();
        switch (kind) {
        case Text:
            return getImage(IMG_TEXT);
        case Method:
            return getImage(IMG_METHOD);
        case Function:
            return getImage(IMG_FUNCTION);
        case Constructor:
            return getImage(IMG_CONSTRUCTOR);
        case Field:
            return getImage(IMG_FIELD);
        case Variable:
            return getImage(IMG_VARIABLE);
        case Class:
            return getImage(IMG_CLASS);
        case Interface:
            return getImage(IMG_INTERACE);
        case Module:
            return getImage(IMG_MODULE);
        case Property:
            return getImage(IMG_PROPERTY);
        case Unit:
            return getImage(IMG_UNIT);
        case Value:
            return getImage(IMG_VALUE);
        case Enum:
            return getImage(IMG_ENUM);
        case Keyword:
            return getImage(IMG_KEYWORD);
        case Snippet:
            return getImage(IMG_SNIPPET);
//        case Color:
//            return getImageForColor(completionItem);
        case File:
            return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FILE);
        case Folder:
            return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);
        case Reference:
            return getImage(IMG_REFERENCE);
        default:
            return null;
        }
    }

//    private static Image getImageForColor(CompletionItem completionItem) {
//        String hexValue = null;
//
//        // TODO most probably can be extended for more cases
//        String docString = LSPEclipseUtils.getDocString(completionItem.getDocumentation());
//        if (docString != null && docString.startsWith("#")) { //$NON-NLS-1$
//            hexValue = docString;
//        } else if (completionItem.getLabel().startsWith("#")) { //$NON-NLS-1$
//            hexValue = completionItem.getLabel();
//        }
//        if (hexValue == null) {
//            return null;
//        }
//
//        java.awt.Color decodedColor = null;
//        try {
//            decodedColor = java.awt.Color.decode(hexValue);
//        } catch (NumberFormatException e) {
//            LanguageServerPlugin.logError(e);
//            return null;
//        }
//
//        return colorToImageCache.computeIfAbsent(decodedColor, key -> {
//            // TODO most probably some scaling should be done for HIDPI
//            Image image = new Image(Display.getDefault(), 16, 16);
//            GC gc = new GC(image);
//            Color color = new Color(Display.getDefault(), key.getRed(), key.getGreen(),
//                key.getBlue(), key.getAlpha());
//            gc.setBackground(color);
//            gc.fillRectangle(0, 0, 16, 16);
//            color.dispose();
//            gc.dispose();
//            return image;
//        });
//    }

}
