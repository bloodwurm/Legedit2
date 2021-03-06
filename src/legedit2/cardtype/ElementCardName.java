package legedit2.cardtype;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.font.LineMetrics;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.ArrayList;

import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.w3c.dom.Node;

import legedit2.card.Card;
import legedit2.deck.Deck;
import legedit2.helpers.LegeditHelper;
import legedit2.imaging.MotionBlurOp;


public class ElementCardName extends CustomElement implements Cloneable {

	public enum HIGHLIGHT {BANNER, BLUR, BANNER_BLUR, NONE}
	
	// global card name settings
	public String defaultValue;
	public boolean includeSubname;
	public String subnameText;
	public boolean subnameEditable;
	public ALIGNMENT alignment = ALIGNMENT.CENTER;
	public boolean allowChange;
	public int x;
	public int y;
	public Color colour;
	public Color subNameColour;
	public boolean drawUnderlay;
	public int blurRadius;
	public boolean blurDouble;
	public int blurExpand;
	public Color highlightColour;
	public boolean uppercase;
	public HIGHLIGHT highlight = HIGHLIGHT.NONE;
	public int subnameGap = -1;
	public int bannerExtraSizeTop = 10; // set to 10 only for backward compatibility
	public int bannerExtraSizeBottom = 15; // set to 15 only for backward compatibility
	private JTextField cardNameField;
	private JTextField cardSubNameField;
	
	// card name specific settings
	public String value;
	public String fontName;
	public int fontStyle;
	public int textSize;
	public String namePrefix = "";
	public String nameSuffix = "";
	
	// sub name specific settings
	public String subnameValue;
	public String subnameFontName;
	public int subnameFontStyle;
	public int subnameSize;
	private String subnamePrefix = "";
	private String subnameSuffix = "";	

	
	private int getScreenStartingXPositionForString(String text, FontMetrics metrics)
	{
    	int stringLength = SwingUtilities.computeStringWidth(metrics, text);
        
        if (alignment.equals(ALIGNMENT.CENTER))
        	stringLength /= 2;
        
    	return LegeditHelper.getPercentage(x, getScale()) - stringLength;
	}
	
	private LineInformation createLineInformation(String text, Graphics2D g, FontMetrics metrics, int x, int y)
	{
        int stringLength = SwingUtilities.computeStringWidth(metrics, text);		        
        
        if (alignment.equals(ALIGNMENT.CENTER))
        	stringLength /= 2;
        if (alignment.equals(ALIGNMENT.RIGHT) || alignment.equals(ALIGNMENT.CENTER))
        	x -= stringLength;

    	LineMetrics lm = metrics.getLineMetrics(text, g);
    	float height = (lm.getAscent() - lm.getDescent()) * 1.05f;
    	return new LineInformation(text, x, (int)(y + height), (int)height);
	}
	
