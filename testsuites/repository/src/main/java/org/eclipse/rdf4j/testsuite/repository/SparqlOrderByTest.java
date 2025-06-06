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
package org.eclipse.rdf4j.testsuite.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public abstract class SparqlOrderByTest {

	@BeforeAll
	public static void setUpClass() {
		System.setProperty("org.eclipse.rdf4j.repository.debug", "true");
	}

	@AfterAll
	public static void afterClass() throws Exception {
		System.setProperty("org.eclipse.rdf4j.repository.debug", "false");
	}

	private final String query1 = "PREFIX foaf:    <http://xmlns.com/foaf/0.1/>\n" + "SELECT ?name\n"
			+ "WHERE { ?x foaf:name ?name }\n" + "ORDER BY ?name\n";

	private final String query2 = "PREFIX     :    <http://example.org/ns#>\n"
			+ "PREFIX foaf:    <http://xmlns.com/foaf/0.1/>\n" + "PREFIX xsd:     <http://www.w3.org/2001/XMLSchema#>\n"
			+ "SELECT ?name\n" + "WHERE { ?x foaf:name ?name ; :empId ?emp }\n" + "ORDER BY DESC(?emp)\n";

	private final String query3 = "PREFIX     :    <http://example.org/ns#>\n"
			+ "PREFIX foaf:    <http://xmlns.com/foaf/0.1/>\n" + "SELECT ?name\n"
			+ "WHERE { ?x foaf:name ?name ; :empId ?emp }\n" + "ORDER BY ?name DESC(?emp)\n";

	private Repository repository;

	private RepositoryConnection conn;

	@Test
	public void testQuery1() {
		assertTrue("James Leigh".compareTo("James Leigh Hunt") < 0);
		assertResult(query1, Arrays.asList("James Leigh", "James Leigh", "James Leigh Hunt", "Megan Leigh"));
	}

	@Test
	public void testQuery2() {
		assertResult(query2, Arrays.asList("Megan Leigh", "James Leigh", "James Leigh Hunt", "James Leigh"));
	}

	@Test
	public void testQuery3() {
		assertResult(query3, Arrays.asList("James Leigh", "James Leigh", "James Leigh Hunt", "Megan Leigh"));
	}

	@BeforeEach
	public void setUp() {
		repository = createRepository();
		createEmployee("james", "James Leigh", 123);
		createEmployee("jim", "James Leigh", 244);
		createEmployee("megan", "Megan Leigh", 1234);
		createEmployee("hunt", "James Leigh Hunt", 243);
		conn = repository.getConnection();
	}

	protected Repository createRepository() {
		Repository repository = newRepository();
		try (RepositoryConnection con = repository.getConnection()) {
			con.clear();
			con.clearNamespaces();
		}
		return repository;
	}

	protected abstract Repository newRepository();

	@AfterEach
	public void tearDown() {
		conn.close();
		conn = null;

		repository.shutDown();
		repository = null;
	}

	private void createEmployee(String id, String name, int empId) throws RepositoryException {
		ValueFactory vf = repository.getValueFactory();
		String foafName = "http://xmlns.com/foaf/0.1/name";
		String exEmpId = "http://example.org/ns#empId";
		RepositoryConnection conn = repository.getConnection();
		conn.add(vf.createIRI("http://example.org/ns#" + id), vf.createIRI(foafName), vf.createLiteral(name));
		conn.add(vf.createIRI("http://example.org/ns#" + id), vf.createIRI(exEmpId), vf.createLiteral(empId));
		conn.close();
	}

	private void assertResult(String queryStr, List<String> names)
			throws RepositoryException, MalformedQueryException, QueryEvaluationException {
		TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryStr);
		TupleQueryResult result = query.evaluate();
		for (String name : names) {
			Value value = result.next().getValue("name");
			assertEquals(name, ((Literal) value).getLabel());
		}
		assertFalse(result.hasNext());
		result.close();
	}
}
