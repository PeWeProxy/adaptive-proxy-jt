package sk.fiit.peweproxy;

import java.util.Queue;

import rabbit.httpio.request.ContentCachingListener;
import rabbit.proxy.Connection;

public class RequestContentCahcedListener implements ContentCachingListener {
	private final Connection con;
	
	public RequestContentCahcedListener(Connection con) {
		this.con = con;
	}

	@Override
	public void dataCached(byte[] contentData, Queue<Integer> dataIncrements) {
		con.getProxy().getAdaptiveEngine().requestContentCached(con, contentData, dataIncrements);
	}

	@Override
	public void failed(Exception e) {
		con.readFailed(e);
	}

	@Override
	public void timeout() {
		con.readTimeout();
	}
}
