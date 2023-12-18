import com.dyptan.gen.proto.AdvertisementMessage;
import com.dyptan.gen.proto.FilterMessage;
import com.dyptan.gen.proto.LoaderServiceGrpc;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.Iterator;

import static com.mongodb.client.model.Filters.eq;
import com.mongodb.client.*;
import org.bson.Document;
import org.bson.conversions.Bson;
public class LoaderSpec {
    public static void test( String[] args ) {
        // Replace the placeholder with your MongoDB deployment's connection string
        String uri = "mongodb://localhost:27017";
        try (MongoClient mongoClient = MongoClients.create(uri)) {
            MongoDatabase database = mongoClient.getDatabase("ria");
            MongoCollection<Document> collection = database.getCollection("autos");
            Document doc = collection.find(eq("markNameEng", "volvo")).first();
            if (doc != null) {
                System.out.println(doc.toJson());
            } else {
                System.out.println("No matching documents found.");
            }
        }
    }
    public static void main( String[] args ) {

        // Create a channel to connect to the gRPC server
        ManagedChannel channel = ManagedChannelBuilder
                .forAddress("localhost", 8082)  // Replace with your server's address and port
                .usePlaintext()  // Use plaintext (non-secure) communication for simplicity
                .build();

        // Create a gRPC client using the channel
        LoaderServiceGrpc.LoaderServiceBlockingStub blockingStub = LoaderServiceGrpc.newBlockingStub(channel);

        // Create a request
        FilterMessage request = FilterMessage.newBuilder()
                .setBrands("volvo")
                .setModels("xc90")
                .setYearFrom(2000)
                .setYearTo(2022)
                .setRaceFrom(0)
                .setRaceTo(40000)
                .setPriceFrom(0)
                .setPriceTo(100000)
                .build();

        // Make an RPC call and get the response
        Iterator<AdvertisementMessage> response = blockingStub.getAdvertisements(request);

        for (Iterator<AdvertisementMessage> it = response; it.hasNext(); ) {
            AdvertisementMessage ad = it.next();
            System.out.println("Response received: " + ad);

        }
        // Process the response

        // Shutdown the channel when done
        channel.shutdown();
    }
}
