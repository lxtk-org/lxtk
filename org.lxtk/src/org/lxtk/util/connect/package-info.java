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
 * Provides basic facilities related to connection management in LXTK.
 * <p>
 * {@link org.lxtk.util.connect.Connection} is a common interface for connections.
 * Sub-interface {@link org.lxtk.util.connect.StreamBasedConnection} represents a connection
 * based on input and output streams. {@link org.lxtk.util.connect.StdioConnection} and
 * {@link org.lxtk.util.connect.SocketConnection} are two general-purpose implementations
 * of <code>StreamBasedConnection</code>.
 * </p>
 * <p>
 * {@link org.lxtk.util.connect.Connectable} provides a common interface for connecting,
 * disconnecting, and monitoring the connection state of <i>connectable</i> objects.
 * {@link org.lxtk.util.connect.AbstractConnectable} is the root class of all
 * <code>Connectable</code> objects.
 * </p>
 */
package org.lxtk.util.connect;
