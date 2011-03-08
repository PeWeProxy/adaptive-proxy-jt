package sk.fiit.peweproxy.utils;

import rabbit.http.HttpHeader;

public final class HeaderUtils {
	private HeaderUtils() {}
	
	public static void removeContentHeaders(HttpHeader header) {
		header.removeHeader("Transfer-Encoding");
		header.removeHeader("Trailer");
		header.removeHeader("Content-Encoding");
		header.removeHeader("Content-Language");
		header.removeHeader("Content-Length");
		header.removeHeader("Content-Location");
		header.removeHeader("Content-MD5");
		header.removeHeader("Content-Range");
		header.removeHeader("Content-Type");
	}
}
