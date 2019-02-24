package org.kettle.trans.steps.dominoinput;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Vector;

import org.kettle.core.database.DominoDatabaseConnection;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.RowDataUtil;
import org.pentaho.di.core.row.RowMeta;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.util.Utils;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStep;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;

import lotus.domino.DateTime;
import lotus.domino.Document;
import lotus.domino.DocumentCollection;
import lotus.domino.NotesException;
import lotus.domino.View;
import lotus.domino.ViewEntry;
import lotus.domino.ViewNavigator;

public class DominoInputStep extends BaseStep implements StepInterface {

	/**
	 * The package name used for internationalization
	 */
	private static final Class<?> PKG = DominoInputMeta.class;

	private DominoDatabaseConnection connection = null;

	private View view;

	private ViewNavigator viewNavigator;

	private ViewEntry viewEntry;

	private DocumentCollection collection;

	private Document document;

	public DominoInputStep(StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr, TransMeta transMeta,
			Trans trans) {

		super(stepMeta, stepDataInterface, copyNr, transMeta, trans);

		connection = null;
		view = null;
		viewNavigator = null;
		viewEntry = null;
	}

	@Override
	public boolean init(StepMetaInterface smi, StepDataInterface sdi) {
		// Casting to step-specific implementation classes is safe
		DominoInputMeta meta = (DominoInputMeta) smi;
		DominoInputData data = (DominoInputData) sdi;

		if (super.init(meta, data)) {
			first = true;

			return true;
		}

		return false;
	}

	@Override
	public boolean processRow(StepMetaInterface smi, StepDataInterface sdi) throws KettleException {
		DominoInputMeta meta = (DominoInputMeta) smi;
		DominoInputData data = (DominoInputData) sdi;

		boolean result = false;
		try {
			if (first) {
				first = false;

				// Create the output row meta-data
				data.outputRowMeta = new RowMeta();
				meta.getFields(data.outputRowMeta, getStepname(), null, null, this, repository, metaStore);

				// create Domino connection
				connection = new DominoDatabaseConnection(meta.getDatabaseMeta());
				if (log.isDetailed()) {
					logDetailed(BaseMessages.getString(PKG, "DominoInputStep.Log.DatabaseOpen"), connection);
				}

				if (meta.getMode() == DominoInputMode.VIEW) {

					// Resolve variables
					String viewName = environmentSubstitute(meta.getView());

					// get a view from a database
					view = connection.getView(viewName);
					if (view == null) {

						throw new KettleException(BaseMessages.getString(PKG, "DominoInputStep.Error.ViewNotfound",
								connection, viewName));

					}

					if (log.isDetailed()) {
						logDetailed(BaseMessages.getString(PKG, "DominoInputStep.Log.ViewFound"), viewName);
					}

					// disable view auto updating
					view.setAutoUpdate(false);

					// Create mapping column
					data.columns = new HashMap<>();
					Vector<?> names = view.getColumnNames();
					for (DominoField field : meta.getDominoFields()) {
						boolean found = false;
						for (int column = 0; column < names.size(); column++) {
							if (field.getName().equals(names.get(column))) {
								data.columns.put(field, column);
								found = true;
							}

						}

						if (found == false) {
							data.columns.put(field, -1);
							this.logError(BaseMessages.getString(PKG, "DominoInputStep.Error.ViewColumnNotfound",
									viewName, field.getName()));
						}
					}

					// create view navigator
					viewNavigator = view.createViewNav();
					if (log.isDetailed()) {
						logDetailed(BaseMessages.getString(PKG, "DominoInputStep.Log.ViewNavigatorCreated"));
					}

					// enable cache for max buffering
					if (connection.isUseLocalClient()) {
						this.setCacheGuidance(viewNavigator, 400, ViewNavigator.VN_CACHEGUIDANCE_READSELECTIVE);
					}

					// Not implemented on IIOP
					// navigator.setEntryOptions(ViewNavigator.VN_ENTRYOPT_NOCOUNTDATA
					// +
					// ViewNavigator.VN_ENTRYOPT_NOCOLUMNVALUES);
					// navigator.setCacheGuidance(Integer.MAX_VALUE,
					// ViewNavigator.VN_CACHEGUIDANCE_READSELECTIVE);

					viewEntry = viewNavigator.getFirst();
					if (viewEntry == null) {
						setOutputDone();
						return false;
					}

				}
				// Formula mode
				else {

					// Resolve variables
					String search = environmentSubstitute(meta.getSearch());
					collection = connection.getDatabase().search(search);

					document = collection.getFirstDocument();
					if (document == null) {
						setOutputDone();
						return false;
					}
				}

			}

			if (meta.getMode() == DominoInputMode.VIEW) {
				result = this.processViewEntry(meta, data);
			} else {
				result = this.processDocument(meta, data);
			}
		} catch (Exception e) {
			logError(e.getMessage(), e);
			setErrors(1);
			stopAll();
			setOutputDone(); // signal end to receiver(s)
			
			return false;
		}

		return result;
	}

