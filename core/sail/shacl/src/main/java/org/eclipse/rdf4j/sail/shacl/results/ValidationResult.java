/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.results;

import static org.eclipse.rdf4j.model.util.Values.bnode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDF4J;
import org.eclipse.rdf4j.model.vocabulary.RSX;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ast.PropertyShape;
import org.eclipse.rdf4j.sail.shacl.ast.Severity;
import org.eclipse.rdf4j.sail.shacl.ast.Shape;
import org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents.ConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents.SparqlConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ast.paths.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The ValidationResult represents the results from a SHACL validation in an easy-to-use Java API.
 *
 * @deprecated The ValidationResult is deprecated because it is planned moved to a new package to allow it to be used
 *             with remote validation results.
 */
@Deprecated
public class ValidationResult {

	private static final Logger logger = LoggerFactory.getLogger(ValidationResult.class);

	private Resource id;
	private final Optional<Value> value;
	private final Shape shape;

	private final SourceConstraintComponent sourceConstraintComponent;
	private final ConstraintComponent sourceConstraint;
	private final Severity severity;
	private final Value focusNode;
	private final Resource[] dataGraphs;
	private final Resource[] shapesGraphs;
	private Path path;
	private Path rsxPairwisePath;
	private ValidationResult detail;
	private Value pathIri;

	public ValidationResult(Value focusNode, Value value, Shape shape,
			ConstraintComponent sourceConstraint, Severity severity, ConstraintComponent.Scope scope,
			Resource[] dataGraphs, Resource[] shapesGraphs) {
		this.focusNode = focusNode;
		assert this.focusNode != null;
		this.sourceConstraintComponent = sourceConstraint.getConstraintComponent();
		this.sourceConstraint = sourceConstraint;
		this.shape = shape;

		if (sourceConstraintComponent.producesValidationResultValue()) {
			assert !sourceConstraintComponent.alwaysProducesValidationResultValue() || value != null;

			// value could be null if assertions are disabled
			// noinspection ConstantValue
			if (value == null && sourceConstraintComponent.alwaysProducesValidationResultValue()) {
				logger.error(
						"Source constraint component {} was expected to produce a value, but value is null! Shape: {}",
						sourceConstraintComponent, shape);
			}

			this.value = Optional.ofNullable(value);
		} else {
			assert scope != ConstraintComponent.Scope.propertyShape || value == null;
			this.value = Optional.empty();
		}

		if (shape instanceof PropertyShape) {
			this.path = ((PropertyShape) shape).getPath();
		}
		this.severity = severity;
		this.dataGraphs = dataGraphs;
		this.shapesGraphs = shapesGraphs;
	}

	public ValidationResult(Value focusNode, Value value, Shape shape,
			ConstraintComponent sourceConstraint, Severity severity, ConstraintComponent.Scope scope,
			Resource[] dataGraphs, Resource[] shapesGraphs, Path rsxPairwisePath) {
		this(focusNode, value, shape, sourceConstraint, severity, scope, dataGraphs, shapesGraphs);
		this.rsxPairwisePath = rsxPairwisePath;
	}

	/**
	 * @return ValidationResult with more information as to what failed. Usually for nested Shapes in eg. sh:or.
	 */
	public ValidationResult getDetail() {
		return detail;
	}

	public void setDetail(ValidationResult detail) {
		this.detail = detail;
	}

	/**
	 * @return all ValidationResult(s) with more information as to what failed. Usually for nested Shapes in eg. sh:or.
	 */
	public List<ValidationResult> getDetails() {

		ArrayList<ValidationResult> validationResults = new ArrayList<>();

		ValidationResult temp = detail;
		while (temp != null) {
			validationResults.add(temp);
			temp = temp.detail;
		}

		return validationResults;

	}

	public Model asModel(Model model) {
		return asModel(model, new HashSet<>());
	}

