package io.openmessaging.chaos.driver.demo.imp;

import io.openmessaging.chaos.driver.demo.api.MQChaosPushConsumerAdapter;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.exception.MQClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RMQChaosPushConsumer extends MQChaosPushConsumerAdapter {

    private static final Logger log = LoggerFactory.getLogger(RMQChaosPushConsumer.class);
    private DefaultMQPushConsumer defaultMQPushConsumer;

    public RMQChaosPushConsumer(DefaultMQPushConsumer defaultMQPushConsumer) {
        this.defaultMQPushConsumer = defaultMQPushConsumer;
    }

    @Override public void start() {
        try {
            if (defaultMQPushConsumer != null) {
                defaultMQPushConsumer.start();
            }
        } catch (MQClientException e) {
            log.error("Failed to start the RocketMQChaosPullConsumer instance.", e);
        }
    }

    @Override public void close() {
        if (defaultMQPushConsumer != null) {
            defaultMQPushConsumer.shutdown();
        }
    }

}