	private List<LineInformation> prepareTextLines(String text, Graphics2D g, Font font, int xStart, int yStart)
	{
		///////////////////////////////////////////////////////////////
		/// Manually convert end lines so that calling split finds them
		if (text.length() > 1)
		{
			for (int i = 0; i < text.length() - 2; i++) 
			{
				if ((text.charAt(i) == '\\') && (text.charAt(i+1) == 'N'))
				{
					text = text.substring(0,i) + '\n' + text.substring(i+2);
				}
			}
		}
		///////////////////////////////////////////////////////////////
		
		///////////////////////////////////////////////////////////////
		/// Break the strings in regards to all the end lines manually entered by the user
        String[] linesToProcess = text.split("\n");
        if (linesToProcess.length > 1)
        {
    		List<LineInformation> linesToReturn = new ArrayList<LineInformation>();
            for (String line : linesToProcess)
            {
           		linesToReturn.addAll(prepareTextLines(line, g, font, xStart, yStart));
           		LineInformation lastLine = linesToReturn.get(linesToReturn.size()-1);
           		yStart = lastLine.drawYPosition + lastLine.lineThickness;
            }
            return linesToReturn;
        }
		///////////////////////////////////////////////////////////////

        g.setFont(font);
        FontMetrics metrics = g.getFontMetrics(font);

		List<LineInformation> lines = new ArrayList<LineInformation>();
		if (getScreenStartingXPositionForString(text, metrics) > 0)
		{
			// no need to break anything up, it fits already
			lines.add(createLineInformation(text, g, metrics, xStart, yStart));
		}
		else
		{
			// its too long, we need to break it down
			String newLine = "";
			int currentY = yStart;
	        for (String word : text.split("\\s+"))
	        {
	        	String testLine = newLine + " " + word;
	        	if (getScreenStartingXPositionForString(testLine, metrics) > 0)
	        	{
	        		// still fits, add and continue
	        		newLine = testLine;
	        	}
	        	else
	        	{
	        		// became too long, break and start new line
	        		LineInformation newLineInfo = createLineInformation(newLine, g, metrics, xStart, currentY);
	        		lines.add(newLineInfo);
	        		newLine = word;
	        		
	        		// update for next line
	        		currentY = newLineInfo.drawYPosition + newLineInfo.lineThickness;
	        	}
	        }
	        
	        if (!newLine.isEmpty())
	        {
	        	// must not forget last line
        		LineInformation newLineInfo = createLineInformation(newLine, g, metrics, xStart, currentY);
        		lines.add(newLineInfo);
	        }
		}
		
		return lines;
	}	
	
