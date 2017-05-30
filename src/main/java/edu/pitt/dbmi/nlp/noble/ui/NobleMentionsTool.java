package edu.pitt.dbmi.nlp.noble.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.html.HTMLDocument;

import edu.pitt.dbmi.nlp.noble.eval.AnnotationEvaluation;
import edu.pitt.dbmi.nlp.noble.mentions.NobleMentions;
import edu.pitt.dbmi.nlp.noble.mentions.model.AnnotationVariable;
import edu.pitt.dbmi.nlp.noble.mentions.model.Composition;
import edu.pitt.dbmi.nlp.noble.mentions.model.DomainOntology;
import edu.pitt.dbmi.nlp.noble.mentions.model.Instance;
import edu.pitt.dbmi.nlp.noble.ontology.DefaultRepository;
import edu.pitt.dbmi.nlp.noble.ontology.IOntology;
import edu.pitt.dbmi.nlp.noble.ontology.IOntologyException;
import edu.pitt.dbmi.nlp.noble.ontology.IRepository;
import edu.pitt.dbmi.nlp.noble.terminology.CompositTerminology;
import edu.pitt.dbmi.nlp.noble.terminology.TerminologyError;
import edu.pitt.dbmi.nlp.noble.terminology.TerminologyException;
import edu.pitt.dbmi.nlp.noble.tools.ConText;
import edu.pitt.dbmi.nlp.noble.tools.TextTools;
import edu.pitt.dbmi.nlp.noble.util.CSVExporter;
import edu.pitt.dbmi.nlp.noble.util.FileTools;
import edu.pitt.dbmi.nlp.noble.util.HTMLExporter;
import edu.pitt.dbmi.nlp.noble.util.UITools;






/**
 * process a set of reports and generate an HTML to get.
 *
 * @author tseytlin
 */
public class NobleMentionsTool implements ActionListener{
	private final URL LOGO_ICON = getClass().getResource("/icons/NobleLogo256.png");
	private final URL CANCEL_ICON = getClass().getResource("/icons/cancel16.png");
	private JFrame frame;
	private JTextField input,output;
	private JList<DomainOntology> ontologyList;
	private JTextArea console;
	private JProgressBar progress;
	private JPanel buttonPanel,progressPanel,optionsPanel;
	private JButton run;
	private File lastFile;
	private long totalTime;
	private long processCount;
	private HTMLExporter htmlExporter;
	private CSVExporter csvExporter;
	private static boolean statandlone = false;
	private DefaultRepository repository = new DefaultRepository();
	private boolean cancelRun;
	private Map<String,Long> processTime;
	
	
	// options
	private ButtonGroup annotationScope ;
	private JCheckBox processHeaderAnchor;
	private JCheckBox processHeaderModifier;
	private JCheckBox normalizeAnchors ;
	private JCheckBox scoreAnchors;
	private JCheckBox ignoreLabels;
	private JRadioButton sectionScope,paragraphScope; 
	
	
	
