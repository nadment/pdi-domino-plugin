package org.pentaho.di.core.database;

import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettleDatabaseException;

import lotus.domino.Database;
import lotus.domino.NotesException;
import lotus.domino.NotesFactory;
import lotus.domino.Session;

public class DominoDatabaseFactory implements DatabaseFactoryInterface {

	// Needed by the Database dialog
	public DominoDatabaseFactory() {
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

			// NotesThread.sinitThread();
			session = NotesFactory.createSession(db.getHostname(), db.getUsername(), db.getPassword());

			// If the connection was successful
			report.append("Connecting to IBM Domino server [").append(db.getHostname());
			report.append("] on platform ").append(session.getPlatform());
			report.append(" succeeded without a problem.").append(Const.CR);

			database = session.getDatabase(db.getHostname(), db.getDatabaseName());
			if (database.isOpen()) {
				report.append("Database ").append(database.getTitle()).append(" [").append(database.getFilePath());
				report.append("] open successfully").append(Const.CR);
			}

			database.recycle();
		} catch (Throwable e) {
			report.append("Unable to connect to the IBM Domino server: ").append(db.getHostname()).append(Const.CR)
					.append(Const.CR);
			report.append("os.name=" + System.getProperty("os.name")).append(Const.CR);
			report.append("os.arch=").append(System.getProperty("os.arch")).append(Const.CR);
			report.append("os.version=").append(System.getProperty("os.version")).append(Const.CR);
			report.append("java.version=").append(System.getProperty("java.version")).append(Const.CR);
			report.append("java.class.path=").append(System.getProperty("java.class.path")).append(Const.CR);
			report.append("java.library.path=").append(System.getProperty("java.library.path")).append(Const.CR);
			report.append(Const.getStackTracker(e));
		} finally {
			// NotesThread.stermThread();

			if (session != null) {
				try {
					session.recycle();
				} catch (NotesException e) {
				}
			}
		}

		return report.toString();
	}
}
