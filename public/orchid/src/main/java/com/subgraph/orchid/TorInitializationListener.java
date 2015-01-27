package com.subgraph.orchid;

public interface TorInitializationListener {
	void initializationProgress(final String message, final int percent);
	void initializationCompleted();
}
