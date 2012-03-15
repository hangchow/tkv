/**
 * 
 */
package tkv.perf;

import java.io.File;
import java.io.IOException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import tkv.LocalImpl;

/**
 * @author sean.wang
 * @since Feb 20, 2012
 */
public class LocalImplPerfTest {

	private LocalImpl fkv;

	private File dbFile;

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		dbFile = new File("/tmp/fkvtest.db");
		dbFile.delete();
		fkv = new LocalImpl(dbFile);
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		fkv.close();
		fkv.delete();
	}

	private int perfTimes = 100000;

	/**
	 * Test method for {@link tkv.LocalImpl#get(java.lang.String)}.
	 * 
	 * @throws IOException
	 */
	@Test
	@Ignore
	public void testPutDiffKeyWithoutTagsPerf() throws IOException {
		String value = "0123456789";
		long start = System.currentTimeMillis();
		for (int i = 0; i < perfTimes; i++) {
			fkv.put("" + (10000000 + i), value.getBytes());
		}
		System.out.println("testPutDiffKeyWithoutTagsPerf:" + (System.currentTimeMillis() - start));
	}

	/**
	 * Test method for {@link tkv.LocalImpl#get(java.lang.String)}.
	 * 
	 * @throws IOException
	 */
	@Test
	@Ignore
	public void testPutDiffKeyWithTagsPerf() throws IOException {
		String value = "0123456789";
		long start = System.currentTimeMillis();
		for (int i = 0; i < perfTimes; i++) {
			fkv.put("" + (10000000 + i), value.getBytes(), "pet", "dog");
		}
		System.out.println("testPutDiffKeyWithTagsPerf:" + (System.currentTimeMillis() - start));
	}

	@Test
	@Ignore
	public void testGetDiffKeyWithoutTagsPerf() throws IOException {
		String value = "0123456789";
		for (int i = 0; i < perfTimes; i++) {
			fkv.put("" + (10000000 + i), value.getBytes());
		}
		long start = System.currentTimeMillis();
		for (int i = 0; i < perfTimes; i++) {
			fkv.get("" + (10000000 + i));
		}
		System.out.println("testGetDiffKeyWithoutTagsPerf:" + (System.currentTimeMillis() - start));
	}

	@Test
	@Ignore
	public void testGetDiffKeyWithTagsPerf() throws IOException {
		String value = "0123456789";
		for (int i = 0; i < perfTimes; i++) {
			fkv.put("" + (10000000 + i), value.getBytes(), "pet", "dog");
		}
		long start = System.currentTimeMillis();
		for (int i = 0; i < perfTimes; i++) {
			fkv.get("" + (10000000 + i), "pet");
		}
		System.out.println("testGetDiffKeyWithTagsPerf:" + (System.currentTimeMillis() - start));
	}

	@Test
	@Ignore
	public void testGetDiffKeyWithMoreTagsPerf() throws IOException {
		String value = "0123456789";
		for (int i = 0; i < perfTimes; i++) {
			fkv.put("" + (10000000 + i), value.getBytes(), "pet" + i % 100, "dog" + i % 100);
		}
		long start = System.currentTimeMillis();
		for (int i = 0; i < perfTimes; i++) {
			fkv.getRecord("" + (10000000 + i), "pet" + i % 100);
		}
		System.out.println("testGetDiffKeyWithMoreTagsPerf:" + (System.currentTimeMillis() - start));
	}

}
