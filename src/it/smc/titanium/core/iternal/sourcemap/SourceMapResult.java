package it.smc.titanium.core.iternal.sourcemap;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import com.aptana.core.sourcemap.ISourceMapResult;
import com.aptana.core.util.ObjectUtil;
import com.aptana.core.util.StringUtil;

public class SourceMapResult implements ISourceMapResult {
	private IPath filePath;
	private int lineNumber;
	private int columnPosition;

	public SourceMapResult(String filePath, int lineNumber, int columnPosition) {
		this.filePath = (StringUtil.isEmpty(filePath) ? null : Path
				.fromOSString(filePath));
		this.lineNumber = lineNumber;
		this.columnPosition = columnPosition;
	}

	public IPath getFile() {
		return this.filePath;
	}

	public void setFile(IPath path) {
		this.filePath = path;
	}

	public int getLineNumber() {
		return this.lineNumber;
	}

	public int getColumnPosition() {
		return this.columnPosition;
	}

	public int hashCode() {
		int result = 1;
		result = 31 * result + this.lineNumber;
		result = 31 * result + this.columnPosition;
		result = 31 * result
				+ (this.filePath != null ? this.filePath.hashCode() : 0);
		return result;
	}

	public boolean equals(Object obj) {
		if ((obj instanceof SourceMapResult)) {
			SourceMapResult other = (SourceMapResult) obj;
			return (this.lineNumber == other.lineNumber)
					&& (this.columnPosition == other.columnPosition)
					&& (ObjectUtil.areEqual(this.filePath, other.filePath));
		}
		return false;
	}

	public String toString() {
		StringBuilder builder = new StringBuilder("SourceMapResult [file = ");
		builder.append(this.filePath);
		builder.append(", lineNumber = ");
		builder.append(this.lineNumber);
		builder.append(", column = ");
		builder.append(this.columnPosition);
		builder.append(']');
		return builder.toString();
	}
}