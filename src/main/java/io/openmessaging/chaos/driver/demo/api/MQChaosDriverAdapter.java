package io.openmessaging.chaos.driver.demo.api;

import io.openmessaging.chaos.driver.mq.MQChaosDriver;
import io.openmessaging.chaos.driver.mq.MQChaosNode;
import io.openmessaging.chaos.driver.mq.MQChaosPullConsumer;
import java.util.List;

public abstract class MQChaosDriverAdapter implements MQChaosDriver {

    public MQChaosPullConsumer createPullConsumer(String topic, String subscriptionName) {
        throw new UnsupportedOperationException("No need to create pull consumer");
    }

    public MQChaosNode createChaosNode(String node, List<String> nodes) {
        throw new UnsupportedOperationException("No need to create chaos node");
    }
}
