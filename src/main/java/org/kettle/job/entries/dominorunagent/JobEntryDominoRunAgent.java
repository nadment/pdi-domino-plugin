package org.kettle.job.entries.dominorunagent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.kettle.core.database.DominoDatabaseConnection;
import org.pentaho.di.cluster.SlaveServer;
import org.pentaho.di.core.CheckResultInterface;
import org.pentaho.di.core.Result;
import org.pentaho.di.core.annotations.JobEntry;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleDatabaseException;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleXMLException;
import org.pentaho.di.core.variables.VariableSpace;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.job.JobMeta;
import org.pentaho.di.job.entry.JobEntryBase;
import org.pentaho.di.job.entry.JobEntryInterface;
import org.pentaho.di.job.entry.validator.AndValidator;
import org.pentaho.di.job.entry.validator.JobEntryValidatorUtils;
import org.pentaho.di.repository.ObjectId;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.resource.ResourceEntry;
import org.pentaho.di.resource.ResourceEntry.ResourceType;
import org.pentaho.di.resource.ResourceReference;
import org.pentaho.metastore.api.IMetaStore;
import org.w3c.dom.Node;

import lotus.domino.Agent;

/**
 * Run IBM Domino agent on server
 * 
 * @author Nicolas ADMENT
 *
 */

@JobEntry(id = "DominoRunAgent", image = "dominorunagent.svg", i18nPackageName = "org.kettle.job.entries.dominorunagent", name = "JobEntryDominoRunAgent.Name", description = "JobEntryDominoRunAgent.Description", categoryDescription = "i18n:org.pentaho.di.job:JobCategory.Category.Utility")

public class JobEntryDominoRunAgent extends JobEntryBase implements Cloneable, JobEntryInterface {

	private static final Class<?> PKG = JobEntryDominoRunAgent.class;

	private static final String TAG_CONNECTION = "connection"; //$NON-NLS-1$
	private static final String TAG_ID_DATABASE = "id_database"; //$NON-NLS-1$
	private static final String TAG_AGENT = "agent"; //$NON-NLS-1$

	private DatabaseMeta database;
	private String agentName;

	public JobEntryDominoRunAgent() {
		this("", "");
	}

	public JobEntryDominoRunAgent(String name, String description) {
		super(name, description);

		agentName = null;
	}

	@Override
	public Object clone() {
		JobEntryDominoRunAgent cmd = (JobEntryDominoRunAgent) super.clone();
		return cmd;
	}

	@Override
	public Result execute(Result previousResult, int nr) throws KettleException {

		Result result = previousResult;
		result.setNrErrors(1);
		result.setResult(false);

		if (isBasic()) {
			logBasic(BaseMessages.getString(PKG, "JobEntryDominoRunAgent.Log.Started"));
		}

		// Resolve variables
		String name = environmentSubstitute(agentName);

		// Create database connection
		try (DominoDatabaseConnection connection = new DominoDatabaseConnection(database)) {

			// Search agent
			Agent agent = connection.getAgent(name);
			if (agent != null) {

				if (isBasic()) {
					logBasic(BaseMessages.getString(PKG, "JobEntryDominoRunAgent.Log.RunAgent"), connection, name);
				}

				// Run agent on server
				int success = agent.runOnServer();
				if (success == 0) {
					logBasic(BaseMessages.getString(PKG, "JobEntryDominoRunAgent.Log.RunAgentSuccess", connection,
							agentName));

					result.setResult(true);
				} else {
					logError(BaseMessages.getString(PKG, "JobEntryDominoRunAgent.Log.RunAgentFailed", connection,
							agentName));
				}
			} else {
				logError(BaseMessages.getString(PKG, "JobEntryDominoRunAgent.Log.AgentNotfound", connection, name));
			}
		} catch (Exception e) {
			logError(BaseMessages.getString(PKG, "JobEntryDominoRunAgent.Log.RunAgentException", database, name), e);
		}

		return result;
	}

	/**
	 * @return Returns the database.
	 */
	public DatabaseMeta getDatabase() {
		return database;
	}

	/**
	 * @param database
	 *            The database to set.
	 */
	public void setDatabase(final DatabaseMeta database) {
		this.database = database;
	}

	/**
	 * The agent to run on the Domino server
	 * 
	 * @return
	 */
	public String getAgent() {
		return agentName;
	}

	public void setAgent(final String agent) {
		this.agentName = agent;
	}

