package com.pda.distributed.services;

import com.pda.distributed.storage.DistributedDirectory;
import org.junit.Before;
import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class StorageCoordinatorTest {

	private StorageCoordinator coordinator;

	@Before
	public void setUp() {
		coordinator = new StorageCoordinator();
		coordinator.setDistributedDirectory(new DistributedDirectory());
	}

	@Test
	public void testAllocateToNodes_RetornaListaVaciaPorDefecto() {
		DistributedDirectory.FileMetadata meta = new DistributedDirectory.FileMetadata("test.txt", 100);
		List<String> nodosAsignados = coordinator.allocateToNodes(meta);

		// Assert temporal hasta que implementes la lógica real de balanceo
		assertNotNull(nodosAsignados);
	}
}