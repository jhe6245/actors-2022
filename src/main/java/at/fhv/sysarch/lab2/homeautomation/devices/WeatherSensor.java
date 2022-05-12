package at.fhv.sysarch.lab2.homeautomation.devices;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;
import at.fhv.sysarch.lab2.homeautomation.environmental.Weather;
import at.fhv.sysarch.lab2.homeautomation.environmental.WeatherType;

import java.time.Duration;

public class WeatherSensor extends AbstractBehavior<WeatherSensor.Command> {

    public interface Command {}

    private enum Measure implements Command { INST }
    private record ReceiveWeather(WeatherType weather) implements Command {}

    private final ActorRef<Weather.Command> weather;
    private final ActorRef<Blinds.Command> blinds;

    private WeatherSensor(ActorContext<Command> context, TimerScheduler<Command> timers, ActorRef<Weather.Command> weather, ActorRef<Blinds.Command> blinds) {
        super(context);

        timers.startTimerAtFixedRate(Measure.INST, Duration.ofSeconds(3));

        this.weather = weather;
        this.blinds = blinds;
    }

    public static Behavior<Command> create(ActorRef<Weather.Command> weather, ActorRef<Blinds.Command> blinds) {
        return Behaviors.setup(context ->
                Behaviors.withTimers(timers ->
                        new WeatherSensor(context, timers, weather, blinds)
                )
        );
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(Measure.class, this::onMeasure)
                .onMessage(ReceiveWeather.class, this::onReceiveWeather)
                .build();
    }

    private Behavior<Command> onMeasure(Measure measure) {
        getContext().ask(
                Weather.WeatherResponse.class,
                weather,
                Duration.ofMillis(100),
                Weather.WeatherRequest::new,
                (res, err) -> new ReceiveWeather(res.weather())
        );
        return this;
    }

    private Behavior<Command> onReceiveWeather(ReceiveWeather receiveWeather) {
        this.blinds.tell(new Blinds.WeatherChanged(receiveWeather.weather));
        return this;
    }
}
