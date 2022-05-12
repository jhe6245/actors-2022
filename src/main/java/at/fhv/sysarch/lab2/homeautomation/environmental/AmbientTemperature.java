package at.fhv.sysarch.lab2.homeautomation.environmental;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;

import java.time.Duration;

public class AmbientTemperature extends AbstractBehavior<AmbientTemperature.Command> {

    public interface Command {}

    public enum Fluctuate implements Command { INST }
    public record SetTemp(double value) implements Command {}

    public record TempRequest(ActorRef<TempResponse> receiver) implements Command {}
    public record TempResponse(double value) implements Command {}

    private static final double maxChange = 1;

    private double currentTemp;

    private AmbientTemperature(ActorContext<Command> context, TimerScheduler<Command> timerScheduler) {
        super(context);

        currentTemp = 15;

        timerScheduler.startTimerAtFixedRate(Fluctuate.INST, Duration.ofSeconds(1));
    }

    public static Behavior<Command> create() {
        return Behaviors.setup(
                context -> Behaviors.withTimers(
                        timers -> new AmbientTemperature(context, timers)
                )
        );
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(Fluctuate.class, this::onFluctuate)
                .onMessage(SetTemp.class, this::onSetTemp)
                .onMessage(TempRequest.class, this::onTempRequest)
                .build();
    }

    private Behavior<Command> onFluctuate(Fluctuate fluctuate) {

        currentTemp += (Math.random() * 2 - 1) * maxChange;
        return this;
    }

    private Behavior<Command> onSetTemp(SetTemp setTemp) {

        this.currentTemp = setTemp.value();

        getContext().getLog().info("{} manually updated", currentTemp);

        return this;
    }

    private Behavior<Command> onTempRequest(TempRequest tempRequest) {
        tempRequest.receiver().tell(new TempResponse(this.currentTemp));
        return this;
    }
}
