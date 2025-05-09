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
package org.eclipse.rdf4j.rio.hdt;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.CheckedInputStream;

import org.eclipse.rdf4j.common.io.UncloseableInputStream;

/**
 * HDT Dictionary Part.
 * <p>
 * This part starts with <code>$HDT</code>, followed by a byte indicating the type of the part, the NULL-terminated URI
 * string for the format, and optionally one or more <code>key=value;</code> properties.
 * <p>
 * Then a <code>NULL</code> byte, followed by the 16-bit CRC (<code>$HDT</code> and <code>NULL</code> included).
 * <p>
 * Structure:
 *
 * <pre>
 * +------+------+-----+------+------------+------+-------+
 * | $HDT | type | URI | NULL | key=value; | NULL | CRC16 |
 * +------+------+-----+------+------------+------+-------+
 * </pre>
 *
 * @author Bart Hanssens
 */
class HDTDictionary extends HDTPart {
	final static byte[] DICT_FORMAT = "<http://purl.org/HDT/hdt#dictionaryFour>"
			.getBytes(StandardCharsets.US_ASCII);
	final static String DICT_MAPPING = "mapping";
	final static String DICT_ELEMENTS = "elements";

	@Override
	void parse(InputStream is) throws IOException {
		// don't close CheckedInputStream, as it will close the underlying inputstream
		try (UncloseableInputStream uis = new UncloseableInputStream(is);
				CheckedInputStream cis = new CheckedInputStream(uis, new CRC16())) {

			checkControl(cis, HDTPart.Type.DICTIONARY);
			checkFormat(cis, DICT_FORMAT);

			properties = getProperties(cis);

			checkCRC(cis, is, 2);
		}
	}
}
