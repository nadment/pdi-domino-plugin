package org.kettle.trans.steps.dominoinput;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.kettle.core.database.DominoDatabaseConnection;
import org.pentaho.di.core.CheckResult;
import org.pentaho.di.core.CheckResultInterface;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.annotations.Step;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleDatabaseException;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleStepException;
import org.pentaho.di.core.exception.KettleXMLException;
import org.pentaho.di.core.injection.Injection;
import org.pentaho.di.core.injection.InjectionDeep;
import org.pentaho.di.core.injection.InjectionSupported;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.row.value.ValueMetaFactory;
import org.pentaho.di.core.util.Utils;
import org.pentaho.di.core.variables.VariableSpace;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.repository.ObjectId;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.shared.SharedObjectInterface;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStepMeta;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;
import org.pentaho.metastore.api.IMetaStore;
import org.w3c.dom.Node;

import lotus.domino.Document;
import lotus.domino.DocumentCollection;
import lotus.domino.Item;
import lotus.domino.View;

@Step(id = "DominoInput", name = "DominoInput.Name", description = "DominoInput.Description", image = "dominoinput.svg", i18nPackageName = "org.kettle.trans.steps.domino", categoryDescription = "i18n:org.pentaho.di.trans.step:BaseStep.Category.Input")

@InjectionSupported(localizationPrefix = "DominoInputMeta.Injection.", groups = { "FIELDS" })

public class DominoInputMeta extends BaseStepMeta implements StepMetaInterface {

	/**
	 * The package name used for internationalization
	 */
	private static final Class<?> PKG = DominoInputMeta.class; // for i18n purposes

	private static final String TAG_ID_CONNECTION = "id_connection"; //$NON-NLS-1$
	private static final String TAG_CONNECTION = "connection"; //$NON-NLS-1$
	private static final String TAG_MODE = "mode"; //$NON-NLS-1$
	private static final String TAG_VIEW = "view"; //$NON-NLS-1$
	private static final String TAG_SEARCH = "search"; //$NON-NLS-1$
	private static final String TAG_FIELD_NAME = "name"; //$NON-NLS-1$
	private static final String TAG_FIELD_FORMULA = "formula"; //$NON-NLS-1$
	private static final String TAG_FIELD_TYPE = "type"; //$NON-NLS-1$
	private static final String TAG_FIELD_FORMAT = "format"; //$NON-NLS-1$
	private static final String TAG_FIELD_CURRENCY_SYMBOL = "currency"; //$NON-NLS-1$
	private static final String TAG_FIELD_DECIMAL_SYMBOL = "decimal"; //$NON-NLS-1$
	private static final String TAG_FIELD_GROUP_SYMBOL = "group"; //$NON-NLS-1$
	private static final String TAG_FIELD_LENGTH = "length"; //$NON-NLS-1$
	private static final String TAG_FIELD_PRECISION = "precision"; //$NON-NLS-1$
	private static final String TAG_FIELD_TRIM_TYPE = "trim_type"; //$NON-NLS-1$

	private List<? extends SharedObjectInterface> databases;

	/** The database connection */
	private DatabaseMeta database;

	/** The view name */
	@Injection(name = "VIEW")
	private String view;

	/** The search document selection */
	@Injection(name = "SEARCH")
	private String search;

	/** The view or search document selection */
	@Injection(name = "MODE")
	private DominoInputMode mode;

	/** The fields to retrieve from domino view or document */
	@InjectionDeep
	private DominoField[] fields;

	public DominoInputMeta() {
		super();
	}

	@Override
	public StepInterface getStep(StepMeta stepMeta, StepDataInterface stepData, int cnr, TransMeta transMeta,
			Trans disp) {
		return new DominoInputStep(stepMeta, stepData, cnr, transMeta, disp);
	}

	@Override
	public StepDataInterface getStepData() {
		return new DominoInputData();
	}

	@Override
	public void setDefault() {
		database = null;
		view = "";
		mode = DominoInputMode.VIEW;
		search = "@ALL";
		fields = new DominoField[0];
	}

