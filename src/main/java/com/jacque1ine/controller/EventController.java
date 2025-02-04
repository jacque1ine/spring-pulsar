package com.jacque1ine.controller;

import com.jacque1ine.dto.Customer;
import org.apache.pulsar.client.api.PulsarClientException;
import com.jacque1ine.producer.EventPublisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/producer")
public class EventController {


    @Autowired
    private EventPublisher publisher;

    @GetMapping("/text/{message}")
    public String sendTextEvent(@PathVariable String message) throws PulsarClientException {
        publisher.publishPlainMessage(message);
        return "message published !";
    }


    @PostMapping("/raw")
    public String sendTextEvent(@RequestBody Customer customer) throws PulsarClientException {
        publisher.publishRawMessages(customer);
        return "Custom object published !";
    }

}