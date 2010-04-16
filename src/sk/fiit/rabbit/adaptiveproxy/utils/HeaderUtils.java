package sk.fiit.rabbit.adaptiveproxy.utils;

import rabbit.http.HttpHeader;

public final class HeaderUtils {
	private HeaderUtils() {}
	
	public static void removeContentHeaders(HttpHeader headers) {
		headers.removeHeader("Transfer-Encoding");
		headers.removeHeader("Trailer");
		headers.removeHeader("Content-Encoding");
		headers.removeHeader("Content-Language");
		headers.removeHeader("Content-Length");
		headers.removeHeader("Content-Location");
		headers.removeHeader("Content-MD5");
		headers.removeHeader("Content-Range");
		headers.removeHeader("Content-Type");
	}
}