	/**
	 * @return Returns the database.
	 */
	public DatabaseMeta getDatabaseMeta() {
		return database;
	}

	/**
	 * @param database
	 *            The database to set.
	 */
	public void setDatabaseMeta(final DatabaseMeta database) {
		this.database = database;
	}

	@Injection(name = "CONNECTIONNAME")
	public void setConnection(String connectionName) {
		database = DatabaseMeta.findDatabase(this.databases, connectionName);
	}

	@Override
	public DatabaseMeta[] getUsedDatabaseConnections() {
		if (database != null) {
			return new DatabaseMeta[] { database };
		} else {
			return super.getUsedDatabaseConnections();
		}
	}

	public DominoInputMode getMode() {
		return mode;
	}

	public void setMode(final DominoInputMode mode) {
		this.mode = mode;
	}

	/**
	 * 
	 * 
	 * @return the Domino view name
	 */
	public String getView() {
		return view;
	}

	/**
	 * @param view
	 *            the view name to set
	 */
	public void setView(final String name) {
		this.view = name;
	}

	public List<String> getViewNames() throws KettleDatabaseException {
		List<String> names = new ArrayList<String>();

		try (DominoDatabaseConnection connection = new DominoDatabaseConnection(database)) {
			for (View view : connection.getViews()) {
				names.add(view.getName());
				view.recycle();
			}
		} catch (Throwable e) {
			throw new KettleDatabaseException(
					BaseMessages.getString(PKG, "DominoInputMeta.Exception.UnableToFindAvailableViews", database), e);
		}

		Collections.sort(names);

		return names;
	}

	public List<DominoField> getAvailableViewColumns(final String viewName) throws KettleDatabaseException {
		List<DominoField> fields = new ArrayList<>();

		if (database != null && !Utils.isEmpty(view)) {

			try (DominoDatabaseConnection connection = new DominoDatabaseConnection(database)) {

				// TODO: View view =
				// connection.getView(environmentSubstitute(viewName));
				View view = connection.getView(viewName);
				if (view != null) {
					for (Object column : view.getColumnNames()) {
						if (!Utils.isEmpty(column.toString())) {

							DominoField field = new DominoField();
							field.setName(column.toString());
							field.setType(ValueMetaInterface.TYPE_STRING);

							fields.add(field);
						}
					}

					view.recycle();
				}
			} catch (Throwable e) {
				throw new KettleDatabaseException(
						BaseMessages.getString(PKG, "DominoInputMeta.Exception.UnableToFindsAvailableFields", viewName),
						e);
			}

			// Collections.sort(fields);
		}

		return fields;

	}

	public List<DominoField> getAvailableDocumentItems(final String searchFormula) throws KettleDatabaseException {
		List<DominoField> fields = new ArrayList<>();

		if (database != null && !Utils.isEmpty(searchFormula)) {

			try (DominoDatabaseConnection connection = new DominoDatabaseConnection(database)) {

				// TODO: View view =
				// connection.getView(environmentSubstitute(viewName));
				DocumentCollection collection = connection.getDatabase().search(searchFormula);

				if (collection != null) {
					Document doc = collection.getFirstDocument();

					if (doc != null) {
						for (Object object : doc.getItems()) {
							Item item = (Item) object;

							DominoField field = new DominoField();
							field.setName(item.getName());
							field.setType(ValueMetaInterface.TYPE_STRING);

							fields.add(field);
							item.recycle();
						}

						doc.recycle();
					}

					collection.recycle();
				}
			} catch (Throwable e) {
				throw new KettleDatabaseException(BaseMessages.getString(PKG,
						"DominoInputMeta.Exception.UnableToFindsAvailableFields", searchFormula), e);
			}

			// Collections.sort(itemNames);
		}

		return fields;
	}

	/**
	 * 
	 * 
	 * @return the Notes @function formula that defines the selection criteria
	 */
	public String getSearch() {
		return search;
	}

	/**
	 * @param search
	 *            the Notes @function formula that defines the selection
	 *            criteria
	 */
	public void setSearch(final String search) {
		this.search = search;
	}

