package edu.pitt.dbmi.nlp.noble.ui;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
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
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.html.HTMLDocument;

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
import edu.pitt.dbmi.nlp.noble.tools.TextTools;
import edu.pitt.dbmi.nlp.noble.util.CSVExporter;
import edu.pitt.dbmi.nlp.noble.util.FileTools;
import edu.pitt.dbmi.nlp.noble.util.HTMLExporter;






/**
 * process a set of reports and generate an HTML to get.
 *
 * @author tseytlin
 */
public class NobleMentionsTool implements ActionListener{
	private final URL LOGO_ICON = getClass().getResource("/icons/NobleLogo256.png");
	private JFrame frame;
	private JTextField input,output;
	private JList<DomainOntology> templateList;
	private JTextArea console;
	private JProgressBar progress;
	private JPanel buttonPanel;
	private JButton run;
	private File file;
	private long totalTime;
	private long processCount;
	private HTMLExporter htmlExporter;
	private CSVExporter csvExporter;
	private static boolean statandlone = false;
	private DefaultRepository repository = new DefaultRepository();
	
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
			templateList = new JList(new DefaultListModel<DomainOntology>());
			JButton browse = new JButton("Browse");
			browse.addActionListener(this);
			browse.setActionCommand("i_browser");
		
			
			JButton export = new JButton("Export");
			export.setActionCommand("export");
			export.addActionListener(this);
			JButton add = new JButton("Import");
			add.setActionCommand("import");
			add.addActionListener(this);
			JButton info = new JButton("Preview");
			info.setActionCommand("preview");
			info.addActionListener(this);
			JScrollPane scroll = new JScrollPane(templateList);
			scroll.setPreferredSize(new Dimension(100,100));
			
			panel.add(new JLabel("Input Schema"),c);c.gridx++;c.gridheight=3;
			panel.add(scroll,c);c.gridx++;c.gridheight=1;
			panel.add(add,c);c.gridy++;
			panel.add(export,c);c.gridy++;
			panel.add(info,c);c.gridy++;
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
			
			progress = new JProgressBar();
			progress.setIndeterminate(true);
			progress.setString("Please Wait. It will take a while ...");
			progress.setStringPainted(true);
			
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
		}else{
			frame.setVisible(true);
		}
	}	
	
	/**
	 * Load deafaults.
	 */
	private void loadDeafaults(){
		(new Thread(new Runnable(){
			public void run(){
				setBusy(true);
				refreshTemplateList();
				//input.setText("/home/tseytlin/Data/Reports/ReportProcessorInput/");
				//output.setText("/home/tseytlin/Data/Reports/Output/ReportProcessorInput/");
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
				((DefaultListModel<DomainOntology>)templateList.getModel()).removeAllElements();
				for(IOntology t: repository.getOntologies()){
					try {
						((DefaultListModel<DomainOntology>)templateList.getModel()).addElement(new DomainOntology(t));
					} catch (IOntologyException e) {
						e.printStackTrace();
					}
				}
				templateList.validate();
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
					buttonPanel.add(progress,BorderLayout.CENTER);
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
		}else if("i_browser".equals(cmd)){
			doBrowse(input);
		}else if("d_browser".equals(cmd)){
			
		}else if("o_browser".equals(cmd)){
			doBrowse(output);
		}else if("exit".equals(cmd)){
			System.exit(0);
		}else if("export".equals(cmd)){
			doExport();
		}else if("import".equals(cmd)){
			doImport();
		}else if("preview".equals(cmd)){
			doPreview();
		}
	}
	
	/**
	 * do preview.
	 */
	private void doPreview() {
		final DomainOntology t = templateList.getSelectedValue();
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
		DomainOntology template = templateList.getSelectedValue();
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
		final JFileChooser chooser = new JFileChooser(file);
		chooser.setFileFilter(new FileFilter(){
			public boolean accept(File f) {
				return f.isDirectory() || f.getName().endsWith(".owl");
			}
			public String getDescription() {
				return "OWL file (extending Schema.owl)";
			}
			
		});
		int r = chooser.showOpenDialog(frame);
		if(r == JFileChooser.APPROVE_OPTION){
			
				new Thread(new Runnable(){
					public void run(){
						setBusy(true);
						try{
							file = chooser.getSelectedFile();
							File newLocation = new File(repository.getOntologyLocation(),file.getName());
							Files.copy(file.toPath(),newLocation.toPath(),StandardCopyOption.REPLACE_EXISTING);
							
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
						setBusy(false);
					}
				}).start();
			
		}
	}

	
	
	/**
     * Display a file in the system browser. If you want to display a file, you
     * must include the absolute path name.
     *
     * @param url
     *            the file's url (the url must start with either "http://" or
     *            "file://").
     */
     private void browseURLInSystemBrowser(String url) {
    	 Desktop desktop = Desktop.getDesktop();
    	 if( !desktop.isSupported( java.awt.Desktop.Action.BROWSE ) ) {
    		 progress("Could not open "+url+"\n");
    	 }
    	 try {
    		 java.net.URI uri = new java.net.URI( url );
    		 desktop.browse( uri );
    	 }catch ( Exception e ) {
           System.err.println( e.getMessage() );
    	 }
     }
	
	
     /**
      * check UI inputs.
      *
      * @return true, if successful
      */
    private boolean checkInputs(){
 		if(templateList.getSelectedValuesList().isEmpty()){
			JOptionPane.showMessageDialog(frame,"Please Select Templates");
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
				
				DomainOntology ontology = templateList.getSelectedValue();
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
				
				// create just-in-time instance file
				try {
					ontology = new DomainOntology(ontology.getOntology().getLocation());
				} catch (IOntologyException e1) {
					e1.printStackTrace();
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
					e.printStackTrace();
				}
				
				
				setBusy(false);
				
				// open in browser
				browseURLInSystemBrowser(new File(output.getText()+File.separator+"index.html").toURI().toString());
				
			}
		})).start();
	}
	
	
	/**
	 * Do browse.
	 *
	 * @param text the text
	 */
	private void doBrowse(JTextField text){
		//if(text == domain){
		//	
		//}else{
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
					String prefix = file.getName();
					if(prefix.endsWith(".txt"))
						prefix = prefix.substring(0,prefix.length()-4);
					prefix = prefix+File.separator+(new SimpleDateFormat("yyyy-MM-dd HH.mm.ss").format(new Date(System.currentTimeMillis())));
					output.setText(new File(file.getParent()+File.separator+"Output"+File.separator+prefix).getAbsolutePath());
				}
			}
		//}
	}

	/**
	 * process  documents.
	 * @param ontology the templates to process
	 * @param in the in
	 * @param out the out
	 */
	public void process(DomainOntology ontology,String in, String out){	
		// start a new instance of noble mentions
		NobleMentions noble = new NobleMentions(ontology);
		
		// process file
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
	 * @param reportFile the report file
	 * @throws Exception the exception
	 */
	private void process(NobleMentions noble,File reportFile) throws Exception {
		progress("processing report ("+(processCount+1)+") "+reportFile.getName()+" ... ");
		
		// read in the report, do first level proce
		Composition doc = noble.process(reportFile);
		
		// temp system.out
		/*for(AnnotationVariable var: doc.getAnnotationVariables()){
			System.out.println(var);
		}*/
		
		
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
