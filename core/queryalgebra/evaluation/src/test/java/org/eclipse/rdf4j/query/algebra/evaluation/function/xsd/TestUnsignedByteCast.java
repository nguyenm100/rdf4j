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
package org.eclipse.rdf4j.query.algebra.evaluation.function.xsd;

import java.math.BigInteger;
import java.util.Optional;

public class TestUnsignedByteCast extends TestIntegerDatatypeCast<UnsignedByteCast> {

	@Override
	protected UnsignedByteCast getCastFunction() {
		return new UnsignedByteCast();
	}

	@Override
	protected Optional<BigInteger> getMaxValue() {
		return Optional.of(new BigInteger(String.valueOf((long) Math.pow(2, 8) - 1l)));
	}

	@Override
	protected Optional<BigInteger> getMinValue() {
		return Optional.of(BigInteger.ZERO);
	}
}
