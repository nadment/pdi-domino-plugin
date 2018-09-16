package org.pentaho.di.ui.trans.steps.dominoinput;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableItem;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.Props;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.database.DominoDatabaseMeta;

import org.pentaho.di.core.row.value.ValueMetaBase;
import org.pentaho.di.core.row.value.ValueMetaFactory;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.TransPreviewFactory;
import org.pentaho.di.trans.steps.dominoinput.DominoField;
import org.pentaho.di.trans.steps.dominoinput.DominoInputMeta;
import org.pentaho.di.trans.steps.dominoinput.DominoInputMode;
import org.pentaho.di.ui.core.ConstUI;
import org.pentaho.di.ui.core.FormDataBuilder;
import org.pentaho.di.ui.core.dialog.EnterNumberDialog;
import org.pentaho.di.ui.core.dialog.EnterSelectionDialog;
import org.pentaho.di.ui.core.dialog.EnterTextDialog;
import org.pentaho.di.ui.core.dialog.ErrorDialog;
import org.pentaho.di.ui.core.dialog.PreviewRowsDialog;
import org.pentaho.di.ui.core.gui.GUIResource;
import org.pentaho.di.ui.core.widget.ColumnInfo;
import org.pentaho.di.ui.core.widget.TableView;
import org.pentaho.di.ui.core.widget.TextVar;
import org.pentaho.di.ui.core.widget.TextVarButton;
import org.pentaho.di.ui.dialog.AbstractStepDialog;
import org.pentaho.di.ui.trans.dialog.TransPreviewProgressDialog;

/**
 * Dialog for the Domino input step.
 */
public class DominoInputDialog extends AbstractStepDialog<DominoInputMeta> {

	 /**
	   * The package name used for internationalization
	   */
	private static Class<?> PKG = DominoInputMeta.class; // for i18n purposes

	// Widget

	private CTabFolder wTabFolder;

	private CTabItem wGeneralTab, wFieldsTab;

	private CCombo wConnection;

	private Composite wStackComposite;

	private StackLayout wStackLayout;

	private Button wViewMode;

	private Button wSearchMode;

	private TableView wFields;

	private ColumnInfo[] columnInfos;

	private TextVarButton wViewName;
	private TextVar wSearchFormula;

	/**
	 * Constructor that saves incoming meta object to a local variable, so it
	 * can conveniently read and write settings from/to it.
	 *
	 * @param parent
	 *            the SWT shell to open the dialog in
	 * @param input
	 *            the meta object holding the step's settings
	 * @param transMeta
	 *            transformation description
	 * @param name
	 *            the step name
	 */
	public DominoInputDialog(Shell parent, Object in, TransMeta transMeta, String name) {
		super(parent, in, transMeta, name);

		setText(BaseMessages.getString(PKG, "DominoInputDialog.Shell.Title"));
	}

	/**
	 * Preview the data
	 */
	protected void onPreviewPressed() {

		DominoInputMeta stepMeta = new DominoInputMeta();
		saveMeta(stepMeta);

		TransMeta previewMeta = TransPreviewFactory.generatePreviewTransformation(transMeta, stepMeta,
				wStepname.getText());

		EnterNumberDialog numberDialog = new EnterNumberDialog(shell, props.getDefaultPreviewSize(),
				BaseMessages.getString(PKG, "System.Dialog.EnterPreviewSize.Title"),
				BaseMessages.getString(PKG, "System.Dialog.EnterPreviewSize.Message"));
		int previewSize = numberDialog.open();
		if (previewSize > 0) {
			TransPreviewProgressDialog progressDialog = new TransPreviewProgressDialog(shell, previewMeta,
					new String[] { wStepname.getText() }, new int[] { previewSize });
			progressDialog.open();

			Trans trans = progressDialog.getTrans();
			String loggingText = progressDialog.getLoggingText();

			if (!progressDialog.isCancelled()) {
				if (trans.getResult() != null && trans.getResult().getNrErrors() > 0) {
					EnterTextDialog etd = new EnterTextDialog(shell,
							BaseMessages.getString(PKG, "System.Dialog.PreviewError.Title"),
							BaseMessages.getString(PKG, "System.Dialog.PreviewError.Message"), loggingText, true);
					etd.setReadOnly();
					etd.open();
				}
			}

			PreviewRowsDialog previewDialog = new PreviewRowsDialog(shell, transMeta, SWT.NONE, wStepname.getText(),
					progressDialog.getPreviewRowsMeta(wStepname.getText()),
					progressDialog.getPreviewRows(wStepname.getText()), loggingText);
			previewDialog.open();
		}
	}

