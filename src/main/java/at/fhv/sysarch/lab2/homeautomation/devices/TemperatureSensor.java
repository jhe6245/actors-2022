package at.fhv.sysarch.lab2.homeautomation.devices;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;
import at.fhv.sysarch.lab2.homeautomation.environmental.AmbientTemperature;

import java.time.Duration;

public class TemperatureSensor extends AbstractBehavior<TemperatureSensor.Command> {

    private final ActorRef<AmbientTemperature.Command> ambientTemp;

    public interface Command {}

    public record TakeReading(ActorRef<Reading> reader) implements Command {}
    public record Reading(double value, String unit) implements Command {}

    private record ReceiveEnvironmentTemp(double val, ActorRef<Reading> forwardTo) implements Command {}

    private final String groupId;
    private final String deviceId;


    private TemperatureSensor(ActorContext<Command> context, String groupId, String deviceId, ActorRef<AmbientTemperature.Command> ambientTemp) {
        super(context);
        this.groupId = groupId;
        this.deviceId = deviceId;
        this.ambientTemp = ambientTemp;
    }

    public static Behavior<Command> create(String groupId, String deviceId, ActorRef<AmbientTemperature.Command> ambientTemp) {
        return Behaviors.setup(context ->
                new TemperatureSensor(context, groupId, deviceId, ambientTemp)
        );
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(TakeReading.class, this::onTakeReading)
                .onMessage(ReceiveEnvironmentTemp.class, this::onReceiveEnvironmentTemp)
                .build();
    }

    private Behavior<Command> onTakeReading(TakeReading takeReading) {
        getContext().ask(
                AmbientTemperature.TempResponse.class,
                ambientTemp,
                Duration.ofMillis(100),
                AmbientTemperature.TempRequest::new,
                (res, err) -> new ReceiveEnvironmentTemp(res.value(), takeReading.reader())
        );
        return this;
    }

    private Behavior<Command> onReceiveEnvironmentTemp(ReceiveEnvironmentTemp receiveEnvironmentTemp) {
        receiveEnvironmentTemp
                .forwardTo()
                .tell(new Reading(receiveEnvironmentTemp.val(), "Celsius"));
        return this;
    }

    @Override
    public String toString() {
        return "temperature sensor " + groupId + "-" + deviceId;
    }
}
