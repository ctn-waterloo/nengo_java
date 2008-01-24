/*
 * Created on 12-Dec-07
 */
package ca.neo.config;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.apache.log4j.Logger;


/**
 * A registry of graphical Icons that can be used for displaying Property values.
 *  
 * @author Bryan Tripp
 */
public class IconRegistry {

	private static Logger ourLogger = Logger.getLogger(IconRegistry.class);
	private static IconRegistry ourInstance;
	
	private List<Class> myIconClasses;
	private List<Icon> myIcons;
	
	/**
	 * @return Singleton instance
	 */
	public static IconRegistry getInstance() {
		if (ourInstance == null) {
			ourInstance = new IconRegistry();
			
			//TODO: move these somewhere configurable
			ourInstance.setIcon(Property.class, new Icon(){
				public void paintIcon(Component c, Graphics g, int x, int y) {
					g.drawPolygon(new int[]{8, 13, 8, 3}, new int[]{3, 8, 13, 8}, 4);
				}
				public int getIconWidth() {
					return 16;
				}
				public int getIconHeight() {
					return 16;
				}
			});
			ourInstance.setIcon(Integer.class, "/ca/neo/config/ui/integer_icon.GIF");
			ourInstance.setIcon(float[].class, "/ca/neo/config/ui/float_array_icon.GIF");
			ourInstance.setIcon(float[][].class, "/ca/neo/config/ui/matrix_icon.GIF");
			ourInstance.setIcon(String.class, "/ca/neo/config/ui/string_icon.JPG");
		}
		
		return ourInstance;
	}
	
	private IconRegistry() {
		myIconClasses = new ArrayList<Class>(10);
		myIcons = new ArrayList<Icon>(10);
	}
	
	/**
	 * @param o An object 
	 * @return An icon to use in displaying the given object
	 */
	public Icon getIcon(Object o) {
		return (o == null) ? null : getIcon(o.getClass());
	}
	
	private Icon getIcon(Class c) {
		Icon result = null;
		for (int i = 0; result == null && i < myIconClasses.size(); i++) {
			if (myIconClasses.get(i).isAssignableFrom(c)) {
				result = myIcons.get(i);
			}
		}
		
		if (result == null) {
			result = new DefaultIcon();
		}
		
		return result;
	}
	
	/**
	 * @param c A class
	 * @param icon An Icon to use for objects of the given class 
	 */
	public void setIcon(Class c, Icon icon) {
		myIconClasses.add(c);
		myIcons.add(icon);
	}
	
	/**
	 * @param c A class
	 * @param path Path to an image file from which to make an Icon for objects of the 
	 * 		given class
	 */
	public void setIcon(Class c, String path) {
		myIconClasses.add(c);
		myIcons.add(createImageIcon(path, ""));		
	}
	
	private ImageIcon createImageIcon(String path, String description) {
		ImageIcon result = null;
	    java.net.URL imgURL = getClass().getResource(path);
	    if (imgURL != null) {
	        result = new ImageIcon(imgURL, description);
	    } else {
	        ourLogger.warn("Can't load icon from " + path);
	    }
	    
	    return result;
	}
	
	private static class DefaultIcon implements Icon {

		public int getIconHeight() {
			return 16;
		}

		public int getIconWidth() {
			return 16;
		}

		public void paintIcon(Component c, Graphics g, int x, int y) {
			g.setColor(Color.LIGHT_GRAY);
			g.drawOval(1, 1, 14, 14);
		}
		
	}
	
}
