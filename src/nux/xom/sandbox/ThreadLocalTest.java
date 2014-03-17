package nux.xom.sandbox;

import java.lang.ref.SoftReference;
import java.util.Map;
import java.util.WeakHashMap;

public class ThreadLocalTest extends Thread {

	public static void main(String[] args) {
		ThreadLocalTest t = new ThreadLocalTest();
		t.start();
	}

	private static int counter = 0;

	public void run() {
		System.out.println("Starting");
		try {
			while (true) {
				makeNewThreadLocal();
			}
		} catch (OutOfMemoryError oome) {

			System.out.print("Managed to allocate and release: '" + counter);
			System.out.println("' ThreadLocals before running out of memory");

			throw oome;
		}
	}

	private void makeNewThreadLocal() {
		ThreadLocal t = new ThreadLocal();
//		FixedThreadLocal t = new FixedThreadLocal();
//		ThreadLocal t = new InitialThreadLocal();
//		ThreadLocal t = new SoftThreadLocal() {
//			protected Object initialSoftValue() { // lazy init
//				return new byte[1024 * 1024];
//			}
//		};
		t.set(new byte[1024 * 1024]);
		t.get();
		++counter;
	}

	private static final class InitialThreadLocal extends ThreadLocal {

		protected Object initialValue() {
			return new byte[1024 * 1024];
		}
	}	
	
	/**
	 * version of ThreadLocal that does not leak memory (ThreadLocal is fixed in
	 * JDK 1.5)
	 */
	private static final class FixedThreadLocal {

		private final Map values = new WeakHashMap();

		protected Object initialValue() {
			return null;
		}

		public final synchronized Object get() {
			final Object key = Thread.currentThread();
			Object value;
			if (!values.containsKey(key)) {
				value = initialValue();
				values.put(key, value);
			} else {
				value = values.get(key);
			}
			return value;
		}

		public final synchronized void set(Object value) {
			values.put(Thread.currentThread(), value);
		}
	}

	private static abstract class SoftThreadLocal extends ThreadLocal {

		/** Override this method instead of initialValue() */
		protected abstract Object initialSoftValue();
		
		protected final Object initialValue() { // lazy init
			return wrap(initialSoftValue());
		}

		public Object get() {
			Object value = ((SoftReference) super.get()).get(); // unwrap
			if (value == null) { // reinitialize if it's been silently garbage collected
				value = initialSoftValue();
				set(value);
			}
			return value;
		}
		    
		public void set(Object value) {
			super.set(wrap(value));
		}
		
		private static SoftReference wrap(Object value) {
			return new SoftReference(value);
		}
	}

}