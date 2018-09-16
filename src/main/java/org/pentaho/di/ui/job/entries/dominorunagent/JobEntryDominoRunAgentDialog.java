package org.pentaho.di.ui.job.entries.dominorunagent;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.database.DominoDatabaseMeta;
import org.pentaho.di.core.util.Utils;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.job.JobMeta;
import org.pentaho.di.job.entries.dominorunagent.JobEntryDominoRunAgent;
import org.pentaho.di.job.entry.JobEntryDialogInterface;
import org.pentaho.di.job.entry.JobEntryInterface;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.ui.core.FormDataBuilder;
import org.pentaho.di.ui.core.dialog.EnterSelectionDialog;
import org.pentaho.di.ui.core.dialog.ErrorDialog;
import org.pentaho.di.ui.core.widget.TextVarButton;
import org.pentaho.di.ui.dialog.AbstractJobEntryDialog;

public class JobEntryDominoRunAgentDialog extends AbstractJobEntryDialog<JobEntryDominoRunAgent>
		implements JobEntryDialogInterface {

	private static Class<?> PKG = JobEntryDominoRunAgent.class;

	private CCombo wConnection;

	private TextVarButton wAgentName;

	

	public JobEntryDominoRunAgentDialog(Shell parent, JobEntryInterface jobEntry, Repository rep, JobMeta jobMeta) {
		super(parent, jobEntry, rep, jobMeta);

		if (Utils.isEmpty(jobEntry.getName())) {
			jobEntry.setName(BaseMessages.getString(PKG, "JobEntryDominoRunAgentDialog.Name.Default"));
		}

		setText(BaseMessages.getString(PKG, "JobEntryDominoRunAgentDialog.Shell.Title"));
	}

	@Override
	protected Control createDialogArea(final Composite parent) {

		// The ModifyListener used on all controls. It will update the meta
		// object to
		// indicate that changes are being made.
		ModifyListener lsMod = new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				getJobEntry().setChanged();
			}
		};

		// Connection line
		wConnection = addConnectionLine(parent, null, props.getMiddlePct(), Const.MARGIN);
		if (this.getJobEntry().getDatabase() == null && jobMeta.nrDatabases() == 1) {
			wConnection.select(0);
		}
		wConnection.addModifyListener(lsMod);

		List<String> items = new ArrayList<String>();
		for (DatabaseMeta database : jobMeta.getDatabases()) {
			if (database.getDatabaseInterface() instanceof DominoDatabaseMeta) {
				items.add(database.getName());
			}
		}
		wConnection.setItems(items.toArray(new String[items.size()]));
		if (this.getJobEntry().getDatabase() == null && jobMeta.nrDatabases() == 1) {
			wConnection.select(0);
		}

		// wConnection.addModifyListener(lsConnectionMod);
		// wConnection.addSelectionListener(lsSelection);
		// wConnection.addFocusListener(lsConnectionFocus);
		wConnection.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				// We have new content: change database connection:
				getJobEntry().setDatabase(jobMeta.findDatabase(wConnection.getText()));
				getJobEntry().setChanged();
			}
		});

		// Widget Agent
		SelectionAdapter lsButton = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				onSelectAgent();
			}
		};

		Label wlAgentName = new Label(parent, SWT.RIGHT);
		wlAgentName.setText(BaseMessages.getString(PKG, "JobEntryDominoRunAgentDialog.Agent.Label"));
		wlAgentName.setToolTipText(BaseMessages.getString(PKG, "JobEntryDominoRunAgentDialog.Agent.Tooltip"));
		wlAgentName.setLayoutData(
				new FormDataBuilder().top(wConnection).right(props.getMiddlePct(), -Const.MARGIN).result());
		props.setLook(wlAgentName);

		wAgentName = new TextVarButton(jobMeta, parent, SWT.SINGLE | SWT.LEFT | SWT.BORDER, null, null, lsButton);
		wAgentName.setLayoutData(
				new FormDataBuilder().top(wConnection).left(props.getMiddlePct(), 0).right(100, 0).result());
		wAgentName.addModifyListener(lsMod);
		props.setLook(wAgentName);

		return parent;
	}

	@Override
	public Point getMinimumSize() {
		return new Point(650, 440);
	}

	private void onSelectAgent() {

		try {
			shell.setCursor(shell.getDisplay().getSystemCursor(SWT.CURSOR_WAIT));
			String[] agentNames = getJobEntry().getAgentNames();
			shell.setCursor(null);
			
			EnterSelectionDialog dialog = new EnterSelectionDialog(this.shell, agentNames,
					BaseMessages.getString(PKG, "JobEntryDominoRunAgentDialog.AgentSelection.DialogTitle"),
					BaseMessages.getString(PKG, "JobEntryDominoRunAgentDialog.AgentSelection.DialogMessage"), null);

			String result = dialog.open();

			if (result != null) {
				this.getJobEntry().setChanged();
				wAgentName.setText(result);
			}
		} catch (Exception e) {
			new ErrorDialog(shell,
					BaseMessages.getString(PKG, "JobEntryDominoRunAgentDialog.AgentSelection.DialogTitle"),
					BaseMessages.getString(PKG, "JobEntryDominoRunAgentDialog.AgentSelection.DialogError"),
					e);
		}
		
	}

	@Override
	protected void loadMeta(final JobEntryDominoRunAgent jobEntry) {

		wAgentName.setText(Const.NVL(jobEntry.getAgent(), ""));

		if (jobEntry.getDatabase() != null) {
			wConnection.setText(jobEntry.getDatabase().getName());
		} else if (jobMeta.nrDatabases() == 1) {
			wConnection.setText(jobMeta.getDatabase(0).getName());
		}

	}

	@Override
	protected void saveMeta(final JobEntryDominoRunAgent jobEntry) {
		jobEntry.setDatabase(jobMeta.findDatabase(wConnection.getText()));
		jobEntry.setAgent(wAgentName.getText());
	}

}