	protected void onGetField() {
		try {
			List<DominoField> fields = null; 
					
			shell.setCursor(shell.getDisplay().getSystemCursor(SWT.CURSOR_WAIT));

			// Get available view columns
			if (wViewMode.getSelection() == true) {
				String viewName = transMeta.environmentSubstitute(wViewName.getText());
				fields = this.getStepMeta().getAvailableViewColumns(viewName);
			}
			// Get available document items
			else {
				String searchFormula = transMeta.environmentSubstitute(wSearchFormula.getText());
				fields = this.getStepMeta().getAvailableDocumentItems(searchFormula);
			}

			shell.setCursor(null);
			
			
			if (fields.size() == 0)
				return;

			// Ask what we should do with the existing field in the step.
			MessageDialog md = new MessageDialog(shell,
					BaseMessages.getString(PKG, "DominoInputDialog.GetFieldsChoice.Title"), null,
					BaseMessages.getString(PKG, "DominoInputDialog.GetFieldsChoice.Message", "" + fields.size(),
							"" + fields.size()),
					MessageDialog.WARNING,
					new String[] { BaseMessages.getString(PKG, "DominoInputDialog.AddNew"),
							BaseMessages.getString(PKG, "DominoInputDialog.Add"),
							BaseMessages.getString(PKG, "DominoInputDialog.ClearAndAdd"),
							BaseMessages.getString(PKG, "DominoInputDialog.Cancel"), },
					0);
			MessageDialog.setDefaultImage(GUIResource.getInstance().getImageSpoon());
			int choice = md.open();
			
			choice = choice & 0xFF;

			List<String> fieldNames = new ArrayList<>();

			if (choice == 3 || choice == 255) {
				return; // Cancel clicked
			} else if (choice == 2) {
				wFields.clearAll(false);
			} else if (choice == 0) {
				// build list of existing field names 
				for (int j = 0; j < wFields.nrNonEmpty(); j++) {
					fieldNames.add(wFields.getNonEmpty(j).getText(1));
				}
			}

			for (DominoField field : fields) {

				if (choice == 0) {
					// add only new field
					if (fieldNames.contains(field.getName()))
						continue;
				}

				TableItem item = new TableItem(wFields.table, SWT.NONE);
				item.setText(1, field.getName());
				item.setText(3, ValueMetaFactory.getValueMetaName(field.getType()));
			}
			
			wFields.removeEmptyRows();
			wFields.setRowNums();
			wFields.optWidth(true);
		} catch (Exception e) {
			new ErrorDialog(shell, BaseMessages.getString(PKG, "System.Dialog.GetFieldsFailed.Title"),
					BaseMessages.getString(PKG, "System.Dialog.GetFieldsFailed.Message"), e);

		}
	}

	@Override
	protected void loadMeta(final DominoInputMeta meta) {

		if (meta.getDatabaseMeta() != null) {
			wConnection.setText(meta.getDatabaseMeta().getName());
		} else if (transMeta.nrDatabases() == 1) {
			wConnection.setText(transMeta.getDatabase(0).getName());
		}

		if (meta.getMode() == DominoInputMode.VIEW) {
			wViewMode.setSelection(true);
		} else {
			wSearchMode.setSelection(true);
		}

		wViewName.setText(Const.NVL(meta.getView(), ""));
		wSearchFormula.setText(Const.NVL(meta.getSearch(), ""));

		DominoField[] fields = meta.getDominoFields();
		for (int i = 0; i < fields.length; i++) {
			DominoField field = fields[i];
			TableItem item = wFields.table.getItem(i);
			item.setText(1, Const.NVL(field.getName(), ""));
			item.setText(2, Const.NVL(field.getFormula(), ""));
			item.setText(3, ValueMetaBase.getTypeDesc(field.getType()));
			item.setText(4, String.valueOf(field.getLength()));
			item.setText(5, String.valueOf(field.getPrecision()));
			item.setText(6, Const.NVL(field.getFormat(), ""));
			item.setText(7, Const.NVL(field.getCurrencySymbol(), ""));
			item.setText(8, Const.NVL(field.getDecimalSymbol(), ""));
			item.setText(9, Const.NVL(field.getGroupSymbol(), ""));
			item.setText(10, field.getTrimTypeDesc());
		}

		wFields.removeEmptyRows();
		wFields.setRowNums();
		wFields.optWidth(true);

		this.updateMode(meta.getMode());
	}

