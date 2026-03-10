package com.pda.distributed.storage;

import org.junit.Before;
import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class DistributedDirectoryTest {

	private DistributedDirectory directory;

	@Before
	public void setUp() {
		directory = new DistributedDirectory();
	}

	@Test
	public void testUpdateMap_Y_GetFileLocation() {
		// Arrange
		DistributedDirectory.FileMetadata meta = new DistributedDirectory.FileMetadata("video.mp4", 1024);
		meta.nodeAddresses.add("127.0.0.1:50051");
		meta.nodeAddresses.add("127.0.0.1:50052");

		// Act
		directory.updateMap(meta);
		List<String> locations = directory.getFileLocation("video.mp4");

		// Assert
		assertEquals("Debería estar replicado en 2 nodos", 2, locations.size());
		assertTrue(locations.contains("127.0.0.1:50051"));
	}
}