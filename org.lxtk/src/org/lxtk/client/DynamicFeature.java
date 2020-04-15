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
package org.lxtk.client;

import java.util.Set;

import org.eclipse.lsp4j.Registration;
import org.eclipse.lsp4j.Unregistration;
import org.eclipse.lsp4j.services.LanguageServer;

/**
 * Represents a dynamic feature of the language client.
 * <p>
 * <b>Note:</b> Implementations of this interface must be thread-safe.
 * </p>
 *
 * @param <S> server interface type
 */
public interface DynamicFeature<S extends LanguageServer>
    extends Feature<S>
{
    /**
     * Returns one or more JSON-RPC methods for which this feature supports
     * dynamic registration.
     *
     * @return the methods for which the feature supports dynamic registration
     *  (never <code>null</code> or empty). Clients <b>must not</b> modify
     *  the returned set
     */
    Set<String> getMethods();

    /**
     * Called when the language server sends a registration request
     * for one of the feature methods.
     *
     * @param registration not <code>null</code>
     */
    void register(Registration registration);

    /**
     * Called when the language server sends an unregistration request
     * for one of the feature methods.
     *
     * @param unregistration not <code>null</code>
     */
    void unregister(Unregistration unregistration);
}
