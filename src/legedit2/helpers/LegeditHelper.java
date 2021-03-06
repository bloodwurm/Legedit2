package legedit2.helpers;

import java.awt.Font;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import legedit2.card.Card;
import legedit2.cardtype.CustomElement;
import legedit2.deck.Deck;
import legedit2.gui.LegeditFrame;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;



public class LegeditHelper {
	
	static public enum PROPERTIES {lastExpansion};
	
	final static public String FRAME_NAME = "Legedit 2";
	
	final static private String[] errorMessages = new String[] {"The phoenix must burn to emerge.", "Giving up is the only sure way to fail.", 
			"It's failure that gives you the proper perspective on success.", "There is no failure except in no longer trying.",
			"I have not failed. I've just found 10,000 ways that won't work.", "Pain is temporary. Quitting lasts forever.",
			"We are all failures - at least the best of us are.", "I'd rather be partly great than entirely useless."};
	
	final static private Random rand = new Random(new Date().getTime());
	
	private static Properties applicationProps = new Properties();
	
	private static class FontFamilyOverride
	{
		int fontStyle;
		String fontFile;
	}
	
	private static class FontFamily
	{
		String family;
		String defaultFontFile;
		public List<FontFamilyOverride> fontOverrides = new ArrayList<FontFamilyOverride>();
		
		public FontFamily(String family) {
			this.family = family;
		}
		
		public FontFamilyOverride getOverride(int fontStyle) {
			for (FontFamilyOverride fontOverride : fontOverrides) {
				if (fontOverride.fontStyle == fontStyle)
					return fontOverride;
			}
			
			return null;
		}
	}

	private static List<FontFamily> familyFonts = new ArrayList<FontFamily>();

	public static LegeditFrame getLegeditFrame()
	{
		return LegeditFrame.legedit;
	}
	
	public static void resetGUI()
	{
		LegeditFrame.refreshGUI();
	}
	
	public static String getFontPath(String fontName)
	{
		return "." + File.separator + "fonts" + File.separator + fontName;
	}
	
	public static String getFrameName()
	{
		if ((String)applicationProps.get("lastExpansion") != null)
		{
			File file = new File((String)applicationProps.get("lastExpansion"));
			if (file.exists())
			{
				return FRAME_NAME + " - " + file.getName();
			}			
		}
		
		return FRAME_NAME + " - [Untitled]";
	}
	
	public static boolean doChangesExist()
	{
		for (Card c : ProjectHelper.getCards())
		{
			if (c.isChanged())
			{
				return true;
			}
		}
		
		for (Deck d : ProjectHelper.getDecks())
		{
			if (d.isChanged())
			{
				return true;
			}
			
			for (Card c : d.getCards())
			{
				if (c.isChanged())
				{
					return true;
				}
			}
		}
		
		return false;
	}
	
	public static void handleWindowCloseSave()
	{
		if (doChangesExist())
    	{
    		int outcome = JOptionPane.showOptionDialog(getLegeditFrame(), "Save Changes?", "Save Changes", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null);
			
			if (outcome == JOptionPane.YES_OPTION)
			{
				String saveFile = LegeditHelper.getProperty(legedit2.helpers.LegeditHelper.PROPERTIES.lastExpansion);
				if (saveFile != null && new File(saveFile).exists())
				{
					ProjectHelper.saveProject(new File(saveFile));
				}
				else
				{
					JFileChooser chooser = new JFileChooser(LegeditHelper.getLastOpenDirectory());
					int outcome2 = chooser.showSaveDialog(LegeditFrame.legedit);
					if (outcome2 == JFileChooser.APPROVE_OPTION)
					{
						ProjectHelper.saveProject(chooser.getSelectedFile());
						LegeditHelper.setLastOpenDirectory(chooser.getSelectedFile().getAbsolutePath());
						LegeditHelper.setProperty(legedit2.helpers.LegeditHelper.PROPERTIES.lastExpansion, chooser.getSelectedFile().getAbsolutePath());
					}
				}
			}
    	}
	}
	
	public static String getErrorMessage()
	{
		return errorMessages[rand.nextInt(errorMessages.length)];
	}
	
