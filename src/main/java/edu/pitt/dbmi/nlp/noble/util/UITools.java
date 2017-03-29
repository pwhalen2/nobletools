package edu.pitt.dbmi.nlp.noble.util;

import java.awt.Component;
import java.awt.Desktop;
import java.util.Properties;

import javax.swing.JOptionPane;

public class UITools {
	/**
     * Display a lastFile in the system browser. If you want to display a lastFile, you
     * must include the absolute path name.
     *
     * @param url
     *            the lastFile's url (the url must start with either "http://" or
     *            "lastFile://").
     */
     public static void browseURLInSystemBrowser(String url) throws Exception{
    	 Desktop desktop = Desktop.getDesktop();
    	 if( !desktop.isSupported( java.awt.Desktop.Action.BROWSE ) ) {
    		 throw new Exception("Could not open "+url+" as the system browser is not supported");
    	 }
		 java.net.URI uri = new java.net.URI( url );
		 desktop.browse( uri );
     }
     /**
      * show error dialog
      * @param owner - owner panel
      * @param ex -exception
      */
     public static void showErrorDialog(Component owner,Exception ex){
    	showErrorDialog(owner,"",ex);
     }
     /**
      * show error dialog
      * @param owner - owner panel
      * @param error - message
      */
     public static void showErrorDialog(Component owner,String error){
    	showErrorDialog(owner,error,null);
     }
     
     /**
      * show error dialog
      * @param owner - owner panel
      * @param error - error message
      * @param ex - exception
      */
     public static void showErrorDialog(Component owner,String error, Exception ex){
    	 String msg = error;
    	 if(ex != null){
        	 ex.printStackTrace();
        	 msg = error+" "+ex.getMessage();
    	 }
    	 JOptionPane.showMessageDialog(owner,msg,"Error",JOptionPane.ERROR_MESSAGE);

     }
     
     /**
      * save UI settings map for a given class
      * @param p - property map of settings
      * @param cls - class that this belongs to
      */
	public static void saveSettings(Properties p, Class cls) {
		
		
	}
	
	/**
	 * load UI settings map for a given class
	 * @param cls - class that has the mapping
	 * @return - properties
	 */
	
	public static Properties loadSettings(Class cls) {
		
		return null;
	}
}
