package com.pda.distributed.services;

import com.pda.distributed.core.RingType;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class QuorumServiceTest {

	private QuorumService quorumService;

	@Before
	public void setUp() {
		// Inicializamos sin red para probar solo la lógica pura (Unit Testing)
		quorumService = new QuorumService(null);
		// Simulamos que nuestro nodo tiene el ID 50 para las pruebas del Bully
		quorumService.setMiNodeId(50);
	}

	// --- Tests de Asignación de Anillos ---

	@Test
	public void testEvaluarIngreso_PrimerNodo_AsignaRingA() {
		String anilloAsignado = quorumService.evaluarIngresoNuevoNodo("127.0.0.1:50052", 2);
		assertEquals("El primer nodo en unirse debe ser líder (RING_A)", "RING_A", anilloAsignado);
	}

	@Test
	public void testEvaluarIngreso_ExcedeUmbral_AsignaRingB() {
		// Llenamos el cupo máximo de líderes (2)
		quorumService.registrarNodo("127.0.0.1:50050", RingType.RING_A);
		quorumService.registrarNodo("127.0.0.1:50051", RingType.RING_A);

		// Intentamos meter un tercer nodo
		String anilloAsignado = quorumService.evaluarIngresoNuevoNodo("127.0.0.1:50052", 3);

		assertEquals("Si ya hay 2 líderes, el tercero debe ir a trabajadores (RING_B)", "RING_B", anilloAsignado);
	}

	// --- Tests del Algoritmo Bully ---

	@Test
	public void testEvaluarVotoBully_CandidatoMayor_CedeVoto() {
		// Act: Un candidato con ID 100 (mayor que nuestro 50) pide ser líder
		boolean voto = quorumService.evaluarVotoBully(100);

		// Assert: Deberíamos aceptar porque su ID es mayor
		assertTrue("Debe votar 'true' si el ID del candidato (100) es mayor que el nuestro (50)", voto);
	}

	@Test
	public void testEvaluarVotoBully_CandidatoMenor_NiegaVoto() {
		// Act: Un candidato con ID 10 (menor que nuestro 50) pide ser líder
		boolean voto = quorumService.evaluarVotoBully(10);

		// Assert: Deberíamos rechazar porque nosotros tenemos mayor prioridad
		assertFalse("Debe votar 'false' si el ID del candidato (10) es menor que el nuestro (50)", voto);
	}
}