	/**
	 * What .
	 *
	 * @param args the arguments
	 * @throws Exception the exception
	 */
	public static void main(String[] args) throws Exception {
		statandlone = true;
		NobleMentionsTool nc = new NobleMentionsTool();
		if(args.length == 0){
			nc.showDialog();
		}else if(args.length == 3){
			nc.process(new DomainOntology(args[0]),args[1],args[2]);
		}else{
			System.err.println("Usage: java InformationExtractor [template] [input directory] [output directory]");
			System.err.println("Note:  If you don't specify parameters, GUI will pop-up");
		}
	}

	
	/**
	 * create dialog for noble coder.
	 */
	public void showDialog(){
		if(frame == null){
			frame = new JFrame("Noble Mentions");
			frame.setDefaultCloseOperation(statandlone?JFrame.EXIT_ON_CLOSE:JFrame.DISPOSE_ON_CLOSE);
			frame.setJMenuBar(getMenuBar());
			frame.setIconImage(new ImageIcon(LOGO_ICON).getImage());
			
			JPanel panel = new JPanel();
			panel.setLayout(new BoxLayout(panel,BoxLayout.Y_AXIS));
			GridBagConstraints c = new GridBagConstraints(0,0,1,1,1,1,GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,new Insets(5,5,5,5),0,0);
			GridBagLayout l = new GridBagLayout();
			l.setConstraints(panel,c);
			panel.setLayout(l);
			
			input = new JTextField(30);
			ontologyList = new JList(new DefaultListModel<DomainOntology>());
			JButton browse = new JButton("Browse");
			browse.addActionListener(this);
			browse.setActionCommand("i_browser");
		
			
			JButton options = new JButton("Options");
			options.setActionCommand("options");
			options.addActionListener(this);
			JButton add = new JButton("Import");
			add.setActionCommand("import");
			add.addActionListener(this);
			JButton info = new JButton("Preview");
			info.setActionCommand("preview");
			info.addActionListener(this);
			JScrollPane scroll = new JScrollPane(ontologyList);
			scroll.setPreferredSize(new Dimension(100,130));
			
			JButton eval = new JButton("Evaluate");
			eval.setActionCommand("eval");
			eval.addActionListener(this);
			
			panel.add(new JLabel("Input Schema"),c);c.gridx++;c.gridheight=4;
			panel.add(scroll,c);c.gridx++;c.gridheight=1;
			panel.add(add,c);c.gridy++;
			panel.add(info,c);c.gridy++;
			panel.add(options,c);c.gridy++;
			panel.add(eval,c);c.gridy++;
			c.gridx = 0;
			panel.add(new JLabel("Input Directory "),c);c.gridx++;
			panel.add(input,c);c.gridx++;
			panel.add(browse,c);c.gridx=0;c.gridy++;
	
			output = new JTextField(30);
			browse = new JButton("Browse");
			browse.addActionListener(this);
			browse.setActionCommand("o_browser");
		
			panel.add(new JLabel("Output Directory"),c);c.gridx++;
			panel.add(output,c);c.gridx++;
			panel.add(browse,c);c.gridx=0;c.gridy++;
			panel.add(Box.createRigidArea(new Dimension(10,10)),c);
			
			JPanel conp = new JPanel();
			conp.setLayout(new BorderLayout());
			conp.setBorder(new TitledBorder("Output Console"));
			console = new JTextArea(10,40);
			//console.setLineWrap(true);
			console.setEditable(false);
			conp.add(new JScrollPane(console),BorderLayout.CENTER);
			//c.gridwidth=3;		
			//panel.add(conp,c);c.gridy++;c.gridx=0;
			
			buttonPanel = new JPanel();
			buttonPanel.setLayout(new BorderLayout());
			buttonPanel.setBorder(new EmptyBorder(10,30,10,30));
			run = new JButton("Run Noble Mentions");
			run.addActionListener(this);
			run.setActionCommand("run");
			buttonPanel.add(run,BorderLayout.CENTER);
			//panel.add(buttonPanel,c);
			
			JButton cancel = new JButton(new ImageIcon(CANCEL_ICON));
			cancel.setToolTipText("Cancel Run");
			cancel.addActionListener(this);
			cancel.setActionCommand("cancel");
			
			progress = new JProgressBar();
			progress.setIndeterminate(true);
			progress.setString("Please Wait. It will take a while ...");
			progress.setStringPainted(true);
			
			progressPanel = new JPanel();
			progressPanel.setLayout(new BorderLayout());
			progressPanel.add(progress,BorderLayout.CENTER);
			progressPanel.add(cancel,BorderLayout.EAST);
			
			
			JPanel p = new JPanel();
			p.setLayout(new BorderLayout());
			p.add(panel,BorderLayout.NORTH);
			p.add(conp,BorderLayout.CENTER);
			p.add(buttonPanel,BorderLayout.SOUTH);
			
				
			// wrap up, and display
			frame.setContentPane(p);
			frame.pack();
		
			//center on screen
			Dimension d = frame.getSize();
			Dimension s = Toolkit.getDefaultToolkit().getScreenSize();
			frame.setLocation(new Point((s.width-d.width)/2,(s.height-d.height)/2));
			
			frame.setVisible(true);
			// load defaults
			loadDeafaults();
			loadSettings();
		}else{
			frame.setVisible(true);
		}
	}	
	
	/**
	 * save UI settings
	 */
	private void saveSettings(){
		Properties p = new Properties();
		p.setProperty("ontology",ontologyList.getSelectedValue().toString());
		p.setProperty("input",input.getText());
		p.setProperty("output",output.getText());
		UITools.saveSettings(p,getClass());
	}
	
