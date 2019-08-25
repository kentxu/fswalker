package com.labimo.fs.fswalker;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import net.openhft.hashing.LongHashFunction;

public class FSWalkerTest {
	private static final Logger LOGGER = LogManager.getLogger(FSWalkerTest.class.getName());

	String homeDir = System.getProperty("user.home");
	static Path testLoadRootPath = null;

	@BeforeClass
	public static void setUp() throws URISyntaxException {
		URL url = FSWalkerTest.class.getResource("testload");
		URI uri = url.toURI();
		File file = new File(uri);
		testLoadRootPath = file.toPath();
	}

	@AfterClass
	public static void tearDown() {
		testLoadRootPath = null;
	}

	@Test
	public void testFSWalkerWithFileOutput() throws FileNotFoundException {
		LOGGER.debug("running testFSWalkerWithFileOutput");
		FSWalker walker = new DefaultFSWalker();
		FSEntryWriter writer = new DefaultFSEntryWriter(new File("target/FSWalkerTest.testFSWalker-result.text"));
		FSVisitor visitor = new FSVisitor(writer);
		walker.setFSVisitor(visitor);
		walker.walk(testLoadRootPath, Integer.MAX_VALUE);
		writer.close();
		assertEquals(5, visitor.getFileCount());
		assertEquals(0, visitor.getFileErrorCount());
		assertEquals(25, visitor.getSize());
		assertEquals(6, visitor.getDirCount());
	}

	@Test
	public void testFSWalker() throws FileNotFoundException {
		FSWalker walker = new DefaultFSWalker();
		FSVisitor visitor = new FSVisitor();
		walker.setFSVisitor(visitor);
		walker.walk(testLoadRootPath, Integer.MAX_VALUE);
		assertEquals(5, visitor.getFileCount());
		assertEquals(0, visitor.getFileErrorCount());
		assertEquals(25, visitor.getSize());
		assertEquals(6, visitor.getDirCount());
	}

	@Test
	public void testFSWalkerDepth1() throws FileNotFoundException {
		FSWalker walker = new DefaultFSWalker();
		FSVisitor visitor = new FSVisitor();
		walker.setFSVisitor(visitor);
		walker.walk(testLoadRootPath, 1);
		assertEquals(0, visitor.getFileCount());
		assertEquals(0, visitor.getFileErrorCount());
		assertEquals(0, visitor.getSize());
		assertEquals(3, visitor.getDirCount());
	}

	@Test
	public void testFSWalkerDepth2() throws FileNotFoundException {
		FSWalker walker = new DefaultFSWalker();
		FSVisitor visitor = new FSVisitor();
		walker.setFSVisitor(visitor);
		walker.walk(testLoadRootPath, 2);
		assertEquals(2, visitor.getFileCount());
		assertEquals(0, visitor.getFileErrorCount());
		assertEquals(10, visitor.getSize());
		assertEquals(6, visitor.getDirCount());
	}

	@Test
	public void testFSWalkerSingleFolderAndFile() throws URISyntaxException {
		Path path = testLoadRootPath.resolve("load1");
		FSWalker walker = new DefaultFSWalker();
		walker.walk(path, Integer.MAX_VALUE);
		assertEquals(2, walker.getFileCount());

		path = testLoadRootPath.resolve("load1/1.txt");
		walker = new DefaultFSWalker();
		walker.walk(path, Integer.MAX_VALUE);
		assertEquals(1, walker.getFileCount());

	}

	@Test
	public void test4OSX() {
		Assume.assumeTrue(System.getProperty("os.name").contains("Mac"));
		assertTrue(true);
	}

	@Test
	public void test4Linux() {
		Assume.assumeFalse(System.getProperty("os.name").contains("Linux"));
		assertTrue(true);
	}

	@Test
	public void testTreadSimple() throws URISyntaxException, InterruptedException {
		Path path = testLoadRootPath.resolve("load1");
		Path path2 = path.getParent().resolve("load2");
		DefaultFSWalker walker = new DefaultFSWalker(path);
		DefaultFSWalker walker2 = new DefaultFSWalker(path2);

		Thread thread1 = new Thread(walker);
		Thread thread2 = new Thread(walker2);
		thread1.run();
		thread2.run();

		thread1.join();
		thread2.join();
		assertEquals(2, walker.getFileCount());
		assertEquals(3, walker2.getFileCount());
	}

	@Test
	public void testThread2() throws URISyntaxException, InterruptedException, IOException {
		LOGGER.info("starting testThread2");
		ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(4);

		FSVisitor visitor = new FSVisitor();
		int loopcount = 10;
		for (int i = 0; i < loopcount; i++) {

			LOGGER.debug("loop " + i);
			Stream<Path> stream = Files.list(testLoadRootPath);
			stream.forEach(new Consumer<Path>() {

				@Override
				public void accept(Path t) {
					DefaultFSWalker walker = new DefaultFSWalker(t);
					walker.setFSVisitor(visitor);
					executor.execute(walker);

				}
			});
			while ( executor.getActiveCount()!=0 || !executor.getQueue().isEmpty() ) {
				Thread.sleep(100);
			}
			stream.close();

		}

		executor.shutdown();
		while (!executor.awaitTermination(100, TimeUnit.SECONDS)) {
			Thread.sleep(100);
		}
		assertEquals(0, visitor.getFileErrorCount());
		assertEquals(5 * loopcount, visitor.getFileCount());
		LOGGER.info("ending testThread2");

	}

	@Test
	public void testContentHashLib() throws IOException {
		long hash = LongHashFunction.xx().hashChars("hello");
		assertEquals(-4777684530141795464L, hash);
		Path filepath = testLoadRootPath.resolve("load1/1.txt");
		try (InputStream fis = Files.newInputStream(filepath, StandardOpenOption.READ)) {
			byte[] bytes = new byte[20];
			int len = fis.read(bytes, 0, 20);
			assertEquals(5, len);
			hash = LongHashFunction.xx().hashBytes(bytes);
			assertEquals(-8642330888085176338L, hash);
		}
	}

	@Test
	public void testLongToBytes() {
		long l = 1823592678034044L;
		assertArrayEquals(Utils.longToBytes(l), Utils.longToBytes2(l));
	}

}