	@Override
	public String getXML() {
		StringBuilder xml = new StringBuilder(100);

		xml.append(super.getXML());
		xml.append(XMLHandler.addTagValue(TAG_AGENT, agentName));
		xml.append(XMLHandler.addTagValue(TAG_CONNECTION, database == null ? null : database.getName()));

		return xml.toString();
	}

	@Override
	public void loadXML(Node entrynode, List<DatabaseMeta> databases, List<SlaveServer> slaveServers, Repository rep,
			IMetaStore metaStore) throws KettleXMLException {
		try {
			super.loadXML(entrynode, databases, slaveServers);

			String dbname = XMLHandler.getTagValue(entrynode, TAG_CONNECTION);
			database = DatabaseMeta.findDatabase(databases, dbname);

			agentName = XMLHandler.getTagValue(entrynode, TAG_AGENT);

		} catch (Exception xe) {
			throw new KettleXMLException(
					BaseMessages.getString(PKG, "JobEntryDominoRunAgent.Exception.UnableToReadXML"), xe);
		}
	}

	@Override
	public void loadRep(Repository repository, IMetaStore metaStore, ObjectId id_jobentry, List<DatabaseMeta> databases,
			List<SlaveServer> slaveServers) throws KettleException {
		try {
			agentName = repository.getJobEntryAttributeString(id_jobentry, TAG_AGENT);
			database = repository.loadDatabaseMetaFromJobEntryAttribute(id_jobentry, TAG_CONNECTION, TAG_ID_DATABASE,
					databases);
		} catch (Exception dbe) {
			throw new KettleException(
					BaseMessages.getString(PKG, "JobEntryDominoRunAgent.Exception.UnableToReadRepository", id_jobentry),
					dbe);

		}
	}

	@Override
	public void saveRep(Repository repository, IMetaStore metaStore, ObjectId id_job) throws KettleException {
		try {
			repository.saveJobEntryAttribute(id_job, getObjectId(), TAG_AGENT, agentName);
			repository.saveDatabaseMetaJobEntryAttribute(id_job, getObjectId(), TAG_CONNECTION, TAG_ID_DATABASE,
					database);
		} catch (Exception dbe) {
			throw new KettleException(
					BaseMessages.getString(PKG, "JobEntryDominoRunAgent.Exception.UnableToSaveRepository", id_job),
					dbe);
		}
	}

	@Override
	public boolean evaluates() {
		return true;
	}

	@Override
	public boolean isUnconditional() {
		return true;
	}

	@Override
	public DatabaseMeta[] getUsedDatabaseConnections() {
		return new DatabaseMeta[] { database, };
	}

	@Override
	public List<ResourceReference> getResourceDependencies(JobMeta jobMeta) {

		List<ResourceReference> references = super.getResourceDependencies(jobMeta);
		if (database != null) {
			ResourceReference reference = new ResourceReference(this);
			reference.getEntries().add(new ResourceEntry(database.getHostname(), ResourceType.SERVER));
			reference.getEntries().add(new ResourceEntry(database.getDatabaseName(), ResourceType.DATABASENAME));
			references.add(reference);
		}
		return references;
	}

	@Override
	public void check(List<CheckResultInterface> remarks, JobMeta jobMeta, VariableSpace space, Repository repository,
			IMetaStore metaStore) {
		JobEntryValidatorUtils.andValidator().validate(this, TAG_AGENT, remarks,
				AndValidator.putValidators(JobEntryValidatorUtils.notBlankValidator()));
	}

	/**
	 * Build list of runnable agents.
	 * 
	 * @return
	 */
	public String[] getAgentNames() throws KettleDatabaseException {

		final ArrayList<String> result = new ArrayList<String>();

		try (DominoDatabaseConnection connection = new DominoDatabaseConnection(database)) {

			for (Agent agent : connection.getAgents()) {


				if ((agent.getTrigger() == Agent.TRIGGER_SCHEDULED) || (agent.getTrigger() == Agent.TRIGGER_NONE)
						|| (agent.getTrigger() == Agent.TRIGGER_SERVERSTART)) {
					// || (agent.getTrigger() == Agent.TRIGGER_MANUAL)) {

					String name = agent.getName();
					agent.recycle();
					
					int index = name.indexOf('|');
					if (index > 0) {
						name = name.substring(0, index);
					}
					result.add(name);
				}
			}
		} catch (Exception e) {
			throw new KettleDatabaseException("Unable to find availables agents", e);
		}

		Collections.sort(result);
		return result.toArray(new String[result.size()]);
	}

}
