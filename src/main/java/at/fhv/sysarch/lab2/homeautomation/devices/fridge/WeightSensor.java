package at.fhv.sysarch.lab2.homeautomation.devices.fridge;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Receive;

import java.util.Map;

public class WeightSensor extends AbstractBehavior<WeightSensor.Command> {

    public interface Command { }
    public record MeasurementRequest(ActorRef<Measurement> receiver, Map<Fridge.Product, Integer> things) implements Command {}
    public record Measurement(double totalWeight) implements Command { }



    public WeightSensor(ActorContext<Command> context) {
        super(context);
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

        getContext().getLog().info("{} measured {} kg", this, result);

        measurementRequest.receiver.tell(new Measurement(result));

        return this;
    }

    @Override
    public String toString() {
        return "weight sensor";
    }
}
