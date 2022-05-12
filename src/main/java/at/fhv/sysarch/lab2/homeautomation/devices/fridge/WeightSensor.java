package at.fhv.sysarch.lab2.homeautomation.devices.fridge;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.util.Map;

public class WeightSensor extends AbstractBehavior<WeightSensor.Command> {

    public interface Command { }
    public record MeasurementRequest(ActorRef<Measurement> receiver, Map<Fridge.Product, Integer> things) implements Command {}
    public record Measurement(double totalWeight, double remainingWeight) implements Command { }

    private static final double maxLoad = 100;

    public WeightSensor(ActorContext<Command> context) {
        super(context);
    }

    public static Behavior<Command> create() {
        return Behaviors.setup(WeightSensor::new);
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(MeasurementRequest.class, this::onMeasurementRequest)
                .build();
    }

    private Behavior<Command> onMeasurementRequest(MeasurementRequest measurementRequest) {

        var result = measurementRequest.things.entrySet()
                .stream()
                .mapToDouble(e -> e.getKey().weight() * e.getValue())
                .sum();

        getContext().getLog().info("measured {} kg", result);

        measurementRequest.receiver.tell(new Measurement(result, maxLoad - result));

        return this;
    }
}
