/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.ast.planNodes;

import java.util.Arrays;
import java.util.Objects;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.memory.MemoryStoreConnection;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.ConnectionsGroup;

/**
 * @author Håvard Ottestad
 */
public class FilterTargetIsObject extends FilterPlanNode {

	private final SailConnection connection;
	private final Resource[] dataGraph;

	public FilterTargetIsObject(SailConnection connection, Resource[] dataGraph, PlanNode parent,
			ConnectionsGroup connectionsGroup) {
		super(parent, connectionsGroup);
		this.connection = connection;
		assert this.connection != null;
		this.dataGraph = dataGraph;
	}

	@Override
	boolean checkTuple(Reference t) {
		Value target = t.get().getActiveTarget();
		return connection.hasStatement(null, null, target, true, dataGraph);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		if (!super.equals(o)) {
			return false;
		}

		FilterTargetIsObject that = (FilterTargetIsObject) o;
		if (connection instanceof MemoryStoreConnection && that.connection instanceof MemoryStoreConnection) {
			return ((MemoryStoreConnection) connection).getSail()
					.equals(((MemoryStoreConnection) that.connection).getSail())
					&& Arrays.equals(dataGraph, that.dataGraph);
		}
		return Objects.equals(connection, that.connection) && Arrays.equals(dataGraph, that.dataGraph);
	}

	@Override
	public int hashCode() {
		if (connection instanceof MemoryStoreConnection) {
			return Objects.hash(super.hashCode(), ((MemoryStoreConnection) connection).getSail(),
					Arrays.hashCode(dataGraph));
		}
		return Objects.hash(super.hashCode(), connection, Arrays.hashCode(dataGraph));
	}
}
