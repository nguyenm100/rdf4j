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

package org.eclipse.rdf4j.sail.shacl;

import org.eclipse.rdf4j.common.exception.ValidationException;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDF4J;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.RSX;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.shacl.results.ValidationReport;

public class ShaclSailValidationException extends SailException implements ValidationException {

	private final ValidationReport validationReport;
	private Model cachedModel = null;

	ShaclSailValidationException(ValidationReport validationReport) {
		super("Failed SHACL validation");
		this.validationReport = validationReport;
	}

	/**
	 * @return A Model containing the validation report as specified by the SHACL Recommendation
	 */
	@Override
	public Model validationReportAsModel() {
		if (cachedModel != null) {
			return cachedModel;
		}
		Model model = getValidationReport().asModel();

		model.setNamespace(RSX.NS);
		model.setNamespace(RDF4J.NS);
		model.setNamespace(SHACL.NS);
		model.setNamespace(XSD.NS);
		model.setNamespace(RDF.NS);
		model.setNamespace(RDFS.NS);

		cachedModel = model;

		return model;
	}

	/**
	 * @return A ValidationReport Java object that describes what failed and can optionally be converted to a Model as
	 *         specified by the SHACL Recommendation
	 * @deprecated The returned ValidationReport is planned to be moved to a different package and this method is
	 *             planned to return that class.
	 */
	@Deprecated
	public ValidationReport getValidationReport() {
		return validationReport;
	}
}
