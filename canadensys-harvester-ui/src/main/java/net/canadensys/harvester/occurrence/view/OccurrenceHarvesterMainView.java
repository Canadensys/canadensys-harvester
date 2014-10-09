package net.canadensys.harvester.occurrence.view;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ExecutionException;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileFilter;

import net.canadensys.dataportal.occurrence.model.ImportLogModel;
import net.canadensys.dataportal.occurrence.model.ResourceModel;
import net.canadensys.harvester.occurrence.controller.StepControllerIF;
import net.canadensys.harvester.occurrence.model.IPTFeedModel;
import net.canadensys.harvester.occurrence.model.JobStatusModel;
import net.canadensys.harvester.occurrence.model.JobStatusModel.JobStatus;
import net.canadensys.harvester.occurrence.view.model.HarvesterViewModel;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Main UI to control the harvester.
 * 
 * @author canadensys
 * 
 */
public class OccurrenceHarvesterMainView implements PropertyChangeListener {

	private String END_LINE = System.getProperty("line.separator");
	private JFrame harvesterFrame = null;

	private JPanel mainPanel = null;
	private JTextField pathToImportTxt = null;
	private ResourceModel resourceToImport = null;

	// Action buttons
	private JButton openFileBtn = null;
	private JButton openResourceBtn = null;
	private JButton importBtn = null;
	private JButton moveToPublicBtn = null;

	private JTextField bufferSchemaTxt = null;

	private ImageIcon loadingImg = null;
	private JLabel loadingLbl = null;
	private JTextArea statuxTxtArea = null;

	private JButton viewImportLogBtn = null;
	private JButton viewIPTFeedBtn = null;

	@Autowired
	private HarvesterViewModel harvesterViewModel;

	@Autowired
	@Qualifier(value = "stepController")
	private StepControllerIF stepController;

