package org.distropia.server;

import java.io.File;

public class FileToDelete {
	protected long deleteAt;
	protected File file;
	public FileToDelete(long deleteAt, File file) {
		super();
		this.deleteAt = deleteAt;
		this.file = file;
	}
	public long getDeleteAt() {
		return deleteAt;
	}
	public void setDeleteAt(long deleteAt) {
		this.deleteAt = deleteAt;
	}
	public File getFile() {
		return file;
	}
	public void setFile(File file) {
		this.file = file;
	}
		
}
