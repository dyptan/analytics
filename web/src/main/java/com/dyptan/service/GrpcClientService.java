package com.dyptan.service;
import com.dyptan.component.GrpcClientComponent;
import org.springframework.stereotype.Service;

@Service
public class GrpcClientService {
    private final GrpcClientComponent grpcClientService;

    public GrpcClientService(GrpcClientComponent grpcClientService) {
        this.grpcClientService = grpcClientService;
    }

    public void invokeGrpcService() {
        grpcClientService.callRemoteService();
    }
}
