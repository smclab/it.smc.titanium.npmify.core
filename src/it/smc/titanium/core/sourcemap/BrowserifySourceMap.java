package it.smc.titanium.core.sourcemap;

import it.smc.titanium.core.iternal.sourcemap.SourceMapResult;
import it.smc.titanium.npmify.core.NPMifyCorePlugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.text.MessageFormat;
import java.util.Collection;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import com.aptana.core.logging.IdeLog;
import com.aptana.core.sourcemap.ISourceMap;
import com.aptana.core.sourcemap.ISourceMapResult;
import com.aptana.core.util.CollectionsUtil;
import com.aptana.core.util.IOUtil;
import com.aptana.core.util.StringUtil;
import com.google.debugging.sourcemap.SourceMapConsumerFactory;
import com.google.debugging.sourcemap.SourceMapParseException;
import com.google.debugging.sourcemap.SourceMapping;
import com.google.debugging.sourcemap.SourceMappingReversable;
import com.google.debugging.sourcemap.proto.Mapping;

public class BrowserifySourceMap implements ISourceMap {

	private static final String MAP_EXTENSION = "map";
	private SourceMapping sourceMapping;
	private IPath bundleMapLocation;
	private IPath generatedBundleLocation;

	public void setInitializationData(
			IConfigurationElement config, String propertyName, Object data)
		throws CoreException {

		String bundleMapLocation = config.getAttribute("bundleMapLocation");

		if (!StringUtil.isEmpty(bundleMapLocation)) {
			this.bundleMapLocation = validatePlatformLocation(
					bundleMapLocation, propertyName, data);
		}

		String generatedBundleLocation = config
			.getAttribute("generatedBundleLocation");

		if (!StringUtil.isEmpty(generatedBundleLocation)) {
			this.generatedBundleLocation = validatePlatformLocation(
				generatedBundleLocation, propertyName, data);
		}

		NPMifyCorePlugin.log("Initialization data for BrowserifySourceMap");
	}

	private IPath validatePlatformLocation(
			String locationPrefix, String property, Object data) {
		String platform = property;
		IProject project = null;

		if (data instanceof IProject) {
			project = (IProject) data;
		}

		if ((project != null) && (!StringUtil.isEmpty(platform))) {
			String newPrefix = MessageFormat.format(
				locationPrefix, new Object[] { platform });

			IPath newPrefixPath = Path.fromOSString(newPrefix);
			IPath projectLocation = project.getLocation();

			if (projectLocation.append(newPrefixPath).toFile().exists()) {
				return newPrefixPath;
			}
		}

		String newPrefix = MessageFormat.format(
			locationPrefix, new Object[] { "" });

		return Path.fromOSString(newPrefix);
	}

	public void setContents(String contents) throws SourceMapParseException {
		try {
			this.sourceMapping = SourceMapConsumerFactory.parse(contents);
		}
		catch (SourceMapParseException smpe) {
			IdeLog.logError(NPMifyCorePlugin.getDefault(), smpe);
			throw smpe;
		}
	}

	public void setContents(InputStream input) throws SourceMapParseException {
		setContents(IOUtil.read(input));
	}

	public void initialize(IResource resource) throws SourceMapParseException {
		if ((resource == null) || (this.bundleMapLocation == null)) {
			NPMifyCorePlugin.log("Could not initializing BrowserifySourceMap");
			this.sourceMapping = null;
			return;
		}
		else {
			NPMifyCorePlugin.log("Initializing BrowserifySourceMap");
		}

		IPath relativePath = resource.getProjectRelativePath();

		if (relativePath != null) {

			IPath mapLocation = resource.getProject().getLocation()
				.append(this.bundleMapLocation);

			FileInputStream inputStream = null;

			try {
				File file = mapLocation.toFile();

				if (file.exists()) {
					inputStream = new FileInputStream(file);
					setContents(inputStream);
					NPMifyCorePlugin.log("Bundle source map loaded");
				}
				else {
					this.sourceMapping = null;
				}
			}
			catch (IOException e) {
				IdeLog.logError(NPMifyCorePlugin.getDefault(), e);

				if (inputStream == null) {
					return;
				}

				try {
					inputStream.close();
				}
				catch (IOException localIOException1) {
					//
				}
			}
			finally {
				if (inputStream != null) {
					try {
						inputStream.close();
					}
					catch (IOException localIOException2) {
						//
					}
				}
			}

			try {
				inputStream.close();
			}
			catch (IOException localIOException3) {
				//
			}

		}
		else {
			this.sourceMapping = null;
		}
	}