	/**
	 * save UI settings
	 */
	private void saveOptionsSettings(){
		DomainOntology ontology = ontologyList.getSelectedValue();
		if(ontology != null){
			Properties p = new Properties();
			p.setProperty("annotation.relation.scope",annotationScope.getSelection().getActionCommand());
			p.setProperty("process.header.anchors",""+processHeaderAnchor.isSelected());
			p.setProperty("process.header.modifiers",""+processHeaderModifier.isSelected());
			p.setProperty("normalize.anchors",""+normalizeAnchors.isSelected());
			p.setProperty("score.anchors",""+scoreAnchors.isSelected());
			p.setProperty("ignore.labels",""+ignoreLabels.isSelected());
			UITools.saveSettings(p,new File(ontology.getOntologyLocation().getParentFile(),ontology.getName()+".properties"));
		}
	}
	
	
	/**
	 * save UI settings
	 */
	private void loadOptionsSettings(){
		getOptionsPanel();
		DomainOntology ontology = ontologyList.getSelectedValue();
		if(ontology != null){
			final Properties p = UITools.loadSettings(new File(ontology.getOntologyLocation().getParentFile(),ontology.getName()+".properties"));
			sectionScope.setSelected("section".equals(p.getProperty("annotation.relation.scope")));
			processHeaderAnchor.setSelected(Boolean.parseBoolean(p.getProperty("process.header.anchors")));
			processHeaderModifier.setSelected(Boolean.parseBoolean(p.getProperty("process.header.modifiers")));
			normalizeAnchors.setSelected(Boolean.parseBoolean(p.getProperty("normalize.anchors")));
			scoreAnchors.setSelected(Boolean.parseBoolean(p.getProperty("score.anchors")));
			ignoreLabels.setSelected(Boolean.parseBoolean(p.getProperty("ignore.labels")));
		}
	}
	
