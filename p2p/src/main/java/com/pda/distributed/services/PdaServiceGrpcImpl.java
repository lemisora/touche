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

    private final QuorumService quorumService;
    private final StateSyncService stateSyncService;
    private final StorageCoordinator storageCoordinator;

    // Constructor que recibe los tres servicios
    public PdaServiceGrpcImpl(QuorumService quorumService, StateSyncService stateSyncService,
            StorageCoordinator storageCoordinator) {
        this.quorumService = quorumService;
        this.stateSyncService = stateSyncService;
        this.storageCoordinator = storageCoordinator;
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

        if (stateSyncService != null) {
            // Pasamos puerto 0 como default ya que extraer IPs de grpc en java excede este
            // alcance
            stateSyncService.recibirEstado(estadoRecibido, 0);
        } else {
            ConsoleLogger.error("Error", "GRPC: StateSyncService no inicializado!");
        }

        responseObserver.onNext(RespuestaEstado.newBuilder().setExito(true).build());
        responseObserver.onCompleted();
    }

    @Override
    public void votar(PeticionVoto request, StreamObserver<RespuestaVoto> responseObserver) {
        String idAccion = request.getIdAccion();
        ConsoleLogger.info("Log", "GRPC: Recibido voto para acción: " + idAccion);

        // Simular que el voto siempre es a favor (para la práctica)
        boolean votoAFavor = true;

        if (quorumService != null) {
            quorumService.recibirVoto(idAccion, votoAFavor);
        } else {
            ConsoleLogger.error("Error", "GRPC: QuorumService no inicializado!");
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
