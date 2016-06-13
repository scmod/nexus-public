package util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;

public class Files {

	public static File createDirectories(File dir) throws IOException {

		// attempt to create the directory
		try {
			Objects.requireNonNull(dir);
			if (dir.isDirectory()) {
				if (!dir.mkdirs())
					;

			} else {
				File parent = dir.getParentFile();
				if (!parent.mkdirs()) {
					throw new IOException();
				} else if (!dir.createNewFile()) {
					throw new FileAlreadyExistsException();
				}
			}
			return dir;
		} catch (Exception e) {
			throw new IOException(e);
		}
	}

	public static boolean isDirectory(File file) {
		return file.isDirectory();
	}

	public static BufferedReader newBufferedReader(File path, Charset cs)
			throws IOException {
		CharsetDecoder decoder = cs.newDecoder();

		Reader reader = new InputStreamReader(new FileInputStream(path),
				decoder);
		return new BufferedReader(reader);
	}

	public static BufferedWriter newBufferedWriter(File path, Charset cs) throws IOException {
		CharsetEncoder encoder = cs.newEncoder();
		Writer writer = new OutputStreamWriter(new FileOutputStream(path),
				encoder);
		return new BufferedWriter(writer);
	}

	// The "regular" part doesn't mean anything in particular, it only means
	// that it's not a pipe, device, socket or anything other "special".
	//i.e. /dev/hda /dev/null
	public static boolean isRegularFile(File path) {
		return path.isFile();
	}

	// public static InputStream newInputStream(File path, Charset cs)
	// throws IOException {
	// return newByteChannel(path, cs);
	// }
	//
	// public static FileChannel newByteChannel(File path, Charset cs)
	// throws IOException {
	// return new FileInputStream(path).getChannel();
	// }

}
