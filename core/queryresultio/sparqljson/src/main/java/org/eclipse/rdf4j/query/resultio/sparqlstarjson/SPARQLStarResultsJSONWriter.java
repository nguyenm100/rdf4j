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
package org.eclipse.rdf4j.query.resultio.sparqlstarjson;

import java.io.OutputStream;

/**
 * @deprecated Moved to {@link org.eclipse.rdf4j.query.resultio.sparqljson.SPARQLStarResultsJSONWriter}
 */
@Deprecated(since = "3.4.0")
public class SPARQLStarResultsJSONWriter
		extends org.eclipse.rdf4j.query.resultio.sparqljson.SPARQLStarResultsJSONWriter {

	public SPARQLStarResultsJSONWriter(OutputStream out) {
		super(out);
	}

}
