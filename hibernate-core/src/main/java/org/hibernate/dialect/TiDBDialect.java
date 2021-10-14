/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import org.hibernate.LockOptions;
import org.hibernate.MappingException;
import org.hibernate.dialect.sequence.MariaDBSequenceSupport;
import org.hibernate.dialect.sequence.SequenceSupport;
import org.hibernate.dialect.sequence.TiDBSequenceSupport;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorTiDBDatabaseImpl;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;

import java.sql.DatabaseMetaData;
import java.time.Duration;

public class TiDBDialect extends MySQLDialect {

	public TiDBDialect() {
		super(570);

		registerKeywords();
	}

	public TiDBDialect(DialectResolutionInfo info) {
		this();
	}

	private static final String QUERY_SEQUENCES_STRING =
			"SELECT sequence_name FROM information_schema.sequences WHERE sequence_schema = database()";

	private void registerKeywords() {
		// TiDB implemented 'Window Functions' of MySQL 8, so the following keywords are reserved.
		registerKeyword("CUME_DIST");
		registerKeyword("DENSE_RANK");
		registerKeyword("EXCEPT");
		registerKeyword("FIRST_VALUE");
		registerKeyword("GROUPS");
		registerKeyword("LAG");
		registerKeyword("LAST_VALUE");
		registerKeyword("LEAD");
		registerKeyword("NTH_VALUE");
		registerKeyword("NTILE");
		registerKeyword("PERCENT_RANK");
		registerKeyword("RANK");
		registerKeyword("ROW_NUMBER");
	}

	@Override
	public SequenceSupport getSequenceSupport() {
		return TiDBSequenceSupport.INSTANCE;
	}

	@Override
	public boolean supportsCascadeDelete() {
		return false;
	}

	@Override
	public String getQuerySequencesString() {
		return QUERY_SEQUENCES_STRING;
	}

	@Override
	public SequenceInformationExtractor getSequenceInformationExtractor() {
		return SequenceInformationExtractorTiDBDatabaseImpl.INSTANCE;
	}

	@Override
	public String getWriteLockString(int timeout) {
		if ( timeout == LockOptions.NO_WAIT ) {
			return getForUpdateNowaitString();
		}

		if ( timeout > 0 ) {
			return getForUpdateString() + " wait " + getLockWaitTimeoutInSeconds( timeout );
		}

		return getForUpdateString();
	}

	@Override
	public boolean supportsSkipLocked() {
		//only supported on MySQL
		return false;
	}

	@Override
	boolean supportsForShare() {
		//only supported on MySQL
		return false;
	}

	@Override
	boolean supportsAliasLocks() {
		//only supported on MySQL
		return false;
	}

	@Override
	public String getForUpdateNowaitString() {
		return getForUpdateString() + " nowait";
	}

	@Override
	public String getForUpdateNowaitString(String aliases) {
		return getForUpdateString( aliases ) + " nowait";
	}

	private static long getLockWaitTimeoutInSeconds(int timeoutInMilliseconds) {
		Duration duration = Duration.ofMillis( timeoutInMilliseconds );
		return duration.getSeconds();
	}

}
