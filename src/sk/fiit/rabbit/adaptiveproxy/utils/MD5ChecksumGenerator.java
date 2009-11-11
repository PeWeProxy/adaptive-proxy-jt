package sk.fiit.rabbit.adaptiveproxy.utils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;

public final class MD5ChecksumGenerator {
	private static final Logger log = Logger.getLogger(MD5ChecksumGenerator.class.getName());
	private static final MessageDigest md5Generator;
	private static final byte[] HEX_CHAR_TABLE;    
	 
	static {
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			log.warn("Unable to obtain MD5 MessageDigest instance");
		}
		md5Generator = md;
		HEX_CHAR_TABLE = new byte[] {
			    (byte)'0', (byte)'1', (byte)'2', (byte)'3',
			    (byte)'4', (byte)'5', (byte)'6', (byte)'7',
			    (byte)'8', (byte)'9', (byte)'a', (byte)'b',
			    (byte)'c', (byte)'d', (byte)'e', (byte)'f'};    
	}
	
	private MD5ChecksumGenerator() {}
	
	private static byte[] createChecksum(File file) throws IOException {
		//log.debug("\tCalculating MD5 checksum for file "+file);
	  return computeChecksum(new FileInputStream(file));
	}
	
	private static byte[] computeChecksum(InputStream stream) throws IOException {
		byte[] buffer = new byte[1024];
		  int numRead;
		  do {
		   numRead = stream.read(buffer);
		   if (numRead > 0) {
		     md5Generator.update(buffer, 0, numRead);
		     }
		   } while (numRead != -1);
		  stream.close();
		  return md5Generator.digest();
	}
	
	private static byte[] createDirChecksum(File dir, FilenameFilter nameFilter) throws IOException {
		//log.debug("\tCalculating MD5 checksum for directory "+dir);
		if (nameFilter == null)
			throw new IllegalArgumentException("FilenameFilter can not be null");
		File[] dirFiles = dir.listFiles();
		List<byte[]> checksumBufs = new ArrayList<byte[]>(dirFiles.length);
		int bufsLength = 0;
		for (File file : dirFiles) {
			byte[] checksum = null;
			if (file.isDirectory())
				checksum = createDirChecksum(file,nameFilter);
			else if (nameFilter.accept(dir, file.getName()))
				checksum = createChecksum(file);
			if (checksum != null) {
				checksumBufs.add(checksum);
				bufsLength += checksum.length;
			}
		}
		byte[] buffer = new byte[bufsLength];
		int copied = 0;
		for (byte[] buf : checksumBufs) {
			System.arraycopy(buf, 0, buffer, copied, buf.length);
			copied += buf.length;
		}
		ByteArrayInputStream stream = new ByteArrayInputStream(buffer);
		return computeChecksum(stream);
	}
	
	private static final String getChecksumString(byte[] checksum) throws UnsupportedEncodingException {
		byte[] hex = new byte[2 * checksum.length];
		int index = 0;
	    for (byte b : checksum) {
	      int v = b & 0xFF;
	      hex[index++] = HEX_CHAR_TABLE[v >>> 4];
	      hex[index++] = HEX_CHAR_TABLE[v & 0xF];
	    }
	    return new String(hex, "ASCII");
	}
	
	public static String createHexChecksum(File file, FilenameFilter nameFilter) throws IOException {
		//log.debug("Calculating MD5 checksum for "+file);
		if (file.isDirectory())
			return getChecksumString(createDirChecksum(file,nameFilter));
		else
			return getChecksumString(createChecksum(file));
	}
}
