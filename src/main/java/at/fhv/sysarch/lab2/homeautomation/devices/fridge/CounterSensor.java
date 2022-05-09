package at.fhv.sysarch.lab2.homeautomation.devices.fridge;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.time.Duration;
import java.util.Map;

public class CounterSensor extends AbstractBehavior<CounterSensor.Command> {



    public interface Command { }
    public record MeasurementRequest(ActorRef<Measurement> receiver, Map<Fridge.Product, Integer> things) implements Command { }
    public record Measurement(int amount) implements Command { }

    public CounterSensor(ActorContext<Command> context) {
        super(context);
    }

    public static Behavior<Command> create() {
        return Behaviors.setup(CounterSensor::new);
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(MeasurementRequest.class, this::onVolumeRequest)
                .build();
    }

    public Behavior<Command> onVolumeRequest(MeasurementRequest measurementRequest) {
        var result = measurementRequest.things.values().stream().reduce(0, Integer::sum);
        getContext().getLog().info("{} measured {} items", this, result);
        measurementRequest.receiver.tell(new Measurement(result));
        return this;
    }

    @Override
    public String toString() {
        return "counter sensor";
    }
}
