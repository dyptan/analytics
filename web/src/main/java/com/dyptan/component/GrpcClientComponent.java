package com.dyptan.component;

import com.dyptan.gen.proto.ExportStatus;
import com.dyptan.gen.proto.ExportFilter;
import com.dyptan.gen.proto.ExportRequest;
import com.dyptan.gen.proto.ExportServiceGrpc;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Struct;
import com.google.protobuf.util.JsonFormat;
import org.bson.BsonDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
@Component
public class GrpcClientComponent {

    @Autowired
    private ExportServiceGrpc.ExportServiceBlockingStub blockingStub;

    public void exportData(BsonDocument validatedFilter) throws InvalidProtocolBufferException {
        Struct.Builder structBuilder = Struct.newBuilder();
        JsonFormat.parser().ignoringUnknownFields().merge(validatedFilter.toJson(), structBuilder);
        Struct filterStruct = structBuilder.build();
        ExportFilter exportFilter = ExportFilter.newBuilder()
                .setJsonBody(filterStruct)
                .build();

        ExportRequest request = ExportRequest.newBuilder()
                .setFilter(exportFilter)
                .build();


        ExportStatus response = blockingStub.doExport(request);
        System.out.println("Response: " + response);
    }
}
