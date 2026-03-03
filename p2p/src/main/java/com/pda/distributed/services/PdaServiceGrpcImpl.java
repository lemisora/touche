package com.pda.distributed.services;

import com.pda.distributed.utils.ConsoleLogger;

import com.pda.distributed.network.grpc.PdaServiceGrpc;
import com.pda.distributed.network.grpc.PeticionEstado;
import com.pda.distributed.network.grpc.PeticionSubida;
import com.pda.distributed.network.grpc.PeticionVoto;
import com.pda.distributed.network.grpc.PingRequest;
import com.pda.distributed.network.grpc.PingResponse;
import com.pda.distributed.network.grpc.RespuestaEstado;
import com.pda.distributed.network.grpc.RespuestaSubida;
import com.pda.distributed.network.grpc.RespuestaVoto;
import io.grpc.stub.StreamObserver;

// Implementación de los servicios gRPC
public class PdaServiceGrpcImpl extends PdaServiceGrpc.PdaServiceImplBase {

    private final StateSyncService stateSyncService;
    private final StorageCoordinator storageCoordinator;
    private final int miPuerto;

    // Cooldown para evitar que un nodo vote "Sí" a múltiples candidatos en la misma
    // ventana de tiempo
    private long ultimoVotoElectionEmitido = 0;

    // Constructor que recibe los tres servicios y el puerto local
    public PdaServiceGrpcImpl(StateSyncService stateSyncService, StorageCoordinator storageCoordinator, int miPuerto) {
        this.stateSyncService = stateSyncService;
        this.storageCoordinator = storageCoordinator;
        this.miPuerto = miPuerto;
    }

    @Override
    public void ping(PingRequest request, StreamObserver<PingResponse> responseObserver) {
        ConsoleLogger.info("Log", "Recibido ping: " + request.getMensajeSaludo());
        PingResponse response = PingResponse.newBuilder().setExito(true).setRespuesta("Pong").build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void sincronizarEstado(PeticionEstado request, StreamObserver<RespuestaEstado> responseObserver) {
        String estadoRecibido = request.getDatosEstado();
        int puertoOrigen = request.getPuertoOrigen();

        if (stateSyncService != null) {
            stateSyncService.recibirEstado(estadoRecibido, puertoOrigen);
        } else {
            ConsoleLogger.error("Error", "GRPC: StateSyncService no inicializado!");
        }

        responseObserver.onNext(RespuestaEstado.newBuilder().setExito(true).build());
        responseObserver.onCompleted();
    }

    @Override
    public void votar(PeticionVoto request, StreamObserver<RespuestaVoto> responseObserver) {
        String idAccion = request.getIdAccion();
        int puertoOrigen = request.getPuertoOrigen();
        ConsoleLogger.info("Log",
                "GRPC: Recibido voto para acción: " + idAccion + " (desde puerto " + puertoOrigen + ")");

        boolean votoAFavor = true;

        if ("ELECTION".equals(idAccion)) {
            synchronized (this) {
                if (this.stateSyncService != null && this.stateSyncService.hayLiderActivo()) {
                    // Ya tenemos líder
                    votoAFavor = false;
                    ConsoleLogger.info("Log", "GRPC: Voto para ELECTION denegado: ya existe un Lider.");
                } else if (System.currentTimeMillis() - ultimoVotoElectionEmitido < 10000) {
                    // Prevenir "Split Brain" si recibimos 2 peticiones al mismo tiempo
                    votoAFavor = false;
                    ConsoleLogger.info("Log", "GRPC: Voto denegado: ya voté recientemente (cooldown).");
                } else {
                    ConsoleLogger.info("Log", "GRPC: Concediendo voto para ELECTION a puerto " + puertoOrigen);
                    votoAFavor = true;
                    ultimoVotoElectionEmitido = System.currentTimeMillis();
                }
            }
        }

        responseObserver.onNext(RespuestaVoto.newBuilder().setAcepta(votoAFavor).build());
        responseObserver.onCompleted();
    }

    @Override
    public void subirFragmento(PeticionSubida request, StreamObserver<RespuestaSubida> responseObserver) {
        String idArchivo = request.getIdArchivo();
        byte[] fragmento = request.getFragmento().toByteArray();

        ConsoleLogger.info("Log", "GRPC: Recibida peticion de subir archivo: " + idArchivo);

        if (storageCoordinator != null) {
            storageCoordinator.procesarFragmentoEntrante(idArchivo, fragmento);
        } else {
            ConsoleLogger.error("Error", "GRPC: StorageCoordinator no inicializado!");
        }

        responseObserver.onNext(RespuestaSubida.newBuilder().setExito(true).build());
        responseObserver.onCompleted();
    }
}
