package at.fhv.sysarch.lab2.homeautomation.environmental;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Receive;
import akka.actor.typed.javadsl.TimerScheduler;

import java.time.Duration;
import java.util.Random;

public class Weather extends AbstractBehavior<Weather.Command> {

    public interface Command {}

    public record WeatherRequest(ActorRef<WeatherResponse> receiver) implements Command {}
    public record WeatherResponse(WeatherType weather) implements Command {}

    private enum ChangeRandomly implements Command { INST }

    private WeatherType currentWeatherType;

    public Weather(ActorContext<Command> context, TimerScheduler<Command> timers) {
        super(context);

        this.currentWeatherType = WeatherType.SUNNY;

        timers.startTimerAtFixedRate(ChangeRandomly.INST, Duration.ofMinutes(1));
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(WeatherRequest.class, this::onWeatherRequest)
                .onMessage(ChangeRandomly.class, this::onChangeRandomly)
                .build();
    }

    private Behavior<Command> onWeatherRequest(WeatherRequest weatherRequest) {
        weatherRequest.receiver.tell(new WeatherResponse(currentWeatherType));
        return this;
    }

    private Behavior<Command> onChangeRandomly(ChangeRandomly changeRandomly) {

        WeatherType[] values = WeatherType.values();
        this.currentWeatherType = values[new Random().nextInt(values.length)];

        getContext().getLog().info("{} changed randomly", this);

        return this;
    }

    @Override
    public String toString() {
        return "weather (" + currentWeatherType + ")";
    }
}