	@Override
	public void loadXML(Node stepNode, List<DatabaseMeta> databases, IMetaStore metaStore) throws KettleXMLException {

		this.databases = databases;

		try {
			this.database = DatabaseMeta.findDatabase(databases, XMLHandler.getTagValue(stepNode, TAG_CONNECTION));

			// if ( XMLHandler.getTagValue(stepNode, TAG_MODE)==null ) this.mode
			// = DominoInputMode.VIEW;
			// else

			this.mode = DominoInputMode.valueOf(XMLHandler.getTagValue(stepNode, TAG_MODE));
			this.view = XMLHandler.getTagValue(stepNode, TAG_VIEW);
			this.search = XMLHandler.getTagValue(stepNode, TAG_SEARCH);

			Node nodeFields = XMLHandler.getSubNode(stepNode, "fields");
			int count = XMLHandler.countNodes(nodeFields, "field");

			fields = new DominoField[count];
			for (int i = 0; i < fields.length; i++) {
				Node line = XMLHandler.getSubNodeByNr(nodeFields, "field", i);

				DominoField field = new DominoField();
				field.setName(Const.NVL(XMLHandler.getTagValue(line, TAG_FIELD_NAME), ""));
				field.setFormula(Const.NVL(XMLHandler.getTagValue(line, TAG_FIELD_FORMULA), ""));
				field.setType(Const.NVL(XMLHandler.getTagValue(line, TAG_FIELD_TYPE), ""));
				field.setFormat(Const.NVL(XMLHandler.getTagValue(line, TAG_FIELD_FORMAT), ""));
				field.setCurrencySymbol(Const.NVL(XMLHandler.getTagValue(line, TAG_FIELD_CURRENCY_SYMBOL), ""));
				field.setGroupSymbol(Const.NVL(XMLHandler.getTagValue(line, TAG_FIELD_GROUP_SYMBOL), ""));
				field.setDecimalSymbol(Const.NVL(XMLHandler.getTagValue(line, TAG_FIELD_DECIMAL_SYMBOL), ""));
				field.setLength(Const.toInt(XMLHandler.getTagValue(line, TAG_FIELD_LENGTH), -1));
				field.setPrecision(Const.toInt(XMLHandler.getTagValue(line, TAG_FIELD_PRECISION), -1));
				field.setTrimTypeCode(Const.NVL(XMLHandler.getTagValue(line, TAG_FIELD_TRIM_TYPE), ""));

				fields[i] = field;
			}
		} catch (Exception e) {
			throw new KettleXMLException(
					BaseMessages.getString(PKG, "DominoInputMeta.Exception.UnableToReadStepInfoFromXML"), e);
		}
	}

	@Override
	public String getXML() {
		StringBuilder xml = new StringBuilder();

		xml.append(XMLHandler.addTagValue(TAG_CONNECTION, database == null ? "" : database.getName()));
		xml.append(XMLHandler.addTagValue(TAG_MODE, mode.name()));
		xml.append(XMLHandler.addTagValue(TAG_VIEW, view));
		xml.append(XMLHandler.addTagValue(TAG_SEARCH, search));
		xml.append("<fields>");
		for (DominoField field : this.fields) {
			xml.append("<field>");
			xml.append(XMLHandler.addTagValue(TAG_FIELD_NAME, field.getName()));
			xml.append(XMLHandler.addTagValue(TAG_FIELD_FORMULA, field.getFormula()));
			xml.append(XMLHandler.addTagValue(TAG_FIELD_TYPE, ValueMetaFactory.getValueMetaName(field.getType())));
			xml.append(XMLHandler.addTagValue(TAG_FIELD_FORMAT, field.getFormat()));
			xml.append(XMLHandler.addTagValue(TAG_FIELD_CURRENCY_SYMBOL, field.getCurrencySymbol()));
			xml.append(XMLHandler.addTagValue(TAG_FIELD_GROUP_SYMBOL, field.getGroupSymbol()));
			xml.append(XMLHandler.addTagValue(TAG_FIELD_DECIMAL_SYMBOL, field.getDecimalSymbol()));
			xml.append(XMLHandler.addTagValue(TAG_FIELD_LENGTH, field.getLength()));
			xml.append(XMLHandler.addTagValue(TAG_FIELD_PRECISION, field.getPrecision()));
			xml.append(XMLHandler.addTagValue(TAG_FIELD_TRIM_TYPE, field.getTrimTypeCode()));
			xml.append("</field>");
		}
		xml.append("</fields>");

		return xml.toString();
	}

