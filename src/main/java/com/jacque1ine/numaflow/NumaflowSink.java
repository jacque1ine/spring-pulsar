package com.jacque1ine.numaflow;

import com.jacque1ine.producer.EventPublisher;
import io.numaproj.numaflow.sinker.Datum;
import io.numaproj.numaflow.sinker.DatumIterator;
import io.numaproj.numaflow.sinker.Response;
import io.numaproj.numaflow.sinker.ResponseList;
import io.numaproj.numaflow.sinker.Server;
import io.numaproj.numaflow.sinker.Sinker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Slf4j
@Component
public class NumaflowSink extends Sinker {

    @Autowired
    private EventPublisher publisher;

    private Server server;

    @PostConstruct // starts server automatically when the spring context initializes
    public void startServer() throws Exception {
        server = new Server(new NumaflowSink());

        server.start();
        server.awaitTermination();


    }

    @Override
    public ResponseList processMessages(DatumIterator datumIterator) {
        ResponseList.ResponseListBuilder responseListBuilder = ResponseList.newBuilder();
        while (true) {
            Datum datum = null;
            try {
                datum = datumIterator.next();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                continue;
            }
            // null means the iterator is closed, so we break the loop
            if (datum == null) {
                break;
            }
            try {
                String msg = new String(datum.getValue());
//                log.info("Received message: {}, headers - {}", msg, datum.getHeaders());
                publisher.publishPlainMessage(msg);
                log.info("processMessage");
                responseListBuilder.addResponse(Response.responseOK(datum.getId())); //good ack to numaflow
            } catch (Exception e) {
                // TODO - print the stack trace of the exception. before building the response.
                responseListBuilder.addResponse(Response.responseFailure( //bad ack
                        datum.getId(),
                        e.getMessage()));
            }
        }
        return responseListBuilder.build();
    }
}
