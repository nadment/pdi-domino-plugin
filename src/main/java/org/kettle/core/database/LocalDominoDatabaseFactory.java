package org.kettle.core.database;

import org.pentaho.di.core.Const;
import org.pentaho.di.core.database.DatabaseFactoryInterface;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleDatabaseException;

import lotus.domino.Database;
import lotus.domino.NotesException;
import lotus.domino.NotesFactory;
import lotus.domino.NotesThread;
import lotus.domino.Session;

public class LocalDominoDatabaseFactory implements DatabaseFactoryInterface {

	// Needed by the Database dialog
	public LocalDominoDatabaseFactory() {
		super();
	}

	/**
	 * The Domino connection to test
	 */
	public String getConnectionTestReport(final DatabaseMeta db) throws KettleDatabaseException {

		StringBuilder report = new StringBuilder();
		
		Session session = null;
		Database database = null;

		try {		
			NotesThread.sinitThread();
			session = NotesFactory.createSession(db.getHostname(), db.getUsername(), db.getPassword());

			// If the connection was successful
			report.append("Connecting with local client to IBM Domino server [").append(db.getHostname());
			report.append("] on platform ").append(session.getPlatform());
			report.append(" succeeded without a problem.").append(Const.CR);

			database = session.getDatabase(db.getHostname(), db.getDatabaseName());
			if (database.isOpen()) {
				report.append("Database ").append(database.getTitle()).append(" [").append(database.getFilePath());
				report.append("] open successfully").append(Const.CR);
			}

			database.recycle();
		} catch (Throwable e) {
			throw new KettleDatabaseException("Unable to connect with local client to the IBM Domino server: "+db.getHostname(), e.getCause());
		} finally {


			if (session != null) {
				try {
					session.recycle();
				} catch (NotesException e) {
				}
			}
			
			NotesThread.stermThread();
		}

		return report.toString();
	}
}