	@Override
	public void readRep(Repository repository, IMetaStore metaStore, ObjectId id_step, List<DatabaseMeta> databases)
			throws KettleException {

		this.databases = databases;

		try {

			database = repository.loadDatabaseMetaFromStepAttribute(id_step, TAG_ID_CONNECTION, databases);
			mode = DominoInputMode.valueOf(repository.getStepAttributeString(id_step, TAG_MODE));
			view = repository.getStepAttributeString(id_step, TAG_VIEW);
			search = repository.getStepAttributeString(id_step, TAG_SEARCH);

			int count = repository.countNrStepAttributes(id_step, TAG_FIELD_NAME);
			fields = new DominoField[count];
			for (int i = 0; i < fields.length; i++) {
				DominoField field = new DominoField();
				field.setName(repository.getStepAttributeString(id_step, i, TAG_FIELD_NAME));
				field.setFormula(repository.getStepAttributeString(id_step, i, TAG_FIELD_FORMULA));
				field.setType(repository.getStepAttributeString(id_step, i, TAG_FIELD_TYPE));
				field.setFormat(repository.getStepAttributeString(id_step, i, TAG_FIELD_FORMAT));
				field.setCurrencySymbol(repository.getStepAttributeString(id_step, i, TAG_FIELD_CURRENCY_SYMBOL));
				field.setGroupSymbol(repository.getStepAttributeString(id_step, i, TAG_FIELD_GROUP_SYMBOL));
				field.setDecimalSymbol(repository.getStepAttributeString(id_step, i, TAG_FIELD_DECIMAL_SYMBOL));
				field.setLength((int) repository.getStepAttributeInteger(id_step, i, TAG_FIELD_LENGTH));
				field.setPrecision((int) repository.getStepAttributeInteger(id_step, i, TAG_FIELD_PRECISION));
				field.setTrimTypeCode(repository.getStepAttributeString(id_step, i, TAG_FIELD_TRIM_TYPE));

				fields[i] = field;
			}
		} catch (Exception e) {
			throw new KettleException(
					BaseMessages.getString(PKG, "DominoInputMeta.Exception.UnableToReadRepository", id_step), e);
		}
	}

	@Override
	public void saveRep(Repository repository, IMetaStore metaStore, ObjectId id_transformation, ObjectId id_step)
			throws KettleException {
		try {

			// Save the database connection
			repository.saveDatabaseMetaStepAttribute(id_transformation, id_step, TAG_ID_CONNECTION, database);

			repository.saveStepAttribute(id_transformation, id_step, TAG_MODE, mode.name());
			// Save the view name
			repository.saveStepAttribute(id_transformation, id_step, TAG_VIEW, view);
			repository.saveStepAttribute(id_transformation, id_step, TAG_SEARCH, search);

			// Save the fields
			for (int i = 0; i < fields.length; i++) {
				repository.saveStepAttribute(id_transformation, id_step, i, TAG_FIELD_NAME, fields[i].getName());
				repository.saveStepAttribute(id_transformation, id_step, i, TAG_FIELD_FORMULA, fields[i].getFormula());
				repository.saveStepAttribute(id_transformation, id_step, i, TAG_FIELD_TYPE,
						ValueMetaFactory.getValueMetaName(fields[i].getType()));
				repository.saveStepAttribute(id_transformation, id_step, i, TAG_FIELD_FORMAT, fields[i].getFormat());
				repository.saveStepAttribute(id_transformation, id_step, i, TAG_FIELD_CURRENCY_SYMBOL,
						fields[i].getCurrencySymbol());
				repository.saveStepAttribute(id_transformation, id_step, i, TAG_FIELD_GROUP_SYMBOL,
						fields[i].getGroupSymbol());
				repository.saveStepAttribute(id_transformation, id_step, i, TAG_FIELD_DECIMAL_SYMBOL,
						fields[i].getDecimalSymbol());
				repository.saveStepAttribute(id_transformation, id_step, i, TAG_FIELD_LENGTH, fields[i].getLength());
				repository.saveStepAttribute(id_transformation, id_step, i, TAG_FIELD_PRECISION,
						fields[i].getPrecision());
				repository.saveStepAttribute(id_transformation, id_step, i, TAG_FIELD_TRIM_TYPE,
						fields[i].getTrimTypeCode());
			}

			// Save the step-database relationship
			if (database != null) {
				repository.insertStepDatabase(id_transformation, id_step, database.getObjectId());
			}

		} catch (Exception e) {
			throw new KettleException(
					BaseMessages.getString(PKG, "DominoInputMeta.Exception.UnableToSaveRepository", id_step), e);
		}
	}