	public ISourceMapResult getOriginalMapping(IResource resource,
			int lineNumber, int columnNumber) throws SourceMapParseException {
		initialize(resource);

		SourceMapResult result = (SourceMapResult) getOriginalMapping(
				lineNumber, columnNumber);

		NPMifyCorePlugin.log("getOriginalMapping: " + resource.getLocation().toString());

		if (result != null) {
			result.setFile(resource.getProject().getFullPath()
					.append(result.getFile()));
		}
		return result;
	}

	public ISourceMapResult getOriginalMapping(
			IResource resource, int lineNumber)
		throws SourceMapParseException, CoreException {

		int columnNumber = resolveColumnNumber(resource, lineNumber);
		if (columnNumber < 0) {
			return null;
		}
		return getOriginalMapping(resource, lineNumber, columnNumber);
	}

	public ISourceMapResult getOriginalMapping(int lineNumber, int columnNumber) {
		if (this.sourceMapping == null) {
			return null;
		}

		Mapping.OriginalMapping mappingForLine = this.sourceMapping
				.getMappingForLine(lineNumber, columnNumber);

		if (mappingForLine == null) {
			return null;
		}

		String originalFile = mappingForLine.getOriginalFile();

		/*if ((this.originalLocationPrefix != null)
				&& (!this.originalLocationPrefix.isPrefixOf(Path
						.fromOSString(originalFile)))) {
			return null;
		}*/

		return new SourceMapResult(
				originalFile, mappingForLine.getLineNumber(),
				mappingForLine.getColumnPosition());
	}

	public ISourceMapResult getGeneratedMapping(
			IResource resource, int lineNumber, int columnNumber)
		throws SourceMapParseException {

		initialize(resource);

		return getGeneratedMapping(
				resource.getProjectRelativePath().toOSString(), lineNumber,
				columnNumber);
	}

	public ISourceMapResult getGeneratedMapping(
			String originalFile, int lineNumber, int columnNumber) {

		NPMifyCorePlugin.log("getGeneratedMapping: " + originalFile);

		if (!(this.sourceMapping instanceof SourceMappingReversable)) {
			NPMifyCorePlugin.log("Source map is not reversable for " + originalFile);
			return null;
		}

		SourceMappingReversable reversableSourceMap =
			(SourceMappingReversable) this.sourceMapping;

		Collection<Mapping.OriginalMapping> reverseMapping =
			reversableSourceMap.getReverseMapping(
					originalFile, lineNumber, columnNumber);

		if (CollectionsUtil.isEmpty(reverseMapping)) {
			NPMifyCorePlugin.log("Empty reverseMapping list for " + originalFile);
			return null;
		}

		String generated = null;
		Mapping.OriginalMapping mapping = null;

		for (Mapping.OriginalMapping m : reverseMapping) {
			mapping = m;
			generated = mapping.getOriginalFile();

			if (!StringUtil.isEmpty(generated)) {
				break;
			}
		}

		if (StringUtil.isEmpty(generated)) {
			NPMifyCorePlugin.log("No generated file from reversed source map for " + originalFile);

			generated = this.generatedBundleLocation.toOSString();
		}

		return new SourceMapResult(
			generated, mapping.getLineNumber(),
			mapping.getColumnPosition());
	}

	public IPath getMapLocationPrefix() {
		return null;
	}

	public IPath getOriginalLocationPrefix() {
		return null;
	}

	public IPath getGeneratedLocationPrefix() {
		return null;
	}

	protected int resolveColumnNumber(IResource resource, int lineNumber)
			throws CoreException {
		if (!(resource instanceof IFile)) {
			return -1;
		}
		IFile file = (IFile) resource;
		try {
			resource.refreshLocal(0, null);
		} catch (CoreException localCoreException) {
		}
		LineNumberReader reader = null;
		try {
			reader = new LineNumberReader(new InputStreamReader(
					file.getContents(), file.getCharset()));
			String line = null;
			while ((line = reader.readLine()) != null) {
				if (reader.getLineNumber() == lineNumber) {
					for (int i = 0; i < line.length(); i++) {
						if (!Character.isWhitespace(line.charAt(i))) {
							return i + 1;
						}
					}
					return -1;
				}
			}
		} catch (IOException e) {
			IdeLog.logError(NPMifyCorePlugin.getDefault(), e);

			if (reader != null) {
				try {
					reader.close();
				} catch (IOException localIOException3) {
				}
			}
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException localIOException4) {
				}
			}
		}

		return -1;
	}

}
