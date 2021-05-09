/*******************************************************************************
 * Copyright (c) 2019, 2021 1C-Soft LLC.
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
/**
 * Provides basic facilities for LXTK integration with the Eclipse platform.
 *
 * <h2>LX4E-specific implementations of LXTK Core Services</h2>
 * <ul>
 * <li>{@link org.lxtk.lx4e.EclipseDocumentService} provides a {@link org.lxtk.DocumentService}
 * implementation</li>
 * <li>{@link org.lxtk.lx4e.EclipseLanguageService} provides a {@link org.lxtk.LanguageService}
 * implementation</li>
 * <li>{@link org.lxtk.lx4e.EclipseCommandService} provides a {@link org.lxtk.CommandService}
 * implementation</li>
 * <li>{@link org.lxtk.lx4e.EclipseWorkspaceService} provides a {@link org.lxtk.WorkspaceService}
 * implementation</li>
 * <li>{@link org.lxtk.lx4e.EclipseProgressService} provides a {@link org.lxtk.ProgressService}
 * implementation</li>
 * </ul>
 *
 * <h2>Other extensions of LXTK Core Framework</h2>
 * <ul>
 * <li>{@link org.lxtk.lx4e.EclipseTextDocument} implements {@link org.lxtk.TextDocument}
 * on top of a given {@link org.eclipse.handly.buffer.IBuffer}</li>
 * <li>{@link org.lxtk.lx4e.EclipseLog} implements a {@link org.lxtk.util.Log} that delegates
 * to the {@link org.eclipse.core.runtime.ILog} of a given Eclipse bundle</li>
 * <li>{@link org.lxtk.lx4e.ResourceWatchFeature} notifies the language server about
 * Eclipse resource changes</li>
 * <li>{@link org.lxtk.lx4e.WorkspaceFoldersManager} manages a mapping from the Eclipse workspace
 * to the workspace folders of a {@link org.lxtk.WorkspaceService}</li>
 * </ul>
 *
 * <h2>URI Handlers</h2>
 * <ul>
 * <li>{@link org.lxtk.lx4e.IUriHandler} provides information about resources denoted by URIs</li>
 * <li>{@link org.lxtk.lx4e.ResourceUriHandler} is an implementation that maps URIs to
 * Eclipse workspace resources</li>
 * <li>{@link org.lxtk.lx4e.EfsUriHandler} is an implementation that maps URIs to
 * Eclipse File System (EFS) resources</li>
 * <li>{@link org.lxtk.lx4e.TextDocumentUriHandler} is an implementation that maps URIs to
 * text documents managed by a given {@link org.lxtk.DocumentService}</li>
 * <li>{@link org.lxtk.lx4e.UriHandlers} provides static methods for operating on
 * <code>IUriHandler</code>s</li>
 * </ul>
 *
 * <h2>Utilities</h2>
 * <ul>
 * <li>{@link org.lxtk.lx4e.DocumentUtil} provides static utility methods that bridge the gap
 * between Eclipse documents and document-related structures of LSP</li>
 * </ul>
 */
package org.lxtk.lx4e;
