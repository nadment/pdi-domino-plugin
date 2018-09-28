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

	private LabelText txtServer;

	private LabelText txtDatabase;

	private LabelText txtReplicaID;

	private LabelText txtUserName;

	private LabelText txtPassword;

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
		// object to indicate that changes are being made.
		ModifyListener lsMod = new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				setPageComplete(false);
			}
		};

		// Widget Server
		txtServer = new LabelText(composite, BaseMessages.getString(PKG, "CreateDatabaseWizardPageDomino.Server.Label"),
				BaseMessages.getString(PKG, "CreateDatabaseWizardPageDomino.Server.Tooltip"));
		txtServer.setLayoutData(new FormDataBuilder().top().fullWidth().result());
		// wServer.addModifyListener(lsMod);
		props.setLook(txtServer);

		// Widget database
		txtDatabase = new LabelText(composite,
				BaseMessages.getString(PKG, "CreateDatabaseWizardPageDomino.Database.Label"),
				BaseMessages.getString(PKG, "CreateDatabaseWizardPageDomino.Database.Tooltip"));
		txtDatabase.setLayoutData(new FormDataBuilder().top(txtServer).fullWidth().result());
		txtDatabase.addModifyListener(lsMod);
		props.setLook(txtDatabase);

		// Widget replica ID
		txtReplicaID = new LabelText(composite,
				BaseMessages.getString(PKG, "CreateDatabaseWizardPageDomino.ReplicaID.Label"),
				BaseMessages.getString(PKG, "CreateDatabaseWizardPageDomino.ReplicaID.Tooltip"));
		txtReplicaID.setLayoutData(new FormDataBuilder().top(txtDatabase).fullWidth().result());
		txtReplicaID.addModifyListener(lsMod);
		props.setLook(txtReplicaID);

		// Widget user
		txtUserName = new LabelText(composite, BaseMessages.getString(PKG, "CreateDatabaseWizardPageDomino.User.Label"),
				BaseMessages.getString(PKG, "CreateDatabaseWizardPageDomino.User.Tooltip"));
		txtUserName.setLayoutData(new FormDataBuilder().top(txtReplicaID).fullWidth().result());
		txtUserName.addModifyListener(lsMod);
		props.setLook(txtUserName);

		// Password line
		txtPassword = new LabelText(composite,
				BaseMessages.getString(PKG, "CreateDatabaseWizardPageDomino.Password.Label"),
				BaseMessages.getString(PKG, "CreateDatabaseWizardPageDomino.Password.Tooltip"));
		txtPassword.getTextWidget().setEchoChar('*');
		txtPassword.setLayoutData(new FormDataBuilder().top(txtUserName).fullWidth().result());
		txtPassword.addModifyListener(lsMod);
		props.setLook(txtPassword);

		// set the composite as the control for this page
		setControl(composite);
	}

	public void setData() {
		txtServer.setText(Const.NVL(database.getHostname(), ""));
		txtDatabase.setText(Const.NVL(database.getDatabaseName(), ""));
		txtReplicaID
				.setText(Const.NVL(database.getAttributes().getProperty(DominoDatabaseMeta.ATTRIBUTE_REPLICA_ID, ""), ""));
		txtUserName.setText(Const.NVL(database.getUsername(), ""));
		txtPassword.setText(Const.NVL(database.getPassword(), ""));
	}
	
	
	@Override
	public boolean canFlipToNextPage() {

		if (Utils.isEmpty(txtDatabase.getText()) && Utils.isEmpty(txtReplicaID.getText())) {
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
		if (!Utils.isEmpty(txtServer.getText())) {
			database.setHostname(txtServer.getText());
		}

		if (!Utils.isEmpty(txtDatabase.getText())) {
			database.setDBName(txtDatabase.getText());
		}

		if (!Utils.isEmpty(txtReplicaID.getText())) {
			database.getAttributes().put(DominoDatabaseMeta.ATTRIBUTE_REPLICA_ID, txtReplicaID.getText());
		}

		if (!Utils.isEmpty(txtUserName.getText())) {
			database.setDBName(txtDatabase.getText());
		}

		if (!Utils.isEmpty(txtPassword.getText())) {
			database.setDBName(txtDatabase.getText());
		}

		return database;
	}

	
	@Override
	public IWizardPage getNextPage() {
		IWizard wizard = getWizard();
		return wizard.getPage("2");
	}
}
