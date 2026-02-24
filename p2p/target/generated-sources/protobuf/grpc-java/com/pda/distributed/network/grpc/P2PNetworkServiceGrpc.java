package com.pda.distributed.network.grpc;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.61.0)",
    comments = "Source: touche-service.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class P2PNetworkServiceGrpc {

  private P2PNetworkServiceGrpc() {}

  public static final java.lang.String SERVICE_NAME = "com.pda.distributed.network.grpc.P2PNetworkService";

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
    if ((getPingMethod = P2PNetworkServiceGrpc.getPingMethod) == null) {
      synchronized (P2PNetworkServiceGrpc.class) {
        if ((getPingMethod = P2PNetworkServiceGrpc.getPingMethod) == null) {
          P2PNetworkServiceGrpc.getPingMethod = getPingMethod =
              io.grpc.MethodDescriptor.<com.pda.distributed.network.grpc.PingRequest, com.pda.distributed.network.grpc.PingResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ping"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.pda.distributed.network.grpc.PingRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.pda.distributed.network.grpc.PingResponse.getDefaultInstance()))
              .setSchemaDescriptor(new P2PNetworkServiceMethodDescriptorSupplier("ping"))
              .build();
        }
      }
    }
    return getPingMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static P2PNetworkServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<P2PNetworkServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<P2PNetworkServiceStub>() {
        @java.lang.Override
        public P2PNetworkServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new P2PNetworkServiceStub(channel, callOptions);
        }
      };
    return P2PNetworkServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static P2PNetworkServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<P2PNetworkServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<P2PNetworkServiceBlockingStub>() {
        @java.lang.Override
        public P2PNetworkServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new P2PNetworkServiceBlockingStub(channel, callOptions);
        }
      };
    return P2PNetworkServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static P2PNetworkServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<P2PNetworkServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<P2PNetworkServiceFutureStub>() {
        @java.lang.Override
        public P2PNetworkServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new P2PNetworkServiceFutureStub(channel, callOptions);
        }
      };
    return P2PNetworkServiceFutureStub.newStub(factory, channel);
  }

  /**
   */
  public interface AsyncService {

    /**
     */
    default void ping(com.pda.distributed.network.grpc.PingRequest request,
        io.grpc.stub.StreamObserver<com.pda.distributed.network.grpc.PingResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getPingMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service P2PNetworkService.
   */
  public static abstract class P2PNetworkServiceImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return P2PNetworkServiceGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service P2PNetworkService.
   */
  public static final class P2PNetworkServiceStub
      extends io.grpc.stub.AbstractAsyncStub<P2PNetworkServiceStub> {
    private P2PNetworkServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected P2PNetworkServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new P2PNetworkServiceStub(channel, callOptions);
    }

    /**
     */
    public void ping(com.pda.distributed.network.grpc.PingRequest request,
        io.grpc.stub.StreamObserver<com.pda.distributed.network.grpc.PingResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getPingMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service P2PNetworkService.
   */
  public static final class P2PNetworkServiceBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<P2PNetworkServiceBlockingStub> {
    private P2PNetworkServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected P2PNetworkServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new P2PNetworkServiceBlockingStub(channel, callOptions);
    }

    /**
     */
    public com.pda.distributed.network.grpc.PingResponse ping(com.pda.distributed.network.grpc.PingRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getPingMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service P2PNetworkService.
   */
  public static final class P2PNetworkServiceFutureStub
      extends io.grpc.stub.AbstractFutureStub<P2PNetworkServiceFutureStub> {
    private P2PNetworkServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected P2PNetworkServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new P2PNetworkServiceFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.pda.distributed.network.grpc.PingResponse> ping(
        com.pda.distributed.network.grpc.PingRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getPingMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_PING = 0;

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
        .build();
  }

  private static abstract class P2PNetworkServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    P2PNetworkServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.pda.distributed.network.grpc.ToucheService.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("P2PNetworkService");
    }
  }

  private static final class P2PNetworkServiceFileDescriptorSupplier
      extends P2PNetworkServiceBaseDescriptorSupplier {
    P2PNetworkServiceFileDescriptorSupplier() {}
  }

  private static final class P2PNetworkServiceMethodDescriptorSupplier
      extends P2PNetworkServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    P2PNetworkServiceMethodDescriptorSupplier(java.lang.String methodName) {
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
      synchronized (P2PNetworkServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new P2PNetworkServiceFileDescriptorSupplier())
              .addMethod(getPingMethod())
              .build();
        }
      }
    }
    return result;
  }
}
