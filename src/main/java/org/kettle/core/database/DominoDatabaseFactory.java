package org.kettle.core.database;

import org.pentaho.di.core.Const;
import org.pentaho.di.core.database.DatabaseFactoryInterface;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleDatabaseException;

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
	
		try {
			
			DatabaseFactoryInterface factory = null;
			
			Object value = db.getAttributes().get(DominoDatabaseMeta.ATTRIBUTE_USE_LOCAL_CLIENT);
			
			boolean useLocalClient = false;
			if (value != null && value instanceof String) {
						
				// Check if the String can be parsed into a boolean
				try {
					useLocalClient = Boolean.parseBoolean((String) value);
				} catch (IllegalArgumentException e) {
					// Ignore
				}
			}
			
			if ( useLocalClient ) 
				factory = new LocalDominoDatabaseFactory();
			else
				factory = new RemoteDominoDatabaseFactory();

			// If the connection was successful
			report.append(factory.getConnectionTestReport(db));
		} catch (UnsatisfiedLinkError e) {
			throw new KettleDatabaseException("Local Notes Client not accessible", e);
		} catch (NoClassDefFoundError e) {
			throw new KettleDatabaseException("Library not found, classpath must include Notes.jar (local) or NCSO.jar (remote)", e);			
		} catch (Throwable e) {		
			report.append(e.getMessage());
			//report.append(Const.getStackTracker(e));
		} finally {
			report.append(Const.CR);
			report.append("os.name=" + System.getProperty("os.name")).append(Const.CR);
			report.append("os.arch=").append(System.getProperty("os.arch")).append(Const.CR);
			report.append("os.version=").append(System.getProperty("os.version")).append(Const.CR);
			report.append("java.version=").append(System.getProperty("java.version")).append(Const.CR);
			report.append("java.class.path=").append(System.getProperty("java.class.path")).append(Const.CR);
			report.append("java.library.path=").append(System.getProperty("java.library.path")).append(Const.CR);
		}

		return report.toString();
	}
}
