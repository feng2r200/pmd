package net.sourceforge.pmd.eclipse.preferences;


import java.io.IOException;
import java.util.Iterator;
import java.util.Properties;
import java.util.StringTokenizer;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbench;
import net.sourceforge.pmd.eclipse.PMDPlugin;
import org.eclipse.jface.preference.IPreferenceStore;
import net.sourceforge.pmd.*;
import org.eclipse.jface.dialogs.MessageDialog;

/**
 * 
 * This page is used to modify preferences only. They
 * are stored in the preference store that belongs to
 * the main plug-in class. That way, preferences can
 * be accessed directly via the preference store.
 */

public class PMDPreferencePage
	extends PreferencePage
	implements IWorkbenchPreferencePage {

	private List rulesetsList;
	private Combo newEntryCombo;
	private Label title;

	public PMDPreferencePage() {
		super();
	}

	public void init(IWorkbench workbench) {
		setPreferenceStore(PMDPlugin.getDefault().getPreferenceStore());
		setDescription("PMD RuleSet Configuration Options");
	}
	

	/**
	 * @see org.eclipse.jface.preference.PreferencePage#performDefaults()
	 */
	protected void performDefaults() {
		rulesetsList.setItems(PMDPlugin.getDefault().getDefaultRuleSetsPreference());
	}

	/**
	 * @see org.eclipse.jface.preference.IPreferencePage#performOk()
	 */
	public boolean performOk() {
		PMDPlugin.getDefault().setRuleSetsPreference(rulesetsList.getItems());
		return super.performOk();
	}

	/**
	 * @see org.eclipse.jface.preference.PreferencePage#createContents(Composite)
	 */
	protected Control createContents(Composite parent) {

		Composite entryTable = new Composite(parent, SWT.NULL);

		//Create a data that takes up the extra space in the dialog .
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.grabExcessHorizontalSpace = true;
		entryTable.setLayoutData(data);

		GridLayout layout = new GridLayout();
		entryTable.setLayout(layout);			
				
		//Add in a dummy label for spacing
		new Label(entryTable,SWT.NONE);

		title = new Label(entryTable, SWT.NONE);
		title.setText("Active Rule Sets");
		data = new GridData(GridData.FILL_HORIZONTAL);
		title.setLayoutData(data);
		
		rulesetsList = new List(entryTable, SWT.BORDER);
		rulesetsList.setItems(PMDPlugin.getDefault().getRuleSetsPreference());
		//Create a data that takes up the extra space in the dialog and spans both columns.
		data = new GridData(GridData.FILL_BOTH);
		rulesetsList.setLayoutData(data);
		
		Composite buttonComposite = new Composite(entryTable,SWT.NULL);
		
		GridLayout buttonLayout = new GridLayout();
		buttonLayout.numColumns = 2;
		buttonComposite.setLayout(buttonLayout);

		//Create a data that takes up the extra space in the dialog and spans both columns.
		data = new GridData(GridData.FILL_BOTH | GridData.VERTICAL_ALIGN_BEGINNING);
		buttonComposite.setLayoutData(data);		
		
		Button addButton = new Button(buttonComposite, SWT.PUSH | SWT.CENTER);

		addButton.setText("Add Rule Set to List"); //$NON-NLS-1$
		addButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				rulesetsList.add(newEntryCombo.getText(), rulesetsList.getItemCount());
			}
		});
		
		//create the combox box to add new rulesets
		newEntryCombo = new Combo(buttonComposite, SWT.BORDER);
		//Create a data that takes up the extra space in the dialog .
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.grabExcessHorizontalSpace = true;
		newEntryCombo.setLayoutData(data);
		//populate the combo list with the items ruleset properties file
		Properties props = new Properties();
		try {
			props.load(getClass().getClassLoader().getResourceAsStream("rulesets/rulesets.properties"));
			String rulesetFilenames = props.getProperty("rulesets.filenames");
			for (StringTokenizer st = new StringTokenizer(rulesetFilenames, ","); st.hasMoreTokens(); ) {
				newEntryCombo.add(st.nextToken());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	
		
		Button removeButton = new Button(buttonComposite, SWT.PUSH | SWT.CENTER);

		removeButton.setText("Remove Rule Set from List"); //$NON-NLS-1$
		removeButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				rulesetsList.remove(rulesetsList.getSelectionIndex());
			}
		});
		
		data = new GridData();
		data.horizontalSpan = 2;
		removeButton.setLayoutData(data);
	
		return entryTable;

	}

}