package nux.xom.sandbox;

import nu.xom.Document;
import nu.xom.Element;
import nux.xom.xquery.XQueryUtil;

/**
 * Tests Saxon NamePool scalability.
 */
class NamePoolMemoryTest {

	public static void main(String[] args) throws Exception {		
		// if the next two lines are uncommented, the test will fail fairly soon with a NamePoolLimitException
		System.setProperty("nux.xom.pool.PoolConfig.maxLifeTime", "10000");
		System.setProperty("nux.xom.xquery.XQuery.shareNamePools", "false");
		System.setProperty("nux.xom.pool.Pool.debug", "true"); // watch evictions

		// int runs = Integer.parseInt(args[0]);
		// String query = args[1];
		int runs = 1000;
		String query = "element myelem { . }"; // tree copy leads to namepool allocations
		
		int k = 0;
		for (int i=1; i < runs; i++) {
			Element root = new Element("root");
			int j = k + 10000;
			while (k < j) {
				root.appendChild(new Element("elem" + (k++)));
			}
			
			XQueryUtil.xquery(new Document(root), query);
			
			if (i % 1 == 0) System.out.print(".");
			if (i % 10 == 0) System.out.println("i=" + i);
		}
	}
}

