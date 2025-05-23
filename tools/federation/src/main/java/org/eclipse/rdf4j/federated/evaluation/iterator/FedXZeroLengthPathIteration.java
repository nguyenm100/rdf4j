/*******************************************************************************
 * Copyright (c) 2024 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.federated.evaluation.iterator;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

import org.eclipse.rdf4j.collection.factory.api.CollectionFactory;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.LookAheadIteration;
import org.eclipse.rdf4j.federated.algebra.EmptyStatementPattern;
import org.eclipse.rdf4j.federated.algebra.ExclusiveStatement;
import org.eclipse.rdf4j.federated.algebra.FedXZeroLengthPath;
import org.eclipse.rdf4j.federated.algebra.StatementSource;
import org.eclipse.rdf4j.federated.algebra.StatementSourcePattern;
import org.eclipse.rdf4j.federated.structures.QueryInfo;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.MutableBindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.StatementPattern.Scope;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryBindingSet;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.QueryEvaluationContext;
import org.eclipse.rdf4j.query.algebra.evaluation.iterator.CrossProductIteration;
import org.eclipse.rdf4j.query.algebra.evaluation.iterator.ZeroLengthPathIteration;

/**
 * An iteration to evaluated {@link FedXZeroLengthPath}
 *
 * @see ZeroLengthPathIteration
 */
public class FedXZeroLengthPathIteration extends LookAheadIteration<BindingSet> {

	/*
	 * IMPL NOTE:
	 *
	 * This is technically almost a 1:1 copy of
	 * org.eclipse.rdf4j.query.algebra.evaluation.iterator.ZeroLengthPathIteration Reusing or extending
	 * ZeroLengthPathIteration requires refactoring in its constructor initialization.
	 *
	 * The main difference is in keeping track of QueryInfo and statement sources to be used in the precompiled
	 * statement. Additionally the variable names for anon vars are renamed
	 */

	private static final Literal OBJECT = SimpleValueFactory.getInstance().createLiteral("object");

	private static final Literal SUBJECT = SimpleValueFactory.getInstance().createLiteral("subject");

	// Note: in contrast to the original zero length path iteration we use "_" instead of "-"
	// as we need variable names valid in SELECT queries

	private static final String ANON_SUBJECT_VAR = "zero_length_internal_start";

	private static final String ANON_PREDICATE_VAR = "zero_length_internal_pred";

	private static final String ANON_OBJECT_VAR = "zero_length_internal_end";

	private static final String ANON_SEQUENCE_VAR = "zero_length_internal_seq";
	private final CollectionFactory cf;

	private QueryBindingSet result;

	private final Value subj;

	private final Value obj;

	private final BindingSet bindings;

	private CloseableIteration<BindingSet> iter;

	private Set<Value> reportedValues;

	private final Var contextVar;

	private final EvaluationStrategy evaluationStrategy;

	private final QueryEvaluationStep precompile;

	private final QueryEvaluationContext context;

	private final BiConsumer<Value, MutableBindingSet> setSubject;

	private final BiConsumer<Value, MutableBindingSet> setObject;

	private final BiConsumer<Value, MutableBindingSet> setContext;

	public FedXZeroLengthPathIteration(EvaluationStrategy evaluationStrategyImpl, Var subjectVar, Var objVar,
			Value subj, Value obj, Var contextVar, BindingSet bindings, QueryEvaluationContext context,
			QueryInfo queryInfo, List<StatementSource> statementSources) {
		this.evaluationStrategy = evaluationStrategyImpl;
		this.context = context;
		this.result = new QueryBindingSet(bindings);
		this.contextVar = contextVar;
		this.subj = subj;
		this.obj = obj;
		this.bindings = bindings;
		Var startVar = createAnonVar(ANON_SUBJECT_VAR);
		Var predicate = createAnonVar(ANON_PREDICATE_VAR);
		Var endVar = createAnonVar(ANON_OBJECT_VAR);

		StatementPattern subjects;
		if (contextVar != null) {
			subjects = new StatementPattern(Scope.NAMED_CONTEXTS, startVar, predicate, endVar, contextVar.clone());
		} else {
			subjects = new StatementPattern(startVar, predicate, endVar);
		}

		// specialization for federation: we need to attach statement sources
		// to the precompiled expr
		TupleExpr expr;
		if (statementSources.size() == 1) {
			expr = new ExclusiveStatement(subjects, statementSources.get(0), queryInfo);
		} else if (statementSources.size() > 1) {
			expr = new StatementSourcePattern(subjects, queryInfo);
			for (var stmtSource : statementSources) {
				((StatementSourcePattern) expr).addStatementSource(stmtSource);
			}
		} else {
			expr = new EmptyStatementPattern(subjects);
		}
		precompile = evaluationStrategy.precompile(expr, context);

		setSubject = context.addBinding(subjectVar.getName());
		setObject = context.addBinding(objVar.getName());
		if (contextVar != null) {
			setContext = context.addBinding(contextVar.getName());
		} else {
			setContext = null;
		}

		this.cf = evaluationStrategy.getCollectionFactory().get();

	}

	@Override
	protected BindingSet getNextElement() throws QueryEvaluationException {
		if (subj == null && obj == null) {
			if (this.reportedValues == null) {
				reportedValues = cf.createValueSet();
			}
			if (this.iter == null) {
				// join with a sequence so we iterate over every entry twice
				QueryBindingSet bs1 = new QueryBindingSet(1);
				bs1.addBinding(ANON_SEQUENCE_VAR, SUBJECT);
				QueryBindingSet bs2 = new QueryBindingSet(1);
				bs2.addBinding(ANON_SEQUENCE_VAR, OBJECT);
				List<BindingSet> seqList = Arrays.<BindingSet>asList(bs1, bs2);
				iter = new CrossProductIteration(createIteration(), seqList);
			}

			while (iter.hasNext()) {
				BindingSet bs = iter.next();

				boolean isSubjOrObj = bs.getValue(ANON_SEQUENCE_VAR).stringValue().equals("subject");
				String endpointVarName = isSubjOrObj ? ANON_SUBJECT_VAR : ANON_OBJECT_VAR;
				Value v = bs.getValue(endpointVarName);

				if (reportedValues.add(v)) {
					MutableBindingSet next = context.createBindingSet(bindings);
					setSubject.accept(v, next);
					setObject.accept(v, next);
					if (setContext != null) {
						Value context = bs.getValue(contextVar.getName());
						if (context != null) {
							setContext.accept(context, next);
						}
					}
					return next;
				}
			}
			iter.close();

			// if we're done, throw away the cached list of values to avoid hogging
			// resources
			reportedValues = null;
			return null;
		} else {
			if (result != null) {
				if (obj == null && subj != null) {
					setObject.accept(subj, result);
				} else if (subj == null && obj != null) {
					setSubject.accept(obj, result);
				} else if (subj != null && subj.equals(obj)) {
					// empty bindings
					// (result but nothing to bind as subjectVar and objVar are both fixed)
				} else {
					result = null;
				}
			}

			QueryBindingSet next = result;
			result = null;
			return next;
		}
	}

	private CloseableIteration<BindingSet> createIteration() {
		CloseableIteration<BindingSet> iter = precompile.evaluate(bindings);
		return iter;
	}

	public Var createAnonVar(String varName) {
		Var var = new Var(varName, true);
		return var;
	}

	@Override
	protected void handleClose() {
		if (iter != null) {
			iter.close();
		}
	}
}
