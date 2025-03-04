/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lucene.config;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.CONFIG;
import org.eclipse.rdf4j.sail.lucene.LuceneSail;

/**
 * Defines constants for the LuceneSail schema which is used by
 * {@link org.eclipse.rdf4j.sail.lucene.config.LuceneSailFactory}s to initialize {@link LuceneSail}s.
 *
 * @deprecated use {@link CONFIG.Lucene} instead.
 */
@Deprecated(since = "4.3.0", forRemoval = true)
public class LuceneSailConfigSchema {

	/**
	 * The LuceneSail schema namespace ( <var>http://www.openrdf.org/config/sail/lucene#</var>).
	 */
	public static final String NAMESPACE = "http://www.openrdf.org/config/sail/lucene#";

	/**
	 * @deprecated use {@link CONFIG.Lucene#indexDir} instead.
	 */
	public static final IRI INDEX_DIR;

	static {
		ValueFactory factory = SimpleValueFactory.getInstance();
		INDEX_DIR = factory.createIRI(NAMESPACE, "indexDir");
	}
}
