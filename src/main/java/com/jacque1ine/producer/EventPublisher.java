package com.jacque1ine.producer;

import lombok.extern.slf4j.Slf4j;
import org.apache.pulsar.client.api.PulsarClientException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.pulsar.core.PulsarTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EventPublisher {


    @Value("${spring.pulsar.producer.topic-name}")
    private String topicName;


    @Autowired
    private PulsarTemplate<Object> template;

    public void publishPlainMessage(String message) throws PulsarClientException {
        template.send(topicName, message);
        log.info("EventPublisher::publishPlainMessage publish the event {}", message);
    }

}