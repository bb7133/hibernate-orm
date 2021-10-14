/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.sequence;

import org.hibernate.MappingException;

/**
 * Sequence support for {@link org.hibernate.dialect.TiDBDialect}.
 *
 * @author Cong Wang
 */
public final class TiDBSequenceSupport extends ANSISequenceSupport {

	public static final SequenceSupport INSTANCE = new TiDBSequenceSupport();

	@Override
	public String getSelectSequencePreviousValString(String sequenceName) throws MappingException {
		return "LASTVAL(" + sequenceName + ")";
	}

	@Override
	public boolean sometimesNeedsStartingValue() {
		return true;
	}
}
