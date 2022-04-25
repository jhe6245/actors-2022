package at.fhv.sysarch.lab2.homeautomation.devices;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

public class MediaStation extends AbstractBehavior<MediaStation.Command> {

    public interface Command {}

    public record MovieRequest(String movieName) implements Command {}

    private final String groupId;
    private final String deviceId;

    private final Map<String, Duration> movies;

    private String latestMovie;
    private LocalDateTime playingMovieUntil;

    private MediaStation(ActorContext<Command> context, String groupId, String deviceId) {
        super(context);

        this.groupId = groupId;
        this.deviceId = deviceId;

        this.movies = Map.of(
                "sharknado", Duration.ofHours(1).plusMinutes(30)
        );
        this.playingMovieUntil = LocalDateTime.MIN;
    }

    public static Behavior<Command> create(String groupId, String deviceId) {
        return Behaviors.setup(context -> new MediaStation(context, groupId, deviceId));
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(MovieRequest.class, this::onMovieRequest)
                .build();
    }

    private Behavior<Command> onMovieRequest(MovieRequest movieRequest) {

        String requested = movieRequest.movieName;

        Duration requestedMovieDuration = this.movies.get(requested.toLowerCase());

        LocalDateTime now = LocalDateTime.now();

        if(requestedMovieDuration == null) {

            getContext().getLog().info("{} cannot play {}, unknown movie", this, requested);

        } else if(this.playingMovieUntil.isAfter(now)) {

            getContext().getLog().info("{} cannot play {}, still playing {} ({} remaining)",
                    this, requested, this.latestMovie, Duration.between(now, this.playingMovieUntil));

        } else {

            this.latestMovie = requested;
            this.playingMovieUntil = now.plus(requestedMovieDuration);

            getContext().getLog().info("{} now playing {} ({})", this, requested, requestedMovieDuration);
        }

        return this;
    }

    @Override
    public String toString() {
        return "media station " + groupId + "-" + deviceId;
    }
}