	public static void loadProperties()
	{
		try {
			FileInputStream in = new FileInputStream("appProperties");
			getApplicationProps().load(in);
			in.close();
		}
		catch (FileNotFoundException e)
		{
			saveProperties();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void saveProperties()
	{
		try {
			FileOutputStream out = new FileOutputStream("appProperties");
			getApplicationProps().store(out, "---No Comment---");
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static Properties getApplicationProps() {
		return applicationProps;
	}

	public static void setApplicationProps(Properties applicationProps) {
		LegeditHelper.applicationProps = applicationProps;
	}
	
	public static File getLastOpenDirectory()
	{
		if (applicationProps.get("lastOpenDirectory") != null)
		{
			File file = new File((String)applicationProps.get("lastOpenDirectory"));
			if (file.exists())
			{
				return file;
			}
		}
		
		return null;
	}
	
	public static void setLastOpenDirectory(String value)
	{
		putProperty("lastOpenDirectory", value);
	}
	
	public static void putProperty(String property, Object value)
	{
		if (value == null)
		{
			applicationProps.remove(property);
		}
		else
		{
			applicationProps.put(property, value);
		}
		saveProperties();
	}
	
	public static Object getProperty(String property)
	{
		return applicationProps.get(property);
	}

	public static String getProperty(PROPERTIES property)
	{
		if (applicationProps.get(property.name()) != null)
		{
			return (String)applicationProps.get(property.name());
		}
		
		return null;
	}
	
	public static void setProperty(PROPERTIES property, String value)
	{
		putProperty(property.name(), value);
	}
	
	public static NodeList getXMLNodes(File inputFile)
	{
		NodeList nodes = null;
		
		try
		{
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(inputFile);
			
			doc.getDocumentElement().normalize();
	
			if (doc.hasChildNodes() && doc.getChildNodes().item(0).hasChildNodes()) 
			{
				nodes = doc.getChildNodes().item(0).getChildNodes();
			}
		}
		catch (Exception e)
		{
			JOptionPane.showMessageDialog(LegeditFrame.legedit, e.getMessage() != null ? e.getMessage() : LegeditHelper.getErrorMessage(), LegeditHelper.getErrorMessage(), JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}

		return nodes;
	}
	
	public static int getPercentage(int size, double scale)
	{
		return (int)(((double)size * (double)scale));
	}	
	
	private static FontFamily GetFontFamily(String familyName)
	{
		for (FontFamily family : familyFonts) {
			if (family.family.equalsIgnoreCase(familyName))
				return family;
		}

		return null;
	}
	
	public static void AddFontFamily(String filename, Font newFont)
	{
		// TODO Should consider storing the font itself and then simply calling deriveFont on it, would technically
		// save on reloading the font from disk everytime

		String familyName = newFont.getFamily();
		FontFamily fontFamily = GetFontFamily(familyName);
		if (fontFamily == null)
		{
			fontFamily = new FontFamily(familyName);
			fontFamily.defaultFontFile = filename;
			familyFonts.add(fontFamily);
		}

		filename = filename.toUpperCase();
		if (filename.contains("PLAIN") || filename.contains("BOLD") || filename.contains("ITALIC"))
		{
			FontFamilyOverride newOverride = new FontFamilyOverride();
			newOverride.fontFile = filename;

			if (filename.contains("PLAIN"))
				newOverride.fontStyle = 0;
			else if (filename.contains("BOLD"))
				newOverride.fontStyle = Font.BOLD;
			else if (filename.contains("ITAlIC"))
				newOverride.fontStyle = Font.ITALIC;
			
			fontFamily.fontOverrides.add(newOverride);
		}
	}
	
	public static String GetFontFilename(String fontName, int fontStyle)
	{
		FontFamily fontFamily = GetFontFamily(fontName);
		if (fontFamily != null)
		{
			FontFamilyOverride fontOverride = fontFamily.getOverride(fontStyle);
			if (fontOverride != null)
				return fontOverride.fontFile;
			
			return fontFamily.defaultFontFile;
		}
		
		return fontName;
	}
	
	public static Font createFont(String fontName, String fallbackFontName, int fontStyle, int textSize, double scale)
	{
		// TODO Should consider storing the font itself and then simply calling deriveFont on it, would technically
		// save on reloading the font from disk everytime

		if (fallbackFontName == null)
		{
			fallbackFontName = "Percolator";
		}		
	
		String fontNameToUse = fontName;
		if (fontName == null || fontName.isEmpty())
			fontNameToUse = fallbackFontName;

		try
		{
			FontFamily fontFamily = GetFontFamily(fontNameToUse);
			if (fontFamily == null)
			{
				// since we didn't find any family for it, it means its a system provided font
				// and since its a system provided font, we don't need to create it
				return new Font(fontNameToUse, fontStyle, getPercentage(textSize, scale));
			}
			
			// font provided and loaded by Legedit
			// we need to create it and then derive it to have the proper style and size
			Font newFont = Font.createFont(Font.TRUETYPE_FONT, new File(GetFontFilename(fontNameToUse, fontStyle)));
    		newFont = newFont.deriveFont(fontStyle, (float)getPercentage(textSize, scale));
    		return newFont;
		}
		catch (Exception e2)
		{
    		e2.printStackTrace();
    		
    		return new Font("Percolator", Font.PLAIN, getPercentage(textSize, scale));
		}	
	}	
}
