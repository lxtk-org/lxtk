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
/**
 * Provides support for implementing Language Server Protocol clients in LXTK.
 * <p>
 * The class {@link org.lxtk.client.AbstractLanguageClientController} provides an extensible
 * framework for controlling a language client talking to a language server according to
 * the Language Server Protocol. A subclass of that class must return an instance of
 * {@link org.lxtk.client.AbstractLanguageClient} from the <code>getLanguageClient()</code> method,
 * which is called by the framework each time a connection to the language server is created using
 * the connection factory returned from the <code>getConnectionFactory()</code> method.
 * The <code>AbstractLanguageClient</code> provides partial implementation of a
 * {@link org.eclipse.lsp4j.services.LanguageClient} that is also a composite
 * {@link org.lxtk.client.Feature} by containing a given collection of <code>Feature</code>s.
 * This feature-based design of the language client is similar to the design used in
 * <a href="https://github.com/microsoft/vscode-languageserver-node">vscode-languageserver-node</a>.
 * Features that support dynamic registration are represented by the
 * {@link org.lxtk.client.DynamicFeature} sub-interface. This package provides
 * a number of <code>DynamicFeature</code> implementations that are integrated
 * with {@linkplain org.lxtk LXTK Core Services}.
 * </p>
 */
package org.lxtk.client;
