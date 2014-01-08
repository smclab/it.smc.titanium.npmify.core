package it.smc.titanium.npmify.core;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class NPMifyCorePlugin extends Plugin implements BundleActivator {

	public static final String PLUGIN_ID = "it.smc.titanium.npmify.core";

	private static NPMifyCorePlugin plugin;

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	public static void log(String message) {
		IStatus status = new Status(Status.ERROR, PLUGIN_ID, message);
		getDefault().getLog().log(status);
	}

	public static NPMifyCorePlugin getDefault() {
		return plugin;
	}

}