	private void loadOptions(NobleMentions nobleMentions) {
		getOptionsPanel();
		nobleMentions.setProcessAnchorsInHeader(processHeaderAnchor.isSelected());
		nobleMentions.setProcessModifiersInHeader(processHeaderModifier.isSelected());	
		nobleMentions.getDomainOntology().setAnnotatioRelationSkope(annotationScope.getSelection().getActionCommand());
		nobleMentions.getDomainOntology().setNormalizeAnchorTerms(normalizeAnchors.isSelected());
		nobleMentions.getDomainOntology().setScoreAnchors(scoreAnchors.isSelected());
		saveOptionsSettings();
	}
	
	
	/**
	 * save UI settings
	 */
	private void loadSettings(){
		final Properties p = UITools.loadSettings(getClass());
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				if(p.containsKey("input"))
					input.setText(p.getProperty("input"));
				if(p.containsKey("output"))
					output.setText(p.getProperty("output"));
				if(p.containsKey("ontology")){
					selectOntology(p.getProperty("ontology"));
				}
			}
		});
	
	}
	
	/**
	 * Load deafaults.
	 */
	private void loadDeafaults(){
		(new Thread(new Runnable(){
			public void run(){
				setBusy(true);
				refreshTemplateList();
				setBusy(false);
			}
		})).start();
	}
	
	/**
	 * Refresh template list.
	 */
	private void refreshTemplateList(){
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				repository.reset();
				((DefaultListModel<DomainOntology>)ontologyList.getModel()).removeAllElements();
				IOntology [] ontologies = repository.getOntologies();
				Arrays.sort(ontologies,new Comparator<IOntology>() {
					public int compare(IOntology o1, IOntology o2) {
						return o1.getName().compareTo(o2.getName());
					}
				});
				for(IOntology t: ontologies){
					// filter out ontologies that are dependency
					if(ConText.IMPORTED_ONTOLOGIES.contains(t.getName()))
						continue;					
					try {
						((DefaultListModel<DomainOntology>)ontologyList.getModel()).addElement(new DomainOntology(t));
					} catch (IOntologyException e) {
						e.printStackTrace();
					}
				}
				ontologyList.validate();
			}
		});
	}
	
	
	/**
	 * set busy .
	 *
	 * @param b the new busy
	 */
	private void setBusy(boolean b){
		final boolean busy = b;
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				buttonPanel.removeAll();
				if(busy){
					progress.setIndeterminate(true);
					progress.setString("Please Wait. It may take a while ...");
					progress.setStringPainted(true);
					buttonPanel.add(progressPanel,BorderLayout.CENTER);
					console.setText("");
				}else{
					buttonPanel.add(run,BorderLayout.CENTER);
				}
				buttonPanel.validate();
				buttonPanel.repaint();
				
			}
		});
	}
	

	/**
	 * Gets the menu bar.
	 *
	 * @return the menu bar
	 */
	private JMenuBar getMenuBar() {
		JMenuBar menu = new JMenuBar();
		JMenu file = new JMenu("File");
		JMenuItem exit = new JMenuItem("Exit");
		exit.addActionListener(this);
		file.add(exit);
		menu.add(file);
		return menu;
	}

	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		if("run".equals(cmd)){
			doRun();
		}else if("cancel".equals(cmd)){
			cancelRun = true;
		}else if("i_browser".equals(cmd)){
			doBrowse(input);
		}else if("d_browser".equals(cmd)){
			
		}else if("o_browser".equals(cmd)){
			doBrowse(output);
		}else if("exit".equals(cmd)){
			System.exit(0);
		}else if("export".equals(cmd)){
			doExport();
		}else if("options".equals(cmd)){
			doOptions();
		}else if("import".equals(cmd)){
			doImport();
		}else if("preview".equals(cmd)){
			doPreview();
		}else if("eval".equals(cmd)){
			doEvaluate();
		}
	}
	
	private JPanel getOptionsPanel(){
		if(optionsPanel == null){
			optionsPanel = new JPanel();
			
			optionsPanel.setLayout(new BorderLayout());
			optionsPanel.setBorder(new CompoundBorder(new BevelBorder(BevelBorder.RAISED),new EmptyBorder(10, 10, 10, 10)));
			Color blue = new Color(100,100,255);
			
			// runtime options
			JPanel panel1 = new JPanel();
		
			TitledBorder border = new TitledBorder(new LineBorder(blue),"Runtime options");
			border.setTitleColor(blue);
			
			panel1.setBorder(new CompoundBorder(new EmptyBorder(0, 0, 5, 0),border));
			GridBagConstraints c = new GridBagConstraints(0,0,1,1,1,1,GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,new Insets(0,0,0,0),0,0);
			GridBagLayout l = new GridBagLayout();
			l.setConstraints(panel1,c);
			panel1.setLayout(l);
			
			// init controls
			sectionScope = new JRadioButton("Section",false);
			sectionScope.setActionCommand(DomainOntology.SECTION_SCOPE);
			paragraphScope = new JRadioButton("Paragraph",true);
			paragraphScope.setActionCommand(DomainOntology.PARAGRAPH_SCOPE);
			annotationScope = new ButtonGroup();
			annotationScope.add(sectionScope);
			annotationScope.add(paragraphScope);
			
			processHeaderAnchor = new JCheckBox("Anchors");
			processHeaderModifier = new JCheckBox("Modifiers");
			
			panel1.add(new JLabel("Scope of annotation to annotation relation"),c);c.gridx++;
			panel1.add(sectionScope,c);c.gridx++;
			panel1.add(paragraphScope,c);c.gridy++;c.gridx = 0;
			
			panel1.add(new JLabel("Process section header for"),c);c.gridx++;
			panel1.add(processHeaderAnchor,c);c.gridx++;
			panel1.add(processHeaderModifier,c);c.gridy++;c.gridx = 0;
			
			// set dictionary options
			normalizeAnchors = new JCheckBox("Normalize anchor terms");
			scoreAnchors = new JCheckBox("Score matched anchor terms");
			ignoreLabels = new JCheckBox("Ignore class labels as valid terms");
			
			JPanel panel2 = new JPanel();
			border = new TitledBorder(new LineBorder(blue),"Dictionary building options");
			border.setTitleColor(blue);
			panel2.setLayout(new BoxLayout(panel2,BoxLayout.Y_AXIS));
			panel2.setBorder(border);
			panel2.add(normalizeAnchors);
			panel2.add(scoreAnchors);
			panel2.add(ignoreLabels);
			
			optionsPanel.add(panel1,BorderLayout.CENTER);
			optionsPanel.add(panel2,BorderLayout.SOUTH);		
			
			
		}
		return optionsPanel;
	}
	
	private void doOptions() {
		loadOptionsSettings();
		boolean na = normalizeAnchors.isSelected();
		boolean sa = scoreAnchors.isSelected();
		boolean il = ignoreLabels.isSelected();
		
		int r = JOptionPane.showConfirmDialog(frame,getOptionsPanel(),"Runtime Options",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
		if(JOptionPane.OK_OPTION == r){
			DomainOntology ont = ontologyList.getSelectedValue();
			// if terminolgy rebuilding options changed, warn about them
			if(ont != null && ont.getTerminologyCacheLocation().exists() && (na != normalizeAnchors.isSelected() || sa != scoreAnchors.isSelected() ||  il != ignoreLabels.isSelected())){
				int rr = JOptionPane.showConfirmDialog(frame,
						"<html>You have changed one of the <font color=blue>dictionary building options</font>. <br>"+
						"Are you sure want to re-generate cached dictionaries for <font color=red> "+ont.getName()+"</font> domain?<br>"+
						"It will take additional time to re-generate dictionaries.","Warning",JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE);
				if(rr == JOptionPane.YES_OPTION){
					FileTools.deleteDirectory(ont.getTerminologyCacheLocation());
					saveOptionsSettings();
				}else{
					loadOptionsSettings();
				}
			}else{
				saveOptionsSettings();
			}
		}else{
			//roll back the settings
			loadOptionsSettings();
		}
	}


	private void doEvaluate() {
		AnnotationEvaluation ae = new AnnotationEvaluation();
		JDialog dialog = ae.getDialog(frame);
		DomainOntology ontology = ontologyList.getSelectedValue();
		if(ontology != null) {
			String name = ontology.getName() + "Instances.owl";
			ae.setSystemInstanceOntlogy(output.getText() + File.separator + name);
		}
		ae.setInputDocuments(input.getText());
		dialog.setVisible(true);
	}


	/**
	 * do preview.
	 */
	private void doPreview() {
		final DomainOntology t = ontologyList.getSelectedValue();
		if(t == null)
			return;
		new Thread(new Runnable() {
			public void run() {
				setBusy(true);
				TerminologyBrowser browser = new TerminologyBrowser();
				browser.setTerminologies(t.getTerminologies());
				browser.showDialog(null,"NobleMentions");
				setBusy(false);
				
			}
		}).start();
	
		
	}


	/**
	 * do export of highlighted template.
	 */
	private void doExport() {
		DomainOntology template = ontologyList.getSelectedValue();
		if(template != null){
			JFileChooser chooser = new JFileChooser();
			chooser.setFileFilter(new FileFilter(){
				public boolean accept(File f) {
					return f.isDirectory() || f.getName().endsWith(".owl");
				}
				public String getDescription() {
					return "OWL File";
				}
				
			});
			chooser.setSelectedFile(new File(template.getOntology().getName()));
			int r = chooser.showSaveDialog(frame);
			if(r == JFileChooser.APPROVE_OPTION){
				try{
					File f = chooser.getSelectedFile();
					Files.copy(new File(template.getOntology().getLocation()).toPath(),f.toPath());
					//FileOutputStream out = new FileOutputStream(f);
					//templateFactory.exportTemplate(template, out);
					//out.close();
					
				}catch(Exception ex){
					JOptionPane.showMessageDialog(frame,ex.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);
					ex.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * do export of highlighted template.
	 */
	private void doImport() {
		final JFileChooser chooser = new JFileChooser(lastFile);
		chooser.setFileFilter(new FileFilter(){
			public boolean accept(File f) {
				return f.isDirectory() || f.getName().endsWith(".owl");
			}
			public String getDescription() {
				return "OWL lastFile (extending Schema.owl)";
			}
			
		});
		int r = chooser.showOpenDialog(frame);
		if(r == JFileChooser.APPROVE_OPTION){
			
				new Thread(new Runnable(){
					public void run(){
						String ont = null;
						setBusy(true);
						try{
							lastFile = chooser.getSelectedFile();
							File newLocation = new File(repository.getOntologyLocation(),lastFile.getName());
							Files.copy(lastFile.toPath(),newLocation.toPath(),StandardCopyOption.REPLACE_EXISTING);
							ont = FileTools.stripExtension(newLocation.getName());
							
							// remove terminology cache
							File termCache = DomainOntology.getTerminologyCacheLocation(newLocation);
							if(termCache.exists()){
								FileTools.deleteDirectory(termCache);
							}
						}catch(Exception ex){
							JOptionPane.showMessageDialog(frame,ex.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);
							ex.printStackTrace();
						}
						refreshTemplateList();
						selectOntology(ont);
						
						setBusy(false);
					}
				}).start();
			
		}
	}

	/**
	 * select ontology
	 * @param ont
	 */
	private void selectOntology(final String ont){
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				int index = -1;
				for(int i=0;i<ontologyList.getModel().getSize();i++){
					if(ontologyList.getModel().getElementAt(i).toString().equals(ont)){
						index = i; break;
					}
				}
				if(index > -1)
					ontologyList.setSelectedIndex(index);
			}
		});
	}

	
     /**
      * check UI inputs.
      *
      * @return true, if successful
      */
    private boolean checkInputs(){
 		if(ontologyList.getSelectedValuesList().isEmpty()){
			JOptionPane.showMessageDialog(frame,"Please Select the Ontology");
			return false;
		}
		if(!new File(input.getText()).exists()){
			JOptionPane.showMessageDialog(frame,"Please Select Input Report Directory");
			return false;
		}
		return true;
    }
     
    /**
     * run the damn thing.
     */
	private void doRun() {
		(new Thread(new Runnable(){
			public void run() {
				if(!checkInputs()){
					return;
				}
				setBusy(true);
				updateOutputLocation();
				cancelRun = false;
				
				// save settings
				saveSettings();
				
				DomainOntology ontology = ontologyList.getSelectedValue();
				final String ontName = ontology.getName();
				
				// setup progress bar
				if(progress != null){
					SwingUtilities.invokeLater(new Runnable(){
						public void run(){
							progress.setIndeterminate(true);
							progress.setString("Loading "+ontName+" ...");
						}
					});
				}
				
				// create just-in-time instance lastFile
				try {
					long t = System.currentTimeMillis();
					progress("loading "+ontName+" ontology .. ");
					ontology = new DomainOntology(ontology.getOntology().getLocation());
					progress((System.currentTimeMillis()-t)+ " ms\n");
				} catch (IOntologyException e1) {
					UITools.showErrorDialog(frame,
							"<html>Could not load imported ontologies.<br>"
							+ "To procede in offline mode, please add imported ontologies to the local cache.<br> "
							+ "Please see documentation for details.");
					progress("\n"+e1.getMessage()+"\n");
					if(e1.getCause() != null)
						progress(e1.getCause().getMessage());
					e1.printStackTrace();
					setBusy(false);
					return;
				}
				
				
				// check if it is valid
				if(!ontology.isOntologyValid()){
					JOptionPane.showMessageDialog(frame,"Selected ontology "+ontology.getName()+" is not a valid "+DomainOntology.SCHEMA_OWL+" ontology","Error",JOptionPane.ERROR_MESSAGE);
					setBusy(false);
					return;
				}
				
				try {
					process(ontology,input.getText(),output.getText());
				} catch (Exception e) {
					UITools.showErrorDialog(frame,e);
				}
				
				
				setBusy(false);
				
				// open in browser
				try{
					UITools.browseURLInSystemBrowser(new File(output.getText()+File.separator+"index.html").toURI().toString());
				}catch(Exception ex){
					UITools.showErrorDialog(frame,ex);
				}
				
			
				
			}

			
		})).start();
	}
	
	
	/**
	 * Do browse.
	 *
	 * @param text the text
	 */
	private void doBrowse(JTextField text){
		File file = new File(text.getText());
		JFileChooser fc = new JFileChooser(file);
		fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		fc.addChoosableFileFilter(new FileFilter() {
			public String getDescription() {
				return "Text files (.txt)";
			}
			public boolean accept(File f) {
				return f.isDirectory() || f.getName().endsWith(".txt");
			}
		});
	
		int r = (output == text)?fc.showSaveDialog(frame):fc.showOpenDialog(frame);
		if(r == JFileChooser.APPROVE_OPTION){
			file = fc.getSelectedFile();
			text.setText(file.getAbsolutePath());
			
			// if input, change output to default
			if(text == input){
				setDefaultOutputLocation();
			}
		}
	}

	/**
	 * set default output location, based on input file
	 */
	private void setDefaultOutputLocation(){
		// derive output from input
		File file = new File(input.getText());
		if(file.exists()){
			String prefix = file.getName();
			if(prefix.endsWith(".txt"))
				prefix = prefix.substring(0,prefix.length()-4);
			prefix = prefix+File.separator+(new SimpleDateFormat("yyyy-MM-dd HH.mm.ss").format(new Date(System.currentTimeMillis())));
			output.setText(new File(file.getParent()+File.separator+"Output"+File.separator+prefix).getAbsolutePath());
		}
	}
	
	/**
	 * set default output location, based latest date time
	 */
	private void updateOutputLocation(){
		File file = new File(output.getText());
		Pattern pt = Pattern.compile("\\d{4}-\\d{2}-\\d{2} \\d{2}\\.\\d{2}\\.\\d{2}");
		Matcher mt = pt.matcher(file.getName());
		if(mt.matches()){
			String date = (new SimpleDateFormat("yyyy-MM-dd HH.mm.ss").format(new Date(System.currentTimeMillis())));
			output.setText(new File(file.getParentFile(),date).getAbsolutePath());
		}
	}
	
	
	
	/**
	 * process  documents.
	 * @param ontology the templates to process
	 * @param in the in
	 * @param out the out
	 */
	public void process(DomainOntology ontology,String in, String out){	
		// preload terminologies
		long t = System.currentTimeMillis();
		progress("loading anchors .. ");
		ontology.getAnchorTerminology();
		progress((System.currentTimeMillis()-t)+" ms\n");
		
		t = System.currentTimeMillis();
		progress("loading modifiers .. ");
		ontology.getModifierTerminology();
		progress((System.currentTimeMillis()-t)+" ms\n");
		
		t = System.currentTimeMillis();
		progress("loading sections .. ");
		ontology.getSectionTerminology();
		progress((System.currentTimeMillis()-t)+" ms\n");
		
		// start a new instance of noble mentions
		NobleMentions noble = new NobleMentions(ontology);
		
		// load options
		loadOptions(noble);
		
		
		// process lastFile
		List<File> files = FileTools.getFilesInDirectory(new File(in),".txt");
		if(progress != null){
			final int n = files.size();
			SwingUtilities.invokeLater(new Runnable(){
				public void run(){
					progress.setIndeterminate(false);
					progress.setString("Processing Reports ..");
					progress.setMaximum(n);
				}
			});
		}
		
		// process report
		File outputDir = new File(out);
		if(!outputDir.exists())
			outputDir.mkdirs();
		
		// initialize writers
		htmlExporter = new HTMLExporter(outputDir);
		csvExporter = new CSVExporter(outputDir);
		
		// reset stat counters
		processCount = 0;
		totalTime = 0;
		
		
	
		
		for(int i=0;i<files.size();i++){
			try {
				process(noble,files.get(i));
				
				// cancel processing
				if(cancelRun)
					break;
				
			} catch (Exception e) {
				progress("Error: "+e.getMessage());
				e.printStackTrace();
			}
			if(progress != null){
				final int n = i+1;
				SwingUtilities.invokeLater(new Runnable(){
					public void run(){
						progress.setValue(n);
					}
				});
			}
		}
		
		// wrap up
		try {
			ontology.write(new File(outputDir,ontology.getName()+".owl"));
			htmlExporter.flush();
			csvExporter.flush();
		} catch (Exception e) {
			progress("Error: "+e.getMessage());
			e.printStackTrace();
		}
		
		
		// summary
		if(processCount > 0){
			progress("\nTotal process time for all reports:\t"+totalTime+" ms\n");
			progress("Average process time per report:\t"+((totalTime)/processCount)+" ms\n");
		}
	}

	

	/**
	 * process report.
	 *
	 * @param templates the templates
	 * @param reportFile the report lastFile
	 * @throws Exception the exception
	 */
	private void process(NobleMentions noble,File reportFile) throws Exception {
		progress("processing report ("+(processCount+1)+") "+reportFile.getName()+" ... ");
		
		// read in the report, do first level proce
		Composition doc = noble.process(reportFile);
	
		processCount ++;
			
		// now output HTML for this report
		htmlExporter.export(doc);
		csvExporter.export(doc);
		
		// do progress
		totalTime += noble.getProcessTime();
		progress(noble.getProcessTime()+" ms\n");
	}
	
	/**
	 * Progress.
	 *
	 * @param str the str
	 */
	private void progress(String str){
		System.out.print(str);
		if(console != null){
			final String s = str;
			SwingUtilities.invokeLater(new Runnable(){
				public void run(){
					console.append(s);
				}
			});
			
		}
	}


}