	public void drawElement(Graphics2D g)
	{
		if (getValue() != null)
		{
        	double scale = getScale();
        	int xScaled = LegeditHelper.getPercentage(x, scale);
	        int currentYScaled = LegeditHelper.getPercentage(y, scale);
	        
	        int cardWidth = template.getCardWidth();
	        int cardHeight = template.getCardHeight();
	        
	        BufferedImage bi = new BufferedImage(LegeditHelper.getPercentage(cardWidth, getScale()), LegeditHelper.getPercentage(cardHeight, getScale()), BufferedImage.TYPE_INT_ARGB);
			Graphics2D g2 = getGraphics(bi);
			g2 = setGraphicsHints(g2);
			
			if (colour != null)
				g2.setColor(colour);
        

			Font font = LegeditHelper.createFont(fontName, "Percolator", fontStyle, textSize, getScale());
	        g2.setFont(font);

	        Font fontSubname = null;
	        
		
			////////////////////////////////////////////////////////
	        // Prep up by breaking out our lines for card name and subname
	        ////////////////////////////////////////////////////////
	        
	        List<LineInformation> cardNameLines = prepareTextLines(getValueForDraw(), g2, font, xScaled, currentYScaled);
	        List<LineInformation> subNameLines = null;
	        
	        if (includeSubname)
	        {
	        	fontSubname = LegeditHelper.createFont(subnameFontName, "Percolator", subnameFontStyle, subnameSize, getScale());
		        
	        	LineInformation lastLine = cardNameLines.isEmpty() ? null : cardNameLines.get(cardNameLines.size()-1);
	        	if (lastLine != null)
	        		currentYScaled = lastLine.drawYPosition;
	        	
	        	int subnameGapScaled = LegeditHelper.getPercentage(subnameGap, getScale());
		        if (subnameGapScaled >= 0 )
		        	currentYScaled += subnameGapScaled;
		        
	        	subNameLines = prepareTextLines(getSubnameValueForDraw(), g2, fontSubname, xScaled, currentYScaled);
	        }
	        
	        ////////////////////////////////////////////////////////
	        // Draw our banners/highlights if needed
	        ////////////////////////////////////////////////////////
	        
	        if (highlight.equals(HIGHLIGHT.BLUR) || highlight.equals(HIGHLIGHT.BANNER_BLUR))
	        {
	        	// We want the underlay to be applied to the text, that means we need it to had been drawn prior but before 
	        	// the banner (else its just a big blob of blackness). So draw here first (yes text will be drawn twice but thats life)
		        g2.setFont(font);
		        g2.setColor(colour);
		        for (LineInformation line: cardNameLines)
		        {
			    	g2.drawString(line.text, line.drawXPosition, line.drawYPosition);
		        }
		        
		        if (includeSubname)
		        {
			        g2.setColor(subNameColour);
			        g2.setFont(fontSubname);
			        for (LineInformation line: subNameLines)
				    	g2.drawString(line.text, line.drawXPosition, line.drawYPosition);
		        }
		        
		        g2.setColor(colour);	// just in case
		    	drawUnderlay(bi, g2, BufferedImage.TYPE_INT_ARGB, 0, 0, LegeditHelper.getPercentage(blurRadius, getScale()), blurDouble, LegeditHelper.getPercentage(blurExpand, getScale()), highlightColour);
	        }	        

        	/*
        	g.setColor(Color.GREEN);
        	g.drawLine(0, yModified, 750, yModified);
	        
	        g.setColor(Color.RED);
	        g.drawLine(0, yModified, 750, yModified);
	        
	        g.setColor(Color.BLUE);
	        g.drawLine(0, yModified + (int)lm.getDescent(), 750, yModified + (int)lm.getDescent());
	        */	        
	        
	        if (highlight.equals(HIGHLIGHT.BANNER) || highlight.equals(HIGHLIGHT.BANNER_BLUR))
	        {	        
	        	int cardWidthScaled = LegeditHelper.getPercentage(cardWidth, getScale());
	        	int cardHeightScaled = LegeditHelper.getPercentage(cardHeight, getScale());
	        	int bannerStart = LegeditHelper.getPercentage(y, scale) - LegeditHelper.getPercentage(bannerExtraSizeTop, getScale());	
	        	int bannerEnd = 0;
	        	
	        	LineInformation lastLine = null;
		        if (subNameLines != null && !subNameLines.isEmpty())
		        	lastLine = subNameLines.get(subNameLines.size()-1);
		        else if (!cardNameLines.isEmpty())
		        	lastLine = cardNameLines.get(cardNameLines.size()-1);
	        	bannerEnd = lastLine.drawYPosition;
	        	bannerEnd += LegeditHelper.getPercentage(bannerExtraSizeBottom, getScale());
	        	
		        if (bannerEnd > bannerStart)
		        {
		        	BufferedImage bi2 = new BufferedImage(cardWidthScaled, cardHeightScaled, BufferedImage.TYPE_INT_ARGB);
			        Graphics g3 = bi2.getGraphics();
			        
		        	int bannerHeight = bannerEnd - bannerStart;
					g3.setColor(highlightColour);
					g3.fillRect(cardWidthScaled / 2, bannerStart, LegeditHelper.getPercentage(cardWidthScaled, 0.15d), bannerHeight);
			    	
					MotionBlurOp op = new MotionBlurOp();
					op.setDistance(200f);
		        	bi2 = op.filter(bi2, null);
		        	
		        	makeTransparent(bi2, 0.8d);
					
					g2.drawImage(bi2, 0, 0, null);
					
					//Flip and re-draw image
					AffineTransform tx = AffineTransform.getScaleInstance(-1, 1);
					tx.translate(-bi2.getWidth(null), 0);
					AffineTransformOp aop = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
					bi2 = aop.filter(bi2, null);
					
					g2.drawImage(bi2, 0, 0, null);	        	
		        }
	        }
	        
	        ////////////////////////////////////////////////////////
	        // Now we can draw our lines	
	        ////////////////////////////////////////////////////////
	        
	        g2.setFont(font);
	        g2.setColor(colour);
	        for (LineInformation line: cardNameLines)
		    	g2.drawString(line.text, line.drawXPosition, line.drawYPosition);
	        
	        if (includeSubname)
	        {
		        g2.setFont(fontSubname);
		        g2.setColor(subNameColour);
		        for (LineInformation line: subNameLines)
			    	g2.drawString(line.text, line.drawXPosition, line.drawYPosition);
		        g2.setFont(font); // just in case, for consistency, but not really needed since we are doing drawing
	        }
	        
	        
	        ////////////////////////////////////////////////////////
	        // Final polish	
	        ////////////////////////////////////////////////////////

	        if (rotate > 0)
			{
				double rotationRequired = Math.toRadians (rotate);
				double locationX = bi.getWidth() / 2;
				double locationY = bi.getHeight() / 2;
				AffineTransform tx = AffineTransform.getRotateInstance(rotationRequired, locationX, locationY);
				AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_BILINEAR);
				bi = op.filter(bi, null);
			}
	    	
	    	g.drawImage(bi, 0, 0, null);
	    	g2.dispose();
		}
	}
	
	public String getValue()
	{
		if (value != null)
		{
			return resolveAttributes(value, this);
		}
		return resolveAttributes(defaultValue, this);
	}
	
	public String getValueRaw()
	{
		if (value != null) {
			return value;
		}		
		return defaultValue;
	}

	private String getValueForDraw()
	{
		String valueForDraw = getNamePrefix() + getValue() + getNameSuffix();
		if (uppercase && valueForDraw != null)
		{
			return valueForDraw.toUpperCase();
		}
		return valueForDraw;
	}
	
	public String getSubnameValue()
	{
		if (subnameValue != null){
			return resolveAttributes(subnameValue, this);
		}
		if (subnameText != null) {
			return resolveAttributes(subnameText, this);
		}
		return "";
	}
	
	public String getSubnameValueRaw()
	{
		if (subnameValue != null) {
			return subnameValue;
		}
		if (subnameText != null) {
			return subnameText;
		}
		return "";
	}

	private String getSubnameValueForDraw()
	{
		String valueForDraw = getSubnamePrefix() + getSubnameValue() + getSubnameSuffix();
		if (uppercase && valueForDraw != null)
		{
			return valueForDraw.toUpperCase();
		}
		return valueForDraw;
	}
	
	private BufferedImage makeTransparent(BufferedImage bi, double percent)
	{
		int width = bi.getWidth();
		int height = bi.getHeight();
		
		for (int xx = 0; xx < width; xx++) {
            for (int yy = 0; yy < height; yy++) {
                Color originalColor = new Color(bi.getRGB(xx, yy), true);
                if (originalColor.getAlpha() > 0) {
                    int col = (LegeditHelper.getPercentage(originalColor.getAlpha(), percent) << 24) | (originalColor.getRed() << 16) | (originalColor.getGreen() << 8) | originalColor.getBlue();
                    bi.setRGB(xx, yy, col);
                }
            }
        }
		
		return bi;
	}
	
	public String generateOutputString()
	{
		return generateOutputString(false);
	}
	
	public String generateOutputString(boolean fullExport)
	{
		String str = "";
		if (value != null)
		{
			str += "CUSTOMVALUE;" + name + ";value;" + value + "\n";
		}
		if (subnameValue != null)
		{
			str += "CUSTOMVALUE;" + name + ";subnameValue;" + subnameValue + "\n";
		}
		str += "CUSTOMVALUE;" + name + ";visible;" + visible + "\n";
		return str;
	}
	
	public String getNamePrefix() {
		return namePrefix;
	}

	public void setNamePrefix(String namePrefix) {
		this.namePrefix = namePrefix;
	}	

	public String getNameSuffix() {
		return nameSuffix;
	}

	public void setNameSuffix(String nameSuffix) {
		this.nameSuffix = nameSuffix;
	}	

	public String getSubnamePrefix() {
		return subnamePrefix;
	}

	public void setSubnamePrefix(String subnamePrefix) {
		this.subnamePrefix = subnamePrefix;
	}

	public String getSubnameSuffix() {
		return subnameSuffix;
	}

	public void setSubnameSuffix(String subnameSuffix) {
		this.subnameSuffix = subnameSuffix;
	}

	public JTextField getCardNameField() {
		return cardNameField;
	}

	public void setCardNameField(JTextField cardNameField) {
		this.cardNameField = cardNameField;
	}

	public JTextField getCardSubNameField() {
		return cardSubNameField;
	}

	public void setCardSubNameField(JTextField cardSubNameField) {
		this.cardSubNameField = cardSubNameField;
	}
	
	@Override
	public void updateCardValues()
	{
		if (cardNameField != null)
		{
			value = cardNameField.getText();
		}
		
		if (cardSubNameField != null && subnameEditable)
		{
			subnameValue = cardSubNameField.getText();
		}
	}
	
	public String getDifferenceXML()
	{
		String str = "";
		
		/////////////////////////////////////////////////////////////////
		// for global attributes to really work (ie, if the value changes, then it changes for all using cards),
		// we need to keep the original "attribute" like text around so it can continue to be interpreted when
		// needed. So here we check the formatted text with the raw version, if they are not identical, then
		// it means that something got interpreted by a Global attribute, hence we keep its raw value to 
		// keep the attribute around.
		/////////////////////////////////////////////////////////////////
		
		String cardNameValue = "";
		if (getValue().equalsIgnoreCase(getValueRaw()))
			cardNameValue = getValue();
		else
			cardNameValue = getValueRaw();

		String subnameValue = "";
		if (getSubnameValue().equalsIgnoreCase(getSubnameValueRaw()))
			subnameValue = getSubnameValue();
		else
			subnameValue = getSubnameValueRaw();

		str += "<cardname name=\"" + replaceNonXMLCharacters(name) + "\" "
				+ "value=\""+replaceNonXMLCharacters(cardNameValue)+"\" "
				+ (fontName == null ? "" : "fontname=\""+replaceNonXMLCharacters(fontName)+"\" ")
				+ (fontName == null ? "" : "fontstyle=\""+fontStyle+"\" ")
				+ "textsize=\""+textSize+"\" "		
				+ "subnameValue=\"" + replaceNonXMLCharacters(subnameValue) + "\" "
				+ (subnameFontName == null ? "" : "subnamefontname=\""+replaceNonXMLCharacters(subnameFontName)+"\" ")
				+ (subnameFontName == null ? "" : "subnamefontstyle=\""+subnameFontStyle+"\" ")
				+ "subnamesize=\""+subnameSize+"\" />\n";
		
		return str;
	}
	
	public void loadValues(Node node, Card card)
	{
		if (!node.getNodeName().equals("cardname"))
		{
			return;
		}
		
		// card name specific settings
		
		if (node.getAttributes().getNamedItem("value") != null)
		{
			value = node.getAttributes().getNamedItem("value").getNodeValue();
		}
		
		if (node.getAttributes().getNamedItem("fontname") != null)
		{
			fontName = node.getAttributes().getNamedItem("fontname").getNodeValue();
		}
		
		if (node.getAttributes().getNamedItem("fontstyle") != null)
		{
			fontStyle = Integer.parseInt(node.getAttributes().getNamedItem("fontstyle").getNodeValue());
		}

		if (node.getAttributes().getNamedItem("textsize") != null)
		{
			textSize = Integer.parseInt(node.getAttributes().getNamedItem("textsize").getNodeValue());
		}
		
		
		// sub name specific settings

		if (node.getAttributes().getNamedItem("subnameValue") != null)
		{
			subnameValue = node.getAttributes().getNamedItem("subnameValue").getNodeValue();
		}

		if (node.getAttributes().getNamedItem("subnamefontname") != null)
		{
			subnameFontName = node.getAttributes().getNamedItem("subnamefontname").getNodeValue();
		}
		
		if (node.getAttributes().getNamedItem("subnamefontstyle") != null)
		{
			subnameFontStyle = Integer.parseInt(node.getAttributes().getNamedItem("subnamefontstyle").getNodeValue());
		}

		if (node.getAttributes().getNamedItem("subnamesize") != null)
		{
			subnameSize = Integer.parseInt(node.getAttributes().getNamedItem("subnamesize").getNodeValue());
		}
	}
}
