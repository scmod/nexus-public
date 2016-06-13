package util;

public class Objects {

	public static <T> T requireNonNull(T obj) {
		if (obj == null)
			throw new NullPointerException();
		return obj;
	}

	public static <T> T requireNonNull(T paramT, String paramString) {
		if (paramT == null) {
			throw new NullPointerException(paramString);
		}
		return paramT;
	}

	public static boolean equals(Object a, Object b) {
		return a == b || (a != null && a.equals(b));
	}

}
