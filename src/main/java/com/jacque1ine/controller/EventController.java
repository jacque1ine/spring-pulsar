package com.jacque1ine.controller;


import org.apache.pulsar.client.api.PulsarClientException;
import com.jacque1ine.producer.EventPublisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/producer")
public class EventController {


    @Autowired
    private EventPublisher publisher;

    // add code so that instead of waiting for /producer/text/message to be called
    // we just have the logger going from Numaflow
    @GetMapping("/text/{message}")
    public String sendTextEvent(@PathVariable String message) throws PulsarClientException {
        publisher.publishPlainMessage(message); // and we still need this but this would be publishing each result forom the logger
        return "message published !";
    }

    //have two options, either keep calling this method, OR just replace this code



}