	public Model asModel(Model model, Set<Resource> rdfListDedupe) {

		model.add(getId(), RDF.TYPE, SHACL.VALIDATION_RESULT);

		model.add(getId(), SHACL.FOCUS_NODE, focusNode);

		for (Resource graph : contextsToSet(dataGraphs)) {
			model.add(getId(), RSX.dataGraph, graph);
		}

		for (Resource graph : contextsToSet(shapesGraphs)) {
			model.add(getId(), RSX.shapesGraph, graph);
		}

		value.ifPresent(v -> model.add(getId(), SHACL.VALUE, v));

		if (pathIri != null) {
			model.add(getId(), SHACL.RESULT_PATH, pathIri);
		} else if (this.path != null) {
			path.toModel(path.getId(), null, model, new HashSet<>());
			model.add(getId(), SHACL.RESULT_PATH, path.getId());
		}

		if (rsxPairwisePath != null) {
			rsxPairwisePath.toModel(rsxPairwisePath.getId(), null, model, new HashSet<>());
			model.add(getId(), RSX.actualPairwisePath, rsxPairwisePath.getId());
		}

		if (sourceConstraint instanceof SparqlConstraintComponent) {
			model.add(getId(), SHACL.SOURCE_CONSTRAINT, ((SparqlConstraintComponent) sourceConstraint).getId());
		}

		model.add(getId(), SHACL.SOURCE_CONSTRAINT_COMPONENT, getSourceConstraintComponent().getIri());
		model.add(getId(), SHACL.RESULT_SEVERITY, severity.getIri());

		for (Literal message : shape.getMessage()) {
			model.add(getId(), SHACL.RESULT_MESSAGE, message);
		}

		shape.toModel(getId(), SHACL.SOURCE_SHAPE, model, new HashSet<>());

		return model;
	}

	private static Set<Resource> contextsToSet(Resource[] context) {
		if (context == null || context.length == 0) {
			return Collections.emptySet();
		}

		return Arrays.stream(context)
				.map(c -> c == null ? RDF4J.NIL : c)
				.collect(Collectors.toSet());
	}

	/**
	 * @return the path, as specified in the Shape, that caused the violation
	 */
	private Path getPath() {
		return path;
	}

	/**
	 * @return the focus node, aka. the subject, that caused the violation
	 */
	private Value getFocusNode() {
		return focusNode;
	}

	public final Resource getId() {
		if (id == null) {
			id = bnode();
		}
		return id;
	}

	/**
	 * @return the type of the source constraint that caused the violation
	 */
	public SourceConstraintComponent getSourceConstraintComponent() {
		return sourceConstraintComponent;
	}

	@Override
	public String toString() {
		return "ValidationResult{" +
				"focusNode=" + focusNode +
				", value=" + value.orElse(null) +
				", shape=" + shape.getId() +
				", path=" + path +
				", sourceConstraintComponent=" + sourceConstraintComponent +
				", severity=" + severity +
				", detail=" + detail +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		ValidationResult that = (ValidationResult) o;
		return value.equals(that.value) && shape.equals(that.shape)
				&& sourceConstraintComponent == that.sourceConstraintComponent && severity == that.severity
				&& focusNode.equals(that.focusNode) && Objects.equals(path, that.path)
				&& Objects.equals(detail, that.detail);
	}

	@Override
	public int hashCode() {
		return Objects.hash(value, shape, sourceConstraintComponent, severity, focusNode, path, detail);
	}

	public void setPathIri(Value path) {
		this.pathIri = path;
	}

	protected Optional<Value> getValue() {
		return value;
	}

	protected Shape getShape() {
		return shape;
	}

	protected ConstraintComponent getSourceConstraint() {
		return sourceConstraint;
	}

	protected Severity getSeverity() {
		return severity;
	}

	protected Resource[] getDataGraphs() {
		return dataGraphs;
	}

	protected Resource[] getShapesGraphs() {
		return shapesGraphs;
	}
}
