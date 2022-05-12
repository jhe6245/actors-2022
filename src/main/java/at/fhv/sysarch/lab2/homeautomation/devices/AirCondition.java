package at.fhv.sysarch.lab2.homeautomation.devices;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.*;

import java.time.Duration;

public class AirCondition extends AbstractBehavior<AirCondition.Command> {

    public interface Command {}

    public record TemperatureMeasurement(double value, String unit) implements Command {}
    private enum InternalClockTick implements Command { INST }

    private final String groupId;
    private final String deviceId;

    private boolean isCooling;

    private final ActorRef<TemperatureSensor.Command> temperatureSensor;

    private static final double targetTemperature = 20;


    private AirCondition(ActorContext<Command> context, TimerScheduler<AirCondition.Command> timers, String groupId, String deviceId, ActorRef<TemperatureSensor.Command> temperatureSensor) {
        super(context);

        this.groupId = groupId;
        this.deviceId = deviceId;

        this.isCooling = false;

        this.temperatureSensor = temperatureSensor;

        timers.startTimerAtFixedRate(InternalClockTick.INST, Duration.ofSeconds(30));
    }

    public static Behavior<Command> create(String groupId, String deviceId, ActorRef<TemperatureSensor.Command> temperatureSensor) {
        return Behaviors.setup(context ->
                Behaviors.withTimers(timers ->
                        new AirCondition(context, timers, groupId, deviceId, temperatureSensor)
                )
        );
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(InternalClockTick.class, this::onInternalClockTick)
                .onMessage(TemperatureMeasurement.class, this::onTemperatureMeasurement)
                .onSignal(PostStop.class, this::onPostStop)
                .build();
    }

    private Behavior<Command> onInternalClockTick(InternalClockTick tick) {
        getContext().getLog().info("{} reading temperature sensor", this);
        getContext().ask(
                TemperatureSensor.Reading.class,
                temperatureSensor,
                Duration.ofMillis(100),
                TemperatureSensor.TakeReading::new,
                (res, err) -> new TemperatureMeasurement(res.value(), res.unit())
        );
        return this;
    }

    private Behavior<Command> onTemperatureMeasurement(TemperatureMeasurement r) {
        getContext().getLog().info("{} received temperature reading {} {}", this, r.value, r.unit);

        if(r.value >= targetTemperature) {
            if(!this.isCooling) {
                this.isCooling = true;
                getContext().getLog().info("{} turned on", this);
            }
        }
        else {
            if(this.isCooling) {
                this.isCooling = false;
                getContext().getLog().info("{} turned off", this);
            }
        }

        return this;
    }

    private AirCondition onPostStop(PostStop postStop) {
        getContext().getLog().info("{} actor stopped", this);
        return this;
    }

    @Override
    public String toString() {
        return "AC unit " + groupId + "-" + deviceId + " (currently " + (isCooling ? "cooling" : "not cooling") + ")";
    }
}
