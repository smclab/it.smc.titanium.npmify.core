package it.smc.titanium.npmify.core;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;

public class NPMifyProjectNature implements IProjectNature {

	public static final String ID = "it.smc.titanium.npmify.core.nature";

	private IProject project;

	@Override
	public void configure() throws CoreException {
		// Let's do nothing
	}

	@Override
	public void deconfigure() throws CoreException {
		// Let's undo nothing
	}

	@Override
	public IProject getProject() {
		return this.project;
	}

	@Override
	public void setProject(IProject project) {
		this.project = project;
	}

}
