package at.fhv.sysarch.lab2.homeautomation.environmental;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;

import java.time.Duration;
import java.util.Random;

public class Weather extends AbstractBehavior<Weather.Command> {

    public interface Command {}

    public record SetWeather(WeatherType weather) implements Command {}

    public record WeatherRequest(ActorRef<WeatherResponse> receiver) implements Command {}
    public record WeatherResponse(WeatherType weather) implements Command {}

    private enum Fluctuate implements Command { INST }

    private WeatherType currentWeatherType;

    private Weather(ActorContext<Command> context, TimerScheduler<Command> timers) {
        super(context);

        this.currentWeatherType = WeatherType.SUNNY;

        timers.startTimerAtFixedRate(Fluctuate.INST, Duration.ofMinutes(1));
    }

    public static Behavior<Command> create() {
        return Behaviors.setup(context -> Behaviors.withTimers(timers -> new Weather(context, timers)));
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(SetWeather.class, this::onSetWeather)
                .onMessage(WeatherRequest.class, this::onWeatherRequest)
                .onMessage(Fluctuate.class, this::onFluctuate)
                .build();
    }

    private Behavior<Command> onSetWeather(SetWeather setWeather) {

        this.currentWeatherType = setWeather.weather;

        getContext().getLog().info("{} manually set", this);
        return this;
    }

    private Behavior<Command> onWeatherRequest(WeatherRequest weatherRequest) {
        weatherRequest.receiver.tell(new WeatherResponse(currentWeatherType));
        return this;
    }

    private Behavior<Command> onFluctuate(Fluctuate fluctuate) {

        WeatherType[] values = WeatherType.values();
        this.currentWeatherType = values[new Random().nextInt(values.length)];

        getContext().getLog().info("{} fluctuated", this);

        return this;
    }

    @Override
    public String toString() {
        return "weather (" + currentWeatherType + ")";
    }
}
