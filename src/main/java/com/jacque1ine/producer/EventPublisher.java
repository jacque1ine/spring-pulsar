package com.jacque1ine.producer;

import lombok.extern.slf4j.Slf4j;
import org.apache.pulsar.client.api.PulsarClientException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.pulsar.core.PulsarTemplate;
import org.springframework.stereotype.Service;

// TODO - investigate if @Component is better?
@Service
@Slf4j
public class EventPublisher {


    @Value("${spring.pulsar.producer.topic-name1}")
    private String topicName1;

    // TODO - add a @PostConstruct here, which prints out the topicName1. This will help us verify the properties(topicName1) get correctly read.
    // Also check if template is not null, meaning template is successfully instantiated.


    @Autowired
    private PulsarTemplate<Object> template;

    public void publishPlainMessage(String message) throws PulsarClientException {
        template.send(topicName1, message);
        log.info("EventPublisher::publishPlainMessage publish the event {}", message);
    }


}
