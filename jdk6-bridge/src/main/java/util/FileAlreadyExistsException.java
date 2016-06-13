package util;

public class FileAlreadyExistsException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public FileAlreadyExistsException() {
	}

	public FileAlreadyExistsException(String msg) {
		super(msg);
	}
}
