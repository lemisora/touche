package com.pda.distributed.services;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertNotNull;

public class StateSyncServiceTest {

	private StateSyncService syncService;

	@Before
	public void setUp() {
		// Inicializamos con null porque aún no probamos la red real
		syncService = new StateSyncService(null);
	}

	@Test
	public void testRecibirEstado_SeEjecutaCorrectamente() {
		// Simplemente probamos que la función no lance excepciones nulas
		syncService.recibirEstado("{\"archivo\":\"video.mp4\"}", "127.0.0.1:50051");
		assertNotNull(syncService);
	}
}