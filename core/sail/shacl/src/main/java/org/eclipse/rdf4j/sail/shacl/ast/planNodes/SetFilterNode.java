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

import java.util.Objects;
import java.util.Set;

import org.apache.commons.text.StringEscapeUtils;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.ConnectionsGroup;

public class SetFilterNode implements PlanNode {

	private final Set<Value> targetNodeList;
	private final PlanNode parent;
	private final int index;
	private final boolean returnValid;
	private boolean printed;
	private ValidationExecutionLogger validationExecutionLogger;

	public SetFilterNode(Set<Value> targetNodeList, PlanNode parent, int index, boolean returnValid,
			ConnectionsGroup connectionsGroup) {
		this.parent = PlanNodeHelper.handleSorting(this, parent, connectionsGroup);
		this.targetNodeList = targetNodeList;
		this.index = index;
		this.returnValid = returnValid;
	}

	@Override
	public CloseableIteration<? extends ValidationTuple> iterator() {
		return new LoggingCloseableIteration(this, validationExecutionLogger) {

			private CloseableIteration<? extends ValidationTuple> parentIterator;

			ValidationTuple next;

			@Override
			protected void init() {
				parentIterator = parent.iterator();
			}

			private void calulateNext() {
				while (next == null && parentIterator.hasNext()) {
					ValidationTuple temp = parentIterator.next();
					boolean contains = targetNodeList.contains(temp.getActiveTarget());
					if (returnValid && contains) {
						next = temp;
					} else if (!returnValid && !contains) {
						next = temp;
					}
				}
			}

			@Override
			public void localClose() {
				if (parentIterator != null) {
					parentIterator.close();
				}
			}

			@Override
			protected boolean localHasNext() {
				calulateNext();
				return next != null;
			}

			@Override
			protected ValidationTuple loggingNext() {
				calulateNext();

				ValidationTuple temp = next;
				next = null;

				return temp;
			}

		};
	}

	@Override
	public int depth() {
		return parent.depth() + 1;
	}

	@Override
	public void getPlanAsGraphvizDot(StringBuilder stringBuilder) {
		if (printed) {
			return;
		}
		printed = true;
		stringBuilder.append(getId() + " [label=\"" + StringEscapeUtils.escapeJava(this.toString()) + "\"];")
				.append("\n");
		stringBuilder.append(parent.getId() + " -> " + getId()).append("\n");
		parent.getPlanAsGraphvizDot(stringBuilder);
	}

	@Override
	public String getId() {
		return System.identityHashCode(this) + "";
	}

	@Override
	public String toString() {
		return "SetFilterNode{" + "targetNodeList="
				+ Formatter.prefix(targetNodeList) + ", index=" + index
				+ ", returnValid=" + returnValid + '}';
	}

	@Override
	public void receiveLogger(ValidationExecutionLogger validationExecutionLogger) {
		this.validationExecutionLogger = validationExecutionLogger;
		parent.receiveLogger(validationExecutionLogger);
	}

	@Override
	public boolean producesSorted() {
		return parent.producesSorted();
	}

	@Override
	public boolean requiresSorted() {
		return false;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		SetFilterNode that = (SetFilterNode) o;
		return index == that.index && returnValid == that.returnValid && targetNodeList.equals(that.targetNodeList)
				&& parent.equals(that.parent);
	}

	@Override
	public int hashCode() {
		return Objects.hash(targetNodeList, parent, index, returnValid);
	}
}
