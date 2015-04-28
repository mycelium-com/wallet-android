package com.ledger.tbase.comm;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import android.content.Context;

import com.btchip.comm.BTChipTransport;
import com.btchip.comm.BTChipTransportFactory;

public class LedgerTransportTEEProxyFactory implements BTChipTransportFactory {

	private ExecutorService executor;
	private Context context;
	private LedgerTransportTEEProxy transport;

	public LedgerTransportTEEProxyFactory(Context context, ExecutorService executor) {
		this.context = context;
		this.executor = executor;
	}
	
	public LedgerTransportTEEProxyFactory(Context context) {
		this(context, new ScheduledThreadPoolExecutor(1));
	}
	
	@Override
	public BTChipTransport getTransport() {
		if (transport == null) {
			transport = new LedgerTransportTEEProxy(context, executor);
		}
		return transport;
	}

	@Override
	public boolean isPluggedIn() {
		return true;
	}

	@Override
	public boolean connect(Context context) {
		return true;
	}

}
