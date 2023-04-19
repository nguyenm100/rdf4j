/*******************************************************************************
 * Copyright (c) 2016 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.http.server.repository.transaction;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public interface ActiveTransactionRegistry {

	public void destroyScheduler();

	long getTimeout(TimeUnit unit);

	void register(Transaction txn);

	Transaction getTransaction(UUID id);

	void active(Transaction txn);

	void deregister(Transaction transaction);
}
