package com.pda.distributed.network.grpc;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 * <pre>
 * Servicio Principal del Nodo
 * </pre>
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.61.0)",
    comments = "Source: touche-service.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class PdaServiceGrpc {

  private PdaServiceGrpc() {}

  public static final java.lang.String SERVICE_NAME = "com.pda.distributed.network.grpc.PdaService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.pda.distributed.network.grpc.PingRequest,
      com.pda.distributed.network.grpc.PingResponse> getPingMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ping",
      requestType = com.pda.distributed.network.grpc.PingRequest.class,
      responseType = com.pda.distributed.network.grpc.PingResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.pda.distributed.network.grpc.PingRequest,
      com.pda.distributed.network.grpc.PingResponse> getPingMethod() {
    io.grpc.MethodDescriptor<com.pda.distributed.network.grpc.PingRequest, com.pda.distributed.network.grpc.PingResponse> getPingMethod;
    if ((getPingMethod = PdaServiceGrpc.getPingMethod) == null) {
      synchronized (PdaServiceGrpc.class) {
        if ((getPingMethod = PdaServiceGrpc.getPingMethod) == null) {
          PdaServiceGrpc.getPingMethod = getPingMethod =
              io.grpc.MethodDescriptor.<com.pda.distributed.network.grpc.PingRequest, com.pda.distributed.network.grpc.PingResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ping"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.pda.distributed.network.grpc.PingRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.pda.distributed.network.grpc.PingResponse.getDefaultInstance()))
              .setSchemaDescriptor(new PdaServiceMethodDescriptorSupplier("ping"))
              .build();
        }
      }
    }
    return getPingMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.pda.distributed.network.grpc.PeticionEstado,
      com.pda.distributed.network.grpc.RespuestaEstado> getSincronizarEstadoMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "sincronizarEstado",
      requestType = com.pda.distributed.network.grpc.PeticionEstado.class,
      responseType = com.pda.distributed.network.grpc.RespuestaEstado.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.pda.distributed.network.grpc.PeticionEstado,
      com.pda.distributed.network.grpc.RespuestaEstado> getSincronizarEstadoMethod() {
    io.grpc.MethodDescriptor<com.pda.distributed.network.grpc.PeticionEstado, com.pda.distributed.network.grpc.RespuestaEstado> getSincronizarEstadoMethod;
    if ((getSincronizarEstadoMethod = PdaServiceGrpc.getSincronizarEstadoMethod) == null) {
      synchronized (PdaServiceGrpc.class) {
        if ((getSincronizarEstadoMethod = PdaServiceGrpc.getSincronizarEstadoMethod) == null) {
          PdaServiceGrpc.getSincronizarEstadoMethod = getSincronizarEstadoMethod =
              io.grpc.MethodDescriptor.<com.pda.distributed.network.grpc.PeticionEstado, com.pda.distributed.network.grpc.RespuestaEstado>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "sincronizarEstado"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.pda.distributed.network.grpc.PeticionEstado.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.pda.distributed.network.grpc.RespuestaEstado.getDefaultInstance()))
              .setSchemaDescriptor(new PdaServiceMethodDescriptorSupplier("sincronizarEstado"))
              .build();
        }
      }
    }
    return getSincronizarEstadoMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.pda.distributed.network.grpc.PeticionVoto,
      com.pda.distributed.network.grpc.RespuestaVoto> getVotarMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "votar",
      requestType = com.pda.distributed.network.grpc.PeticionVoto.class,
      responseType = com.pda.distributed.network.grpc.RespuestaVoto.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.pda.distributed.network.grpc.PeticionVoto,
      com.pda.distributed.network.grpc.RespuestaVoto> getVotarMethod() {
    io.grpc.MethodDescriptor<com.pda.distributed.network.grpc.PeticionVoto, com.pda.distributed.network.grpc.RespuestaVoto> getVotarMethod;
    if ((getVotarMethod = PdaServiceGrpc.getVotarMethod) == null) {
      synchronized (PdaServiceGrpc.class) {
        if ((getVotarMethod = PdaServiceGrpc.getVotarMethod) == null) {
          PdaServiceGrpc.getVotarMethod = getVotarMethod =
              io.grpc.MethodDescriptor.<com.pda.distributed.network.grpc.PeticionVoto, com.pda.distributed.network.grpc.RespuestaVoto>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "votar"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.pda.distributed.network.grpc.PeticionVoto.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.pda.distributed.network.grpc.RespuestaVoto.getDefaultInstance()))
              .setSchemaDescriptor(new PdaServiceMethodDescriptorSupplier("votar"))
              .build();
        }
      }
    }
    return getVotarMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.pda.distributed.network.grpc.PeticionSubida,
      com.pda.distributed.network.grpc.RespuestaSubida> getSubirFragmentoMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "subirFragmento",
      requestType = com.pda.distributed.network.grpc.PeticionSubida.class,
      responseType = com.pda.distributed.network.grpc.RespuestaSubida.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.pda.distributed.network.grpc.PeticionSubida,
      com.pda.distributed.network.grpc.RespuestaSubida> getSubirFragmentoMethod() {
    io.grpc.MethodDescriptor<com.pda.distributed.network.grpc.PeticionSubida, com.pda.distributed.network.grpc.RespuestaSubida> getSubirFragmentoMethod;
    if ((getSubirFragmentoMethod = PdaServiceGrpc.getSubirFragmentoMethod) == null) {
      synchronized (PdaServiceGrpc.class) {
        if ((getSubirFragmentoMethod = PdaServiceGrpc.getSubirFragmentoMethod) == null) {
          PdaServiceGrpc.getSubirFragmentoMethod = getSubirFragmentoMethod =
              io.grpc.MethodDescriptor.<com.pda.distributed.network.grpc.PeticionSubida, com.pda.distributed.network.grpc.RespuestaSubida>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "subirFragmento"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.pda.distributed.network.grpc.PeticionSubida.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.pda.distributed.network.grpc.RespuestaSubida.getDefaultInstance()))
              .setSchemaDescriptor(new PdaServiceMethodDescriptorSupplier("subirFragmento"))
              .build();
        }
      }
    }
    return getSubirFragmentoMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static PdaServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<PdaServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<PdaServiceStub>() {
        @java.lang.Override
        public PdaServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new PdaServiceStub(channel, callOptions);
        }
      };
    return PdaServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static PdaServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<PdaServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<PdaServiceBlockingStub>() {
        @java.lang.Override
        public PdaServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new PdaServiceBlockingStub(channel, callOptions);
        }
      };
    return PdaServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static PdaServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<PdaServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<PdaServiceFutureStub>() {
        @java.lang.Override
        public PdaServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new PdaServiceFutureStub(channel, callOptions);
        }
      };
    return PdaServiceFutureStub.newStub(factory, channel);
  }

  /**
   * <pre>
   * Servicio Principal del Nodo
   * </pre>
   */
  public interface AsyncService {

    /**
     */
    default void ping(com.pda.distributed.network.grpc.PingRequest request,
        io.grpc.stub.StreamObserver<com.pda.distributed.network.grpc.PingResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getPingMethod(), responseObserver);
    }

    /**
     */
    default void sincronizarEstado(com.pda.distributed.network.grpc.PeticionEstado request,
        io.grpc.stub.StreamObserver<com.pda.distributed.network.grpc.RespuestaEstado> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getSincronizarEstadoMethod(), responseObserver);
    }

    /**
     */
    default void votar(com.pda.distributed.network.grpc.PeticionVoto request,
        io.grpc.stub.StreamObserver<com.pda.distributed.network.grpc.RespuestaVoto> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getVotarMethod(), responseObserver);
    }

    /**
     */
    default void subirFragmento(com.pda.distributed.network.grpc.PeticionSubida request,
        io.grpc.stub.StreamObserver<com.pda.distributed.network.grpc.RespuestaSubida> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getSubirFragmentoMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service PdaService.
   * <pre>
   * Servicio Principal del Nodo
   * </pre>
   */
  public static abstract class PdaServiceImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return PdaServiceGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service PdaService.
   * <pre>
   * Servicio Principal del Nodo
   * </pre>
   */
  public static final class PdaServiceStub
      extends io.grpc.stub.AbstractAsyncStub<PdaServiceStub> {
    private PdaServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected PdaServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new PdaServiceStub(channel, callOptions);
    }

    /**
     */
    public void ping(com.pda.distributed.network.grpc.PingRequest request,
        io.grpc.stub.StreamObserver<com.pda.distributed.network.grpc.PingResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getPingMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void sincronizarEstado(com.pda.distributed.network.grpc.PeticionEstado request,
        io.grpc.stub.StreamObserver<com.pda.distributed.network.grpc.RespuestaEstado> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getSincronizarEstadoMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void votar(com.pda.distributed.network.grpc.PeticionVoto request,
        io.grpc.stub.StreamObserver<com.pda.distributed.network.grpc.RespuestaVoto> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getVotarMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void subirFragmento(com.pda.distributed.network.grpc.PeticionSubida request,
        io.grpc.stub.StreamObserver<com.pda.distributed.network.grpc.RespuestaSubida> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getSubirFragmentoMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service PdaService.
   * <pre>
   * Servicio Principal del Nodo
   * </pre>
   */
  public static final class PdaServiceBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<PdaServiceBlockingStub> {
    private PdaServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected PdaServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new PdaServiceBlockingStub(channel, callOptions);
    }

    /**
     */
    public com.pda.distributed.network.grpc.PingResponse ping(com.pda.distributed.network.grpc.PingRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getPingMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.pda.distributed.network.grpc.RespuestaEstado sincronizarEstado(com.pda.distributed.network.grpc.PeticionEstado request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getSincronizarEstadoMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.pda.distributed.network.grpc.RespuestaVoto votar(com.pda.distributed.network.grpc.PeticionVoto request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getVotarMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.pda.distributed.network.grpc.RespuestaSubida subirFragmento(com.pda.distributed.network.grpc.PeticionSubida request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getSubirFragmentoMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service PdaService.
   * <pre>
   * Servicio Principal del Nodo
   * </pre>
   */
  public static final class PdaServiceFutureStub
      extends io.grpc.stub.AbstractFutureStub<PdaServiceFutureStub> {
    private PdaServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected PdaServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new PdaServiceFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.pda.distributed.network.grpc.PingResponse> ping(
        com.pda.distributed.network.grpc.PingRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getPingMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.pda.distributed.network.grpc.RespuestaEstado> sincronizarEstado(
        com.pda.distributed.network.grpc.PeticionEstado request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getSincronizarEstadoMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.pda.distributed.network.grpc.RespuestaVoto> votar(
        com.pda.distributed.network.grpc.PeticionVoto request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getVotarMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.pda.distributed.network.grpc.RespuestaSubida> subirFragmento(
        com.pda.distributed.network.grpc.PeticionSubida request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getSubirFragmentoMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_PING = 0;
  private static final int METHODID_SINCRONIZAR_ESTADO = 1;
  private static final int METHODID_VOTAR = 2;
  private static final int METHODID_SUBIR_FRAGMENTO = 3;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final AsyncService serviceImpl;
    private final int methodId;

    MethodHandlers(AsyncService serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_PING:
          serviceImpl.ping((com.pda.distributed.network.grpc.PingRequest) request,
              (io.grpc.stub.StreamObserver<com.pda.distributed.network.grpc.PingResponse>) responseObserver);
          break;
        case METHODID_SINCRONIZAR_ESTADO:
          serviceImpl.sincronizarEstado((com.pda.distributed.network.grpc.PeticionEstado) request,
              (io.grpc.stub.StreamObserver<com.pda.distributed.network.grpc.RespuestaEstado>) responseObserver);
          break;
        case METHODID_VOTAR:
          serviceImpl.votar((com.pda.distributed.network.grpc.PeticionVoto) request,
              (io.grpc.stub.StreamObserver<com.pda.distributed.network.grpc.RespuestaVoto>) responseObserver);
          break;
        case METHODID_SUBIR_FRAGMENTO:
          serviceImpl.subirFragmento((com.pda.distributed.network.grpc.PeticionSubida) request,
              (io.grpc.stub.StreamObserver<com.pda.distributed.network.grpc.RespuestaSubida>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  public static final io.grpc.ServerServiceDefinition bindService(AsyncService service) {
    return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
        .addMethod(
          getPingMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.pda.distributed.network.grpc.PingRequest,
              com.pda.distributed.network.grpc.PingResponse>(
                service, METHODID_PING)))
        .addMethod(
          getSincronizarEstadoMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.pda.distributed.network.grpc.PeticionEstado,
              com.pda.distributed.network.grpc.RespuestaEstado>(
                service, METHODID_SINCRONIZAR_ESTADO)))
        .addMethod(
          getVotarMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.pda.distributed.network.grpc.PeticionVoto,
              com.pda.distributed.network.grpc.RespuestaVoto>(
                service, METHODID_VOTAR)))
        .addMethod(
          getSubirFragmentoMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.pda.distributed.network.grpc.PeticionSubida,
              com.pda.distributed.network.grpc.RespuestaSubida>(
                service, METHODID_SUBIR_FRAGMENTO)))
        .build();
  }

  private static abstract class PdaServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    PdaServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.pda.distributed.network.grpc.ToucheService.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("PdaService");
    }
  }

  private static final class PdaServiceFileDescriptorSupplier
      extends PdaServiceBaseDescriptorSupplier {
    PdaServiceFileDescriptorSupplier() {}
  }

  private static final class PdaServiceMethodDescriptorSupplier
      extends PdaServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    PdaServiceMethodDescriptorSupplier(java.lang.String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (PdaServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new PdaServiceFileDescriptorSupplier())
              .addMethod(getPingMethod())
              .addMethod(getSincronizarEstadoMethod())
              .addMethod(getVotarMethod())
              .addMethod(getSubirFragmentoMethod())
              .build();
        }
      }
    }
    return result;
  }
}