	@Override
	public void getFields(RowMetaInterface inputRowMeta, String name, RowMetaInterface[] info, StepMeta nextStep,
			VariableSpace space, Repository repository, IMetaStore metaStore) throws KettleStepException {
		try {

			// add the output fields
			for (DominoField field : fields) {

				int type = field.getType();
				if (type == ValueMetaInterface.TYPE_NONE) {
					type = ValueMetaInterface.TYPE_STRING;
				}
				ValueMetaInterface vm = ValueMetaFactory.createValueMeta(field.getName(), type);
				vm.setOrigin(name);

				if (mode == DominoInputMode.SEARCH) {
					vm.setComments(field.getFormula());
				}

				vm.setLength(field.getLength());
				vm.setPrecision(field.getPrecision());
				vm.setConversionMask(field.getFormat());
				vm.setDecimalSymbol(field.getDecimalSymbol());
				vm.setGroupingSymbol(field.getGroupSymbol());
				vm.setCurrencySymbol(field.getCurrencySymbol());
				vm.setTrimType(field.getTrimType());
				inputRowMeta.addValueMeta(vm);
			}
		} catch (Exception e) {
			throw new KettleStepException(e);
		}
	}

	@Override
	public void check(List<CheckResultInterface> remarks, TransMeta transMeta, StepMeta stepMeta, RowMetaInterface prev,
			String[] input, String[] output, RowMetaInterface info, VariableSpace space, Repository repository,
			IMetaStore metaStore) {

		if (database == null) {
			String message = BaseMessages.getString(PKG, "DominoInputMeta.CheckResult.DatabaseConnectionMissing");
			remarks.add(new CheckResult(CheckResult.TYPE_RESULT_ERROR, message, stepMeta));
		}

		switch (mode) {
		case SEARCH:
			if (Utils.isEmpty(this.search)) {
				String message = BaseMessages.getString(PKG, "DominoInputMeta.CheckResult.SearchFormulaMissing");
				remarks.add(new CheckResult(CheckResult.TYPE_RESULT_ERROR, message, stepMeta));
			}
			break;
		case VIEW:
			if (Utils.isEmpty(this.view)) {
				String message = BaseMessages.getString(PKG, "DominoInputMeta.CheckResult.ViewNameMissing");
				remarks.add(new CheckResult(CheckResult.TYPE_RESULT_ERROR, message, stepMeta));
			}
			break;
		}

		
		// See if we get input...
	    if ( input != null && input.length > 0 ) {
	    	remarks.add(new CheckResult( CheckResult.TYPE_RESULT_ERROR, BaseMessages.getString(
	          PKG, "DominoInputMeta.CheckResult.NoInputExpected" ), stepMeta ));
	    }
	}

	public DominoField[] getDominoFields() {
		return fields;
	}

	public void setDominoFields(DominoField[] fields) {
		this.fields = fields;
	}

}