	public void initView() {
		loadingImg = new ImageIcon(
				OccurrenceHarvesterMainView.class
				.getResource("/ajax-loader.gif"));

		harvesterFrame = new JFrame(Messages.getString("view.title"));
		harvesterFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		mainPanel = new JPanel();
		mainPanel.setLayout(new GridBagLayout());
		pathToImportTxt = new JTextField();
		pathToImportTxt.setColumns(30);
		pathToImportTxt.setEditable(false);
		openFileBtn = new JButton(Messages.getString("view.button.openFile"));
		openResourceBtn = new JButton(
				Messages.getString("view.button.openResource"));
		openFileBtn.setEnabled(false);
		openFileBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				onChooseFile();
			}
		});

		openResourceBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				onOpenResourceBtn();
			}
		});

		importBtn = new JButton(Messages.getString("view.button.import"));
		importBtn.setToolTipText(Messages
				.getString("view.button.import.tooltip"));
		importBtn.setEnabled(false);
		importBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				onImportResource();
			}
		});

		int lineIdx = 0;
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = lineIdx;
		c.anchor = GridBagConstraints.CENTER;
		c.gridwidth = 3;
		JLabel lbl = new JLabel(Messages.getString("view.info.currentDatabase")
				+ harvesterViewModel.getDatabaseLocation());
		lbl.setForeground(Color.BLUE);
		mainPanel.add(lbl, c);

		// UI line break
		lineIdx++;
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = lineIdx;
		c.anchor = GridBagConstraints.WEST;
		mainPanel.add(new JLabel(Messages.getString("view.info.import.dwca")),
				c);

		// UI line break
		lineIdx++;
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = lineIdx;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 0.8;
		mainPanel.add(pathToImportTxt, c);

		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = lineIdx;
		mainPanel.add(openResourceBtn, c);

		c = new GridBagConstraints();
		c.gridx = 2;
		c.gridy = lineIdx;
		mainPanel.add(openFileBtn, c);

		// UI line break
		lineIdx++;
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = lineIdx;
		mainPanel.add(importBtn, c);

		// UI line break
		lineIdx++;
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = lineIdx;
		c.gridwidth = 3;
		c.fill = GridBagConstraints.HORIZONTAL;
		mainPanel.add(new JSeparator(), c);

		// UI line break
		lineIdx++;
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = lineIdx;
		c.gridwidth = 2;
		c.anchor = GridBagConstraints.WEST;
		mainPanel.add(new JLabel(Messages.getString("view.info.move.dwca")), c);

		// UI line break
		lineIdx++;
		moveToPublicBtn = new JButton(Messages.getString("view.button.move"));
		moveToPublicBtn.setToolTipText(Messages
				.getString("view.button.move.tooltip"));
		moveToPublicBtn.setEnabled(false);
		moveToPublicBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				onMoveToPublic();
			}
		});
		c = new GridBagConstraints();
		c.gridx = 2;
		c.gridy = lineIdx;
		c.anchor = GridBagConstraints.SOUTH;
		mainPanel.add(moveToPublicBtn, c);

		bufferSchemaTxt = new JTextField();
		bufferSchemaTxt.setEnabled(false);
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = lineIdx;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1.0;
		mainPanel.add(bufferSchemaTxt, c);

		// UI line break
		lineIdx++;
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = lineIdx;
		c.gridwidth = 3;
		c.fill = GridBagConstraints.HORIZONTAL;
		mainPanel.add(new JSeparator(), c);

		// UI line break
		lineIdx++;
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = lineIdx;
		c.anchor = GridBagConstraints.WEST;
		mainPanel.add(new JLabel(Messages.getString("view.info.status")), c);

		// UI line break
		lineIdx++;
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = lineIdx;
		c.anchor = GridBagConstraints.WEST;
		loadingLbl = new JLabel(Messages.getString("view.info.status.waiting"),
				null, JLabel.CENTER);
		mainPanel.add(loadingLbl, c);

		// UI line break
		lineIdx++;
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = lineIdx;
		c.gridwidth = 3;
		c.fill = GridBagConstraints.HORIZONTAL;
		mainPanel.add(new JSeparator(), c);

		// UI line break
		lineIdx++;
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = lineIdx;
		c.gridwidth = 2;
		c.anchor = GridBagConstraints.WEST;
		mainPanel.add(new JLabel(Messages.getString("view.info.console")), c);

		// UI line break
		lineIdx++;
		statuxTxtArea = new JTextArea();
		statuxTxtArea.setRows(15);
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = lineIdx;
		c.gridwidth = 3;
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1.0;
		c.weighty = 1.0;
		mainPanel.add(new JScrollPane(statuxTxtArea), c);

		// UI line break
		lineIdx++;
		viewImportLogBtn = new JButton(
				Messages.getString("view.button.viewImportLog"));
		viewImportLogBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				onViewImportLog();
			}
		});
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = lineIdx;
		c.anchor = GridBagConstraints.WEST;
		mainPanel.add(viewImportLogBtn, c);

		// UI line break
		lineIdx++;
		viewIPTFeedBtn = new JButton(
				Messages.getString("view.button.viewIPTrss"));
		viewIPTFeedBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				onViewIPTFeed();
			}
		});
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = lineIdx;
		c.anchor = GridBagConstraints.WEST;
		mainPanel.add(viewIPTFeedBtn, c);

		// inner panel
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1.0;
		c.weighty = 1.0;

		harvesterFrame.setLayout(new GridBagLayout());
		harvesterFrame.add(mainPanel, c);
		harvesterFrame.pack();
		harvesterFrame.setLocationRelativeTo(null);
		harvesterFrame.setVisible(true);

		redirectSystemStreams();

		//register to received all updates to the model
		harvesterViewModel.addPropertyChangeListener(this);
		
		checkForOutdatedResources();
	}
	
	/**
	 * Display in text area resource that should be (re)harvested.
	 */
	private void checkForOutdatedResources(){
		List<ResourceModel> outdatedResources = stepController.getResourceToHarvest();
		for(ResourceModel currResourceModel : outdatedResources){
			appendToTextArea(Messages.getString("view.info.harvestRequired") + currResourceModel.getSourcefileid()+"\n");
		}
	}

	/**
	 * Open a FileChooser to import a specific DarwinCore archive
	 */
	private void onChooseFile() {
		JFileChooser fc = new JFileChooser();
		fc.setDialogTitle(Messages.getString("view.fileChooser.title"));
		fc.setMultiSelectionEnabled(false);
		fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
		fc.setFileFilter(new FileFilter() {
			@Override
			public String getDescription() {
				return Messages.getString("view.fileChooser.description");
			}

			@Override
			public boolean accept(File file) {
				if (FilenameUtils.getExtension(file.getName())
						.equalsIgnoreCase("zip")) {
					return true;
				}
				return false;
			}
		});
		int returnVal = fc.showOpenDialog(harvesterFrame);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			pathToImportTxt.setText(fc.getSelectedFile().getAbsolutePath());
			importBtn.setEnabled(true);
		}
	}

	/**
	 * Open a window to select a resource to import.
	 */
	private void onOpenResourceBtn() {
		ResourceChooser urlChooser = new ResourceChooser(stepController,
				stepController.getResourceModelList());
		urlChooser.setLocationRelativeTo(harvesterFrame);
		urlChooser.setVisible(true);

		ResourceModel selectedResource = urlChooser.getSelectedResource();
		if (selectedResource != null) {
			pathToImportTxt.setText(selectedResource.getName());
			importBtn.setEnabled(true);
		}
		// remember the last selected resource
		resourceToImport = selectedResource;
	}

	/**
	 * Import a resource (asynchronously) using a SwingWorker.
	 */
	private void onImportResource() {
		openResourceBtn.setEnabled(false);
		importBtn.setEnabled(false);
		moveToPublicBtn.setEnabled(false);
		loadingLbl.setIcon(loadingImg);

		final SwingWorker<Void, Object> swingWorker = new SwingWorker<Void, Object>() {
			@Override
			public Void doInBackground() {
				try{
					if(resourceToImport != null){
						stepController.importDwcA(resourceToImport.getId());
					}
					else{
						if(StringUtils.isNotBlank(pathToImportTxt.getText())){
							stepController.importDwcAFromLocalFile(pathToImportTxt.getText());
						}
					}
				}
				catch (Exception e) {
					// should not get there but just in case
					e.printStackTrace();
					
				}
				// async call, propertyChange(...) will be called once done
				return null;
			}

			@Override
			protected void done() {
			}
			
		};
		swingWorker.execute();
	}

	/**
	 * Move the previously importer resource to the public schema using a
	 * SwingWorker.
	 */
	private void onMoveToPublic() {
		moveToPublicBtn.setEnabled(false);
		loadingLbl.setIcon(loadingImg);

		final SwingWorker<Boolean, Object> swingWorker = new SwingWorker<Boolean, Object>() {
			@Override
			public Boolean doInBackground() {
				stepController.moveToPublicSchema(bufferSchemaTxt.getText(),resourceToImport.getResource_uuid());
				return true;
			}

			@Override
			protected void done() {
				try {
					if (get()) {
						onMoveDone(JobStatus.DONE);
					} else {
						onMoveDone(JobStatus.ERROR);
					}
				} catch (InterruptedException e) {
					onMoveDone(JobStatus.ERROR);
				} catch (ExecutionException e) {
					onMoveDone(JobStatus.ERROR);
				}
			}
		};
		swingWorker.execute();
	}

	private void onViewImportLog() {
		Vector<String> headers = new Vector<String>();
		headers.add(Messages.getString("view.importLog.table.sourceFileId"));
		headers.add(Messages.getString("view.importLog.table.recordQty"));
		headers.add(Messages.getString("view.importLog.table.updatedBy"));
		headers.add(Messages.getString("view.importLog.table.date"));

		ImportLogDialog dlg = new ImportLogDialog(headers);

		List<ImportLogModel> importLogModelList = stepController
				.getSortedImportLogModelList();
		dlg.loadData(importLogModelList);
		dlg.setLocationRelativeTo(null);
		dlg.setVisible(true);
	}

	private void onViewIPTFeed() {
		Vector<String> headers = new Vector<String>();
		headers.add(Messages.getString("view.iptFeed.table.title"));
		headers.add(Messages.getString("view.iptFeed.table.url"));
		headers.add(Messages.getString("view.iptFeed.table.key"));
		headers.add(Messages.getString("view.iptFeed.table.pubDate"));

		IPTFeedDialog dlg = new IPTFeedDialog(headers);

		List<IPTFeedModel> importLogModelList = stepController.getIPTFeed();
		dlg.loadData(importLogModelList);
		dlg.setLocationRelativeTo(null);
		dlg.setVisible(true);
	}
	
	private void onJobStatusChanged(JobStatus newStatus){
		switch (newStatus) {
			case DONE: 
				loadingLbl.setIcon(null);
				bufferSchemaTxt.setText(resourceToImport.getSourcefileid());
				moveToPublicBtn.setEnabled(true);
				updateStatusLabel(Messages.getString("view.info.status.importDone"));
				break;
			case ERROR:
				loadingLbl.setIcon(null);
				updateStatusLabel(Messages.getString("view.info.status.error.importError"));
				JOptionPane.showMessageDialog(
						harvesterFrame,
						Messages.getString("view.info.status.error.details"),
						Messages.getString("view.info.status.error"), JOptionPane.ERROR_MESSAGE);
				break;
			case CANCEL:
				loadingLbl.setIcon(null);
				updateStatusLabel(Messages.getString("view.info.status.canceled"));
				break;
			default:
				break;
		}
	}
	
	private void updateStatusLabel(final String status){
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				loadingLbl.setText(status);
			}
		});
	}

	private void onMoveDone(JobStatus status) {
		loadingLbl.setIcon(null);
		if (JobStatus.DONE == status) {
			JOptionPane.showMessageDialog(harvesterFrame,
					Messages.getString("view.info.status.moveCompleted"),
					Messages.getString("view.info.status.info"),
					JOptionPane.INFORMATION_MESSAGE);
			bufferSchemaTxt.setText("");
			pathToImportTxt.setText("");
			loadingLbl.setText(Messages.getString("view.info.status.moveDone"));
		} else {
			JOptionPane.showMessageDialog(harvesterFrame,
					Messages.getString("view.info.status.error.details"),
					Messages.getString("view.info.status.error"),
					JOptionPane.ERROR_MESSAGE);
			loadingLbl.setText(Messages
					.getString("view.info.status.error.moveError"));
		}
	}

	private void appendToTextArea(final String text) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				statuxTxtArea.append(text);
			}
		});
	}

	/**
	 * TODO remove this hack.
	 */
	private void redirectSystemStreams() {
		OutputStream out = new OutputStream() {
			@Override
			public void write(int b) throws IOException {
				appendToTextArea(String.valueOf((char) b));
			}

			@Override
			public void write(byte[] b, int off, int len) throws IOException {
				appendToTextArea(new String(b, off, len));
			}

			@Override
			public void write(byte[] b) throws IOException {
				write(b, 0, b.length);
			}
		};

		System.setOut(new PrintStream(out, true));
		System.setErr(new PrintStream(out, true));
	}

	public void updateProgressText(final String text) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				loadingLbl.setText(text);
			}
		});
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if(JobStatusModel.CURRENT_STATUS_EXPLANATION_PROPERTY.equals(evt.getPropertyName())) {
			appendToTextArea(">"+(String)evt.getNewValue() + END_LINE);
		}
		else if(JobStatusModel.CURRENT_STATUS_PROPERTY.equals(evt.getPropertyName())) {
			appendToTextArea("STATUS:"+evt.getNewValue() + END_LINE);
			onJobStatusChanged((JobStatus)evt.getNewValue());
		}
		else if(JobStatusModel.CURRENT_JOB_PROGRESS_PROPERTY.equals(evt.getPropertyName())) {
			updateProgressText((String)evt.getNewValue());
		}
	}
}
