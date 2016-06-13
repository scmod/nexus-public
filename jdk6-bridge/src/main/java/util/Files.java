package util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
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
					throw new IOException();
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

	public static InputStream newInputStream(File path) throws IOException {
		return new FileInputStream(path);
	}

	public static BufferedWriter newBufferedWriter(File path, Charset cs)
			throws IOException {
		CharsetEncoder encoder = cs.newEncoder();
		Writer writer = new OutputStreamWriter(new FileOutputStream(path),
				encoder);
		return new BufferedWriter(writer);
	}

	public static long copy(File source, File out) throws IOException {
		Objects.requireNonNull(out);
		InputStream in = newInputStream(source);
		try {
			if (!out.exists()) {
				File parent = out.getParentFile();
				parent.mkdirs();
				out.createNewFile();
			}
			return copy(new FileInputStream(source), new FileOutputStream(out));

		} finally {
			try {
				if (in != null)
					in.close();
			} catch (IOException e) {
			}
		}
	}

	private static final int BUFFER_SIZE = 8192;

	private static long copy(InputStream source, OutputStream sink)
			throws IOException {
		long nread = 0L;
		byte[] buf = new byte[BUFFER_SIZE];
		int n;
		while ((n = source.read(buf)) > 0) {
			sink.write(buf, 0, n);
			nread += n;
		}
		return nread;
	}

	// The "regular" part doesn't mean anything in particular, it only means
	// that it's not a pipe, device, socket or anything other "special".
	// i.e. /dev/hda /dev/null
	public static boolean isRegularFile(File path) {
		return path.isFile();
	}

	public static boolean exists(File path) {
		return path.exists();
	}
	
	public static void delete(File path) throws IOException {
        path.delete();
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