	@Override
	protected void saveMeta(final DominoInputMeta meta) {

		// save step name
		stepname = wStepname.getText();

		meta.setDatabaseMeta(transMeta.findDatabase(wConnection.getText()));
		meta.setMode(wViewMode.getSelection() ? DominoInputMode.VIEW : DominoInputMode.SEARCH);
		meta.setView(wViewName.getText());
		meta.setSearch(wSearchFormula.getText());

		DominoField[] fields = new DominoField[wFields.nrNonEmpty()];
		for (int i = 0; i < fields.length; i++) {
			TableItem item = wFields.getNonEmpty(i);

			DominoField field = new DominoField();
			field.setName(item.getText(1));
			field.setFormula(item.getText(2));
			field.setType(ValueMetaFactory.getIdForValueMeta(item.getText(3)));
			field.setLength(Const.toInt(item.getText(4), -1));
			field.setPrecision(Const.toInt(item.getText(5), -1));
			field.setFormat(item.getText(6));
			field.setCurrencySymbol(item.getText(8));
			field.setDecimalSymbol(item.getText(9));
			field.setGroupSymbol(item.getText(9));
			field.setTrimType(ValueMetaBase.getTrimTypeByDesc(item.getText(10)));
			fields[i] = field;
		}
		meta.setDominoFields(fields);

		if (transMeta.findDatabase(wConnection.getText()) == null) {
			MessageBox mb = new MessageBox(shell, SWT.OK | SWT.ICON_ERROR);
			mb.setMessage(BaseMessages.getString(PKG, "DominoInputDialog.ErrorDialog.DatabaseConnectionMissing"));
			mb.setText(BaseMessages.getString(PKG, "DominoInputDialog.ErrorDialog.Title"));
			mb.open();
		}
	}

	@Override
	public Point getMinimumSize() {
		return new Point(650, 420);
	}

