package at.fhv.sysarch.lab2.homeautomation.devices;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import at.fhv.sysarch.lab2.homeautomation.environmental.WeatherType;

public class Blinds extends AbstractBehavior<Blinds.Command> {

    public interface Command {}

    public enum MovieStarted implements Command { INST }
    public enum MovieEnded implements Command { INST }

    public record WeatherChanged(WeatherType weatherType) implements Command {}

    private final String groupId;
    private final String deviceId;

    private Blinds(ActorContext<Command> context, String groupId, String deviceId) {
        super(context);

        this.groupId = groupId;
        this.deviceId = deviceId;
    }

    public static Behavior<Command> create(String groupId, String deviceId) {
        return Behaviors.setup(context -> new Blinds(context, groupId, deviceId));
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(MovieStarted.class, this::onMovieStarted)
                .onMessage(MovieEnded.class, this::onMovieEnded)
                .onMessage(WeatherChanged.class, this::onWeatherChanged)
                .build();
    }

    private boolean blindsClosed;

    private boolean moviePlaying;
    private boolean sunny;

    private Behavior<Command> onMovieStarted(MovieStarted movieStarted) {
        moviePlaying = true;

        ensureBlindsClosed();

        return this;
    }

    private Behavior<Command> onMovieEnded(MovieEnded movieEnded) {
        moviePlaying = false;

        if(!sunny)
            ensureBlindsOpen();

        return this;
    }

    private Behavior<Command> onWeatherChanged(WeatherChanged weatherChanged) {
        sunny = weatherChanged.weatherType.equals(WeatherType.SUNNY);

        if(sunny)
            ensureBlindsClosed();
        else if(!moviePlaying)
            ensureBlindsOpen();

        return this;
    }

    private void ensureBlindsClosed() {
        if(blindsClosed)
            return;

        blindsClosed = true;
        getContext().getLog().info("closed");
    }

    private void ensureBlindsOpen() {
        if(!blindsClosed)
            return;

        blindsClosed = false;
        getContext().getLog().info("opened");
    }
}
