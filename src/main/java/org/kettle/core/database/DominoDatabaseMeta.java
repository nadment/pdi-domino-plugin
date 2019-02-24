package org.kettle.core.database;

import org.eclipse.jface.wizard.WizardPage;
import org.kettle.ui.core.database.wizard.domino.CreateDatabaseWizardPageDomino;
import org.pentaho.di.core.database.BaseDatabaseMeta;
import org.pentaho.di.core.database.DatabaseInterface;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.plugins.DatabaseMetaPlugin;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.ui.core.PropsUI;
import org.pentaho.di.ui.core.database.wizard.WizardPageFactory;

/**
 * --------------------------------------------------------------------------------------
 * Ensuring That the Domino IIOP (DIIOP) Task Is Running
 * --------------------------------------------------------------------------------------
 * You must ensure that the Domino IIOP (DIIOP) task is running. To do this,
 * open the IBM Lotus Notes and Domino console and run the Load DIIOP command.
 * If the DIIOP task were not running, then it is started after you run the
 * command. If it were running, then a message that the task has already been
 * started is displayed.
 * 
 * @author Nicolas ADMENT
 *
 */

@DatabaseMetaPlugin(type = "DOMINO", typeDescription = "IBM Domino Database")
public class DominoDatabaseMeta extends BaseDatabaseMeta implements DatabaseInterface, WizardPageFactory {

	public DominoDatabaseMeta() {
		super();

		this.addAttribute(ATTRIBUTE_USE_LOCAL_CLIENT, "False");
	}

	public static final String ATTRIBUTE_USE_LOCAL_CLIENT = "DominoUseLocalClient";

	public static final String ATTRIBUTE_REPLICA_ID = "ReplicaID";

	public static final String DOMINO = "DOMINO";

	/** The default DIIOP port */
	public static final int DEFAULT_PORT = 63148;

	@Override
	public int[] getAccessTypeList() {
		return new int[] { DatabaseMeta.TYPE_ACCESS_PLUGIN };
	}

	@Override
	public int getDefaultDatabasePort() {
		return DEFAULT_PORT;
	}

	/**
	 * @return Whether or not the database can use auto increment type of fields
	 *         (pk)
	 */
	@Override
	public boolean supportsAutoInc() {
		return false;
	}

	@Override
	public String getDriverClass() {
		return null;
	}

	@Override
	public String getURL(String hostname, String port, String databaseName) {
		return null;
	}

	/**
	 * Generates the SQL statement to add a column to the specified table
	 *
	 * @param tablename
	 *            The table to add
	 * @param v
	 *            The column defined as a value
	 * @param tk
	 *            the name of the technical key field
	 * @param use_autoinc
	 *            whether or not this field uses auto increment
	 * @param pk
	 *            the name of the primary key field
	 * @param semicolon
	 *            whether or not to add a semi-colon behind the statement.
	 * @return the SQL statement to add a column to the specified table
	 */
	@Override
	public String getAddColumnStatement(String tablename, ValueMetaInterface v, String tk, boolean use_autoinc,
			String pk, boolean semicolon) {
		return null;
	}

	/**
	 * Generates the SQL statement to modify a column in the specified table
	 *
	 * @param tablename
	 *            The table to add
	 * @param v
	 *            The column defined as a value
	 * @param tk
	 *            the name of the technical key field
	 * @param use_autoinc
	 *            whether or not this field uses auto increment
	 * @param pk
	 *            the name of the primary key field
	 * @param semicolon
	 *            whether or not to add a semi-colon behind the statement.
	 * @return the SQL statement to modify a column in the specified table
	 */
	@Override
	public String getModifyColumnStatement(String tablename, ValueMetaInterface v, String tk, boolean use_autoinc,
			String pk, boolean semicolon) {
		return null;
	}

	@Override
	public String getFieldDefinition(ValueMetaInterface v, String tk, String pk, boolean use_autoinc,
			boolean add_fieldname, boolean add_cr) {
		return null;
	}

	@Override
	public String[] getReservedWords() {
		return null;
	}

	@Override
	public String[] getUsedLibraries() {
		if (this.isUseLocalClient())
			return new String[] { "Notes.jar" };
		else
			return new String[] { "NCSO.jar" };
	}

	@Override
	public String getDatabaseFactoryName() {
		return org.kettle.core.database.DominoDatabaseFactory.class.getName();
	}

	@Override
	public boolean isExplorable() {
		return false;
	}

	public boolean isUseLocalClient() {
		// return Utils.isEmpty(this.getHostname());

		Object value = getAttributes().get(ATTRIBUTE_USE_LOCAL_CLIENT);
		if (value != null && value instanceof String) {

			// Check if the String can be parsed into a boolean
			try {
				Boolean.parseBoolean((String) value);
			} catch (IllegalArgumentException e) {
				// Ignore
			}
		}

		return false;
	}

	@Override
	public boolean canTest() {
		return true;
	}

	@Override
	public boolean requiresName() {
		return true;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		sb.append(this.getDatabaseName());
		sb.append("] on ");
		if (this.isUseLocalClient()) {
			sb.append("local");
		} else {
			sb.append("serveur ");
			sb.append(this.getHostname());
		}
		return sb.toString();
	}

	@Override
	public WizardPage createWizardPage(PropsUI props, DatabaseMeta info) {
		return new CreateDatabaseWizardPageDomino(DOMINO, props, info);
	}

	@Override
	public boolean supportsBitmapIndex() {
		return false;
	}

	@Override
	public boolean supportsSynonyms() {
		return false;
	}

	@Override
	public boolean supportsBatchUpdates() {
		return false;
	}

}