	protected boolean processViewEntry(final DominoInputMeta meta, final DominoInputData data) throws Exception {

		// Build an empty row based on the meta-data
		Object[] outputRowData = RowDataUtil.allocateRowData(data.outputRowMeta.size());

		Vector<?> values = viewEntry.getColumnValues();

		DominoField[] fields = meta.getDominoFields();
		for (int index = 0; index < fields.length; index++) {
			DominoField field = fields[index];

			Object value = null;
			int column = data.columns.get(field);
			if (column > 0) {
				value = values.get(column);
			}
			ValueMetaInterface valueMeta = data.outputRowMeta.getValueMeta(index);
			outputRowData[index] = this.getKettleValue(valueMeta, value);
		}

		// log progress if it is time to to so
		if (checkFeedback(getLinesRead())) {
			logBasic("Line nr " + getLinesRead()); // Some basic logging
		}

		// copy row to output rowset(s);
		putRow(data.outputRowMeta, outputRowData);

		incrementLinesInput();

		// recycle any handles to C++ objects in the Vector like
		// DateTime
		if (connection.isUseLocalClient()) {
			connection.getSession().recycle(values);
		}

		// next view entry
		ViewEntry nextEntry = viewNavigator.getNext(viewEntry);
		viewEntry.recycle();
		viewEntry = nextEntry;

		if (viewEntry == null) {
			setOutputDone();
			return false;
		}

		return true;
	}

	protected boolean processDocument(final DominoInputMeta stepMeta, final DominoInputData data) throws Exception {

		// Build an empty row based on the meta-data
		Object[] outputRowData = RowDataUtil.allocateRowData(data.outputRowMeta.size());

		DominoField[] fields = stepMeta.getDominoFields();
		for (int index = 0; index < fields.length; index++) {
			DominoField field = fields[index];
			ValueMetaInterface valueMeta = data.outputRowMeta.getValueMeta(index);

			@SuppressWarnings("rawtypes")
			java.util.Vector values = null;

			if (Utils.isEmpty(field.getFormula())) {
				values = document.getItemValue(field.getName());
			} else {
				values = connection.getSession().evaluate(field.getFormula(), document);
			}

			Object value = null;
			if (!values.isEmpty()) {
				value = values.firstElement();
				outputRowData[index] = this.getKettleValue(valueMeta, value);
			}
		}

		// log progress if it is time to to so
		if (checkFeedback(getLinesRead())) {
			logBasic("Line nr " + getLinesRead()); // Some basic logging
		}

		// copy row to output rowset(s);
		putRow(data.outputRowMeta, outputRowData);

		incrementLinesInput();

		document.recycle();

		document = collection.getNextDocument();

		if (document == null) {
			setOutputDone();
			return false;
		}

		return true;
	}

	@Override
	public void dispose(StepMetaInterface smi, StepDataInterface sdi) {

		try {

			if (collection != null) {
				collection.recycle();
				collection = null;
			}

			if (viewEntry != null) {
				viewEntry.recycle();
				viewEntry = null;
			}

			if (viewNavigator != null) {
				viewNavigator.recycle();
				viewNavigator = null;
			}

			if (view != null) {
				view.recycle();
				view = null;
			}
		} catch (NotesException e) {
			logError(BaseMessages.getString(PKG, "DominoInputStep.Log.DBException"), e);
		} finally {
			if (connection != null) {
				connection.close();
				connection = null;
			}
		}

		super.dispose(smi, sdi);
	}

	/**
	 * For backward compatibility, use Java reflection instead to check if
	 * setCacheGuidance exists
	 * 
	 * <p>
	 * Not implemented on IIOP
	 * </p>
	 * 
	 * @param viewNav
	 * @param maxEntries
	 * @param readMode
	 * @return
	 * @throws NotesException
	 */

