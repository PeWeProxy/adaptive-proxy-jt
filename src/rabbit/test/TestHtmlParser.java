package rabbit.test;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import rabbit.html.HtmlBlock;
import rabbit.html.HtmlParseException;
import rabbit.html.HtmlParser;
import rabbit.html.Token;
import rabbit.html.TokenType;

/** This class tests the html parser
 *
 * @author <a href="mailto:robo@khelekore.org">Robert Olofsson</a>
 */
public class TestHtmlParser {
    private String file;
    
    public static void main (String[] args) {
	for (String s : args) {
	    try {
		TestHtmlParser thp = new TestHtmlParser (s);
		thp.parse ();
	    } catch (Exception e) {
		e.printStackTrace ();
	    }
	}

	char c = 195; // Å
	char[] ca = {c};
	String s = new String (ca);
	try {
	    Charset cs = Charset.forName ("ISO-8859-1");
	    byte[] b1 = s.getBytes (cs);
	    HtmlParser parser = new HtmlParser (cs);
	    parser.setText (b1);
	    HtmlBlock block = parser.parse ();
	    updateTokens (block);
	    block.getBlocks ();
	    
	    cs = Charset.forName ("UTF-8");	    
	    byte[] b2 = s.getBytes (cs);
	    parser = new HtmlParser (cs);
	    parser.setText (b2);
	    block = parser.parse ();
	    updateTokens (block); 
	    block.getBlocks ();

	    cs = Charset.forName ("UTF-8");
	    byte[] b3 = s.getBytes (cs);
	    parser = new HtmlParser (cs);
	    parser.setText (b3, 0, 1);
	    block = parser.parse ();
	    updateTokens (block);
	    block.getBlocks ();
	    byte[] rest = block.getRestBlock ();
	    if (rest == null || rest.length != 1)
		throw new IllegalStateException ("failed to set rest buffer");
	} catch (HtmlParseException e) {
	    e.printStackTrace ();
	}	
    }
    
    private static void updateTokens (HtmlBlock block) {
	for (Token t : block.getTokens ()) {
	    t.setChanged (true);
	}
    }

    public TestHtmlParser (String file) {
	this.file = file;
    }

    private void parse () throws IOException, HtmlParseException {
	File f = new File (file);
	long size = f.length ();
	DataInputStream dis = null;
	try {
	    FileInputStream fis = new FileInputStream (f);
	    dis = new DataInputStream (fis);
	    byte[] buf = new byte[(int)size];
	    dis.readFully (buf);
	    Charset cs = Charset.defaultCharset ();
	    HtmlParser parser = new HtmlParser (cs);
	    parser.setText (buf);
	    HtmlBlock block = parser.parse ();
	    for (Token t : block.getTokens ()) {
		System.out.print ("t.type: " + t.getType ());
		if (t.getType () == TokenType.TAG)
		    System.out.print (", tag: " + t.getTag ().getType ());
		System.out.println ();
	    }
	    if (block.hasRests ()) {
		byte[] rests = block.getRestBlock ();
		System.out.println ("block has rest of " + rests.length + " bytes");
	    }
	} finally {
	    if (dis != null) {
		try {
		    dis.close ();
		} catch (IOException e) {
		    e.printStackTrace ();
		}
	    }
	}
    }
}
