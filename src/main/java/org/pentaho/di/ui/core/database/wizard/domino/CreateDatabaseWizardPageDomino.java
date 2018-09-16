package org.pentaho.di.ui.core.database.wizard.domino;

import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.database.DominoDatabaseMeta;
import org.pentaho.di.core.util.Utils;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.ui.core.FormDataBuilder;
import org.pentaho.di.ui.core.PropsUI;
import org.pentaho.di.ui.core.widget.LabelText;

public class CreateDatabaseWizardPageDomino extends WizardPage {

	private static Class<?> PKG = CreateDatabaseWizardPageDomino.class; // for i18n purposes

	private LabelText wServer;

	private LabelText wDatabase;

	private LabelText wReplicaID;

	private LabelText wUserName;

	private LabelText wPassword;

	private PropsUI props;

	private DatabaseMeta database;

	public CreateDatabaseWizardPageDomino(String pageName, PropsUI props, DatabaseMeta info) {
		super(pageName);
		this.props = props;
		this.database = info;

		setTitle(BaseMessages.getString(PKG, "CreateDatabaseWizardPageDomino.DialogTitle"));
		setDescription(BaseMessages.getString(PKG, "CreateDatabaseWizardPageDomino.DialogMessage"));
		setPageComplete(false);
	}

	@Override
	public void createControl(final Composite parent) {

		// create the composite to hold the widgets
		Composite composite = new Composite(parent, SWT.NONE);
		props.setLook(composite);

		FormLayout compLayout = new FormLayout();
		compLayout.marginHeight = Const.FORM_MARGIN;
		compLayout.marginWidth = Const.FORM_MARGIN;
		composite.setLayout(compLayout);

		// The ModifyListener used on all controls. It will update the meta
		// object to
		// indicate that changes are being made.
		ModifyListener lsMod = new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				setPageComplete(false);
			}
		};

		// Widget Server
		wServer = new LabelText(composite, BaseMessages.getString(PKG, "CreateDatabaseWizardPageDomino.Server.Label"),
				BaseMessages.getString(PKG, "CreateDatabaseWizardPageDomino.Server.Tooltip"));
		wServer.setLayoutData(new FormDataBuilder().top().fullWidth().result());
		// wServer.addModifyListener(lsMod);
		props.setLook(wServer);

		// Widget database
		wDatabase = new LabelText(composite,
				BaseMessages.getString(PKG, "CreateDatabaseWizardPageDomino.Database.Label"),
				BaseMessages.getString(PKG, "CreateDatabaseWizardPageDomino.Database.Tooltip"));
		wDatabase.setLayoutData(new FormDataBuilder().top(wServer).fullWidth().result());
		wDatabase.addModifyListener(lsMod);
		props.setLook(wDatabase);

		// Widget replica ID
		wReplicaID = new LabelText(composite,
				BaseMessages.getString(PKG, "CreateDatabaseWizardPageDomino.ReplicaID.Label"),
				BaseMessages.getString(PKG, "CreateDatabaseWizardPageDomino.ReplicaID.Tooltip"));
		wReplicaID.setLayoutData(new FormDataBuilder().top(wDatabase).fullWidth().result());
		wReplicaID.addModifyListener(lsMod);
		props.setLook(wReplicaID);

		// Widget user
		wUserName = new LabelText(composite, BaseMessages.getString(PKG, "CreateDatabaseWizardPageDomino.User.Label"),
				BaseMessages.getString(PKG, "CreateDatabaseWizardPageDomino.User.Tooltip"));
		wUserName.setLayoutData(new FormDataBuilder().top(wReplicaID).fullWidth().result());
		wUserName.addModifyListener(lsMod);
		props.setLook(wUserName);

		// Password line
		wPassword = new LabelText(composite,
				BaseMessages.getString(PKG, "CreateDatabaseWizardPageDomino.Password.Label"),
				BaseMessages.getString(PKG, "CreateDatabaseWizardPageDomino.Password.Tooltip"));
		wPassword.getTextWidget().setEchoChar('*');
		wPassword.setLayoutData(new FormDataBuilder().top(wUserName).fullWidth().result());
		wPassword.addModifyListener(lsMod);
		props.setLook(wPassword);

		// set the composite as the control for this page
		setControl(composite);
	}

	public void setData() {
		wServer.setText(Const.NVL(database.getHostname(), ""));
		wDatabase.setText(Const.NVL(database.getDatabaseName(), ""));
		wReplicaID
				.setText(Const.NVL(database.getAttributes().getProperty(DominoDatabaseMeta.ATTRIBUTE_REPLICA_ID, ""), ""));
		wUserName.setText(Const.NVL(database.getUsername(), ""));
		wPassword.setText(Const.NVL(database.getPassword(), ""));
	}
	
	
	@Override
	public boolean canFlipToNextPage() {

		if (Utils.isEmpty(wDatabase.getText()) && Utils.isEmpty(wReplicaID.getText())) {
			setErrorMessage(BaseMessages.getString(PKG, "CreateDatabaseWizardPageDomino.ErrorMessage.InvalidInput"));
			return false;
		} else {
			getDatabaseInfo();
			setErrorMessage(null);
			setMessage(BaseMessages.getString(PKG, "CreateDatabaseWizardPageDomino.Message.Next"));
			return true;
		}

	}

	public DatabaseMeta getDatabaseInfo() {
		if (!Utils.isEmpty(wServer.getText())) {
			database.setHostname(wServer.getText());
		}

		if (!Utils.isEmpty(wDatabase.getText())) {
			database.setDBName(wDatabase.getText());
		}

		if (!Utils.isEmpty(wReplicaID.getText())) {
			database.getAttributes().put(DominoDatabaseMeta.ATTRIBUTE_REPLICA_ID, wReplicaID.getText());
		}

		if (!Utils.isEmpty(wUserName.getText())) {
			database.setDBName(wDatabase.getText());
		}

		if (!Utils.isEmpty(wPassword.getText())) {
			database.setDBName(wDatabase.getText());
		}

		return database;
	}

	
	@Override
	public IWizardPage getNextPage() {
		IWizard wizard = getWizard();
		return wizard.getPage("2");
	}
}