	private boolean setCacheGuidance(final ViewNavigator viewNav, int maxEntries, int readMode) throws NotesException {
		try {
			Method setCacheGuidance = ViewNavigator.class.getMethod("setCacheGuidance", Integer.TYPE, Integer.TYPE);
			setCacheGuidance.invoke(viewNav, maxEntries, readMode);
			return true;
		} catch (SecurityException | NoSuchMethodException | IllegalArgumentException | IllegalAccessException e) {
			// ignore, must be 8.5.2 or below
			viewNav.setCacheSize(maxEntries);
		} catch (InvocationTargetException e) {
			if (e.getCause() instanceof NotesException)
				throw (NotesException) e.getCause();
			// ignore, must be 8.5.2 or below
			viewNav.setCacheSize(maxEntries);
		}
		return false;
	}

	/**
	 * 
	 * Perform Kettle type conversions for the Domino item value.
	 * 
	 * @param value
	 * 
	 *            the item value from Domino
	 * 
	 * @return an Object of the appropriate Kettle type
	 * 
	 * @throws KettleException
	 * 
	 *             if a problem occurs
	 * 
	 */

	public Object getKettleValue(final ValueMetaInterface meta, Object value) throws KettleException {

		if (value == null)
			return value;

		switch (meta.getType()) {

		case ValueMetaInterface.TYPE_BIGNUMBER:

			if (value instanceof Number) {
				value = BigDecimal.valueOf(((Number) value).doubleValue());
			} else if (value instanceof Date) {
				value = new BigDecimal(((Date) value).getTime());
			} else {
				value = new BigDecimal(value.toString());
			}

			return meta.getBigNumber(value);

		case ValueMetaInterface.TYPE_BINARY:

			value = value.toString().getBytes();

			return meta.getBinary(value);

		case ValueMetaInterface.TYPE_BOOLEAN:

			if (value instanceof Number) {
				value = new Boolean(((Number) value).intValue() != 0);
			} else if (value instanceof Date) {
				value = new Boolean(((Date) value).getTime() != 0);
			} else if (!(value instanceof Boolean)) {
				value = new Boolean(value.toString().equalsIgnoreCase("Y") //$NON-NLS-1$

						|| value.toString().equalsIgnoreCase("T") //$NON-NLS-1$

						|| value.toString().equalsIgnoreCase("1")); //$NON-NLS-1$

			}

			return meta.getBoolean(value);

		case ValueMetaInterface.TYPE_DATE:

			if (value instanceof DateTime) {
				try {
					value = ((lotus.domino.DateTime) value).toJavaDate();
				} catch (NotesException e) {
					throw new KettleException(BaseMessages.getString(PKG, "DominoInputStep.Error.DateConversion", //$NON-NLS-1$
							meta, String.valueOf(value)), e);
				}
			} else if (value instanceof Number) {
				value = new Date(((Number) value).longValue());

			} else {
				throw new KettleException(BaseMessages.getString(PKG, "DominoInputStep.Error.DateConversion", //$NON-NLS-1$
						meta, String.valueOf(value)));
			}

			return meta.getDate(value);

		case ValueMetaInterface.TYPE_TIMESTAMP:

			if (value instanceof DateTime) {
				try {
					value = Timestamp.from(((lotus.domino.DateTime) value).toJavaDate().toInstant());
				} catch (NotesException e) {
					throw new KettleException(BaseMessages.getString(PKG, "DominoInputStep.Error.DateConversion", //$NON-NLS-1$
							meta, String.valueOf(value)), e);
				}
			} else if (value instanceof Number) {
				value = new Timestamp(((Number) value).longValue());

			} else {
				throw new KettleException(BaseMessages.getString(PKG, "DominoInputStep.Error.DateConversion", //$NON-NLS-1$
						meta, String.valueOf(value)));

			}

			return meta.getDate(value);

		case ValueMetaInterface.TYPE_INTEGER:

			if (value instanceof Number) {
				value = new Long(((Number) value).intValue());
			} else {
				value = new Long(value.toString());
			}

			return meta.getInteger(value);

		case ValueMetaInterface.TYPE_NUMBER:

			if (value instanceof Number) {
				value = new Double(((Number) value).doubleValue());
			} else {
				value = new Double(value.toString());
			}

			return meta.getNumber(value);

		case ValueMetaInterface.TYPE_INET:

			try {
				value = InetAddress.getByName(value.toString());
			} catch (UnknownHostException e) {
				throw new KettleException(BaseMessages.getString(PKG, "DominoInputStep.ErrorMessage.DateConversion", //$NON-NLS-1$
						meta, String.valueOf(value)), e);

			}

			return meta.getString(value);

		case ValueMetaInterface.TYPE_STRING:

			return meta.getString(value);

		default:

			return null;

		}

	}
}