	protected Control createDialogArea(final Composite parent) {

		int middle = props.getMiddlePct();

		FocusListener lsConnectionFocus = new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent event) {
				getStepMeta().setChanged();

				// setTableFieldCombo();
			}
		};

		wTabFolder = new CTabFolder(parent, SWT.BORDER);
		wTabFolder.setLayoutData(new FormDataBuilder().top().fullWidth().bottom().result());
		props.setLook(wTabFolder, Props.WIDGET_STYLE_TAB);

		// ----------------------------------------------------------------
		// Connection tab
		// ----------------------------------------------------------------

		wGeneralTab = new CTabItem(wTabFolder, SWT.NONE);
		wGeneralTab.setText(BaseMessages.getString(PKG, "DominoInputDialog.General.Tab"));

		Composite wGeneralComposite = new Composite(wTabFolder, SWT.NONE);
		props.setLook(wGeneralComposite);

		FormLayout generalLayout = new FormLayout();
		generalLayout.marginWidth = Const.FORM_MARGIN;
		generalLayout.marginHeight = Const.FORM_MARGIN;
		wGeneralComposite.setLayout(generalLayout);

		wGeneralTab.setControl(wGeneralComposite);

		// ----------------------------------------------------------------
		// Widget Connection line
		// ----------------------------------------------------------------
		wConnection = addConnectionLine(wGeneralComposite, null, middle, Const.MARGIN);

		List<String> items = new ArrayList<String>();
		for (DatabaseMeta dbMeta : transMeta.getDatabases()) {
			if (dbMeta.getDatabaseInterface() instanceof DominoDatabaseMeta) {
				items.add(dbMeta.getName());
			}
		}
		wConnection.setItems(items.toArray(new String[items.size()]));
		if (this.getStepMeta().getDatabaseMeta() == null && transMeta.nrDatabases() == 1) {
			wConnection.select(0);
		}
		wConnection.addFocusListener(lsConnectionFocus);
		wConnection.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				// We have new content: change database connection:
				getStepMeta().setDatabaseMeta(transMeta.findDatabase(wConnection.getText()));
				getStepMeta().setChanged();

				// Update available fields
				// updateAvailableFields();
			}
		});
		props.setLook(wConnection);

		// ----------------------------------------------------------------
		// Widget mode view or search formula
		// ----------------------------------------------------------------

		Composite wModeComposite = new Composite(wGeneralComposite, SWT.NONE);
		wModeComposite.setLayoutData(new FormDataBuilder().top(wConnection, Const.MARGIN)
				.right(props.getMiddlePct(), -Const.MARGIN).result());
		wModeComposite.setLayout(new RowLayout());
		props.setLook(wModeComposite);

		wViewMode = new Button(wModeComposite, SWT.RADIO);
		wViewMode.setText(BaseMessages.getString(PKG, "DominoInputDialog.View.Label"));
		wViewMode.setToolTipText(BaseMessages.getString(PKG, "DominoInputDialog.View.Tooltip"));
		wViewMode.addSelectionListener(lsDef);
		wViewMode.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event e) {
				updateMode(DominoInputMode.VIEW);
				getStepMeta().setChanged();
			}
		});
		props.setLook(wViewMode);

		wSearchMode = new Button(wModeComposite, SWT.RADIO);
		wSearchMode.setText(BaseMessages.getString(PKG, "DominoInputDialog.Search.Label"));
		wSearchMode.setToolTipText(BaseMessages.getString(PKG, "DominoInputDialog.Search.Tooltip"));
		wSearchMode.addSelectionListener(lsDef);
		wSearchMode.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event e) {
				updateMode(DominoInputMode.SEARCH);
				getStepMeta().setChanged();
			}
		});
		props.setLook(wSearchMode);

		wStackComposite = new Composite(wGeneralComposite, SWT.NONE);
		wStackComposite.setLayoutData(new FormDataBuilder().top(wConnection, Const.MARGIN).bottom()
				.left(props.getMiddlePct(), 0).right(100, 0).result());
		wStackLayout = new StackLayout();
		wStackComposite.setLayout(wStackLayout);
		props.setLook(wStackComposite);

		// ----------------------------------------------------------------
		// Widget search formula
		// ----------------------------------------------------------------
		SelectionAdapter lsButton = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				onSelectView();
			}
		};

		// TODO: Set button label 'Browse' with System.Button.Browse
		wViewName = new TextVarButton(stepMeta.getParentTransMeta(), wStackComposite,
				SWT.SINGLE | SWT.LEFT | SWT.BORDER, null, null, lsButton);
		wViewName.addModifyListener(lsMod);

		props.setLook(wViewName);

		// ----------------------------------------------------------------
		// Widget search formula
		// ----------------------------------------------------------------
		wSearchFormula = new TextVar(stepMeta.getParentTransMeta(), wStackComposite, SWT.SINGLE | SWT.LEFT | SWT.BORDER,
				null, null);
		wSearchFormula.addModifyListener(lsMod);
		props.setLook(wSearchFormula);

		// ----------------------------------------------------------------
		// View fields tab
		// ----------------------------------------------------------------

		wFieldsTab = new CTabItem(wTabFolder, SWT.BORDER);
		wFieldsTab.setText(BaseMessages.getString(PKG, "DominoInputDialog.Fields.Tab"));

		Composite wFieldsComposite = new Composite(wTabFolder, SWT.NONE);
		props.setLook(wFieldsComposite);

		FormLayout fieldsLayout = new FormLayout();
		fieldsLayout.marginWidth = Const.FORM_MARGIN;
		fieldsLayout.marginHeight = Const.FORM_MARGIN;
		wFieldsComposite.setLayout(fieldsLayout);
		wFieldsComposite.setLayoutData(new FormDataBuilder().top().left().fullSize().result());

		// Widget Get fields
		wGet = new Button(wFieldsComposite, SWT.PUSH);
		wGet.setText(BaseMessages.getString(PKG, "System.Button.GetFields"));
		wGet.setLayoutData(new FormDataBuilder().top().right().result());
		wGet.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event e) {
				onGetField();
			}
		});

		columnInfos = new ColumnInfo[] {
				// new ColumnInfo(BaseMessages.getString(PKG,
				// "System.Column.Name"), ColumnInfo.COLUMN_TYPE_CCOMBO, false),
				new ColumnInfo(BaseMessages.getString(PKG, "System.Column.Name"), ColumnInfo.COLUMN_TYPE_TEXT, false),
				new ColumnInfo(BaseMessages.getString(PKG, "DominoInputDialog.FieldsTable.Formula.Column"),
						ColumnInfo.COLUMN_TYPE_TEXT, false),
				new ColumnInfo(BaseMessages.getString(PKG, "System.Column.Type"), ColumnInfo.COLUMN_TYPE_CCOMBO,
						ValueMetaFactory.getValueMetaNames(), true),

				new ColumnInfo(BaseMessages.getString(PKG, "System.Column.Length"), ColumnInfo.COLUMN_TYPE_TEXT, false),
				new ColumnInfo(BaseMessages.getString(PKG, "System.Column.Precision"), ColumnInfo.COLUMN_TYPE_TEXT,
						false),
				new ColumnInfo(BaseMessages.getString(PKG, "System.Column.Format"), ColumnInfo.COLUMN_TYPE_FORMAT, 3),
				new ColumnInfo(BaseMessages.getString(PKG, "System.Column.Currency"), ColumnInfo.COLUMN_TYPE_TEXT),
				new ColumnInfo(BaseMessages.getString(PKG, "System.Column.Decimal"), ColumnInfo.COLUMN_TYPE_TEXT),
				new ColumnInfo(BaseMessages.getString(PKG, "System.Column.Group"), ColumnInfo.COLUMN_TYPE_TEXT),
				new ColumnInfo(BaseMessages.getString(PKG, "DominoInputDialog.FieldsTable.TrimType.Column"),
						ColumnInfo.COLUMN_TYPE_CCOMBO, ValueMetaBase.trimTypeDesc),

		};

		columnInfos[0].setUsingVariables(true);
		columnInfos[1].setUsingVariables(true);

		wFields = new TableView(transMeta, wFieldsComposite, SWT.FULL_SELECTION | SWT.MULTI, columnInfos,
				this.getStepMeta().getDominoFields().length, lsMod, props);
		wFields.setLayoutData(new FormDataBuilder().top().left().right(wGet, -Const.MARGIN).bottom().result());

		wFieldsTab.setControl(wFieldsComposite);

		wTabFolder.setSelection(0);

		return parent;
	}

	protected void onSelectView() {

		try {

			shell.setCursor(shell.getDisplay().getSystemCursor(SWT.CURSOR_WAIT));
			List<String> viewNames = this.getStepMeta().getViewNames();
			shell.setCursor(null);

			EnterSelectionDialog dialog = new EnterSelectionDialog(this.shell,
					viewNames.toArray(new String[viewNames.size()]),
					BaseMessages.getString(PKG, "DominoInputDialog.ViewSelection.DialogTitle"),
					BaseMessages.getString(PKG, "DominoInputDialog.ViewSelection.DialogMessage"), null);

			String result = dialog.open();

			if (result != null) {
				getStepMeta().setChanged();
				wViewName.setText(result);
			}
		} catch (Exception e) {
			new ErrorDialog(shell, BaseMessages.getString(PKG, "DominoInputDialog.ViewSelection.DialogTitle"),
					BaseMessages.getString(PKG, "DominoInputDialog.ViewSelection.DialogError"), e);
		}
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {

		super.createButtonsForButtonBar(parent);

		wPreview = new Button(parent, SWT.PUSH);
		wPreview.setText(BaseMessages.getString(PKG, "System.Button.Preview"));
		wPreview.setLayoutData(
				new FormDataBuilder().bottom().right(wOK, -ConstUI.SMALL_MARGIN).width(BUTTON_WIDTH).result());
		wPreview.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event e) {
				onPreviewPressed();
			}
		});
	}

	protected void updateMode(final DominoInputMode mode) {
		if (mode == DominoInputMode.VIEW) {
			columnInfos[1].setReadOnly(true);
			wStackLayout.topControl = wViewName;
			wStackComposite.layout();
		} else {
			columnInfos[1].setReadOnly(false);
			wStackLayout.topControl = wSearchFormula;
			wStackComposite.layout();
		}
	}

}