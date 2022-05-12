package at.fhv.sysarch.lab2.homeautomation.devices;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

public class MediaStation extends AbstractBehavior<MediaStation.Command> {

    public interface Command {}

    public record MovieRequest(String movieName) implements Command {}
    private enum MovieOver implements Command { INST }

    private final TimerScheduler<Command> timers;

    private final String groupId;
    private final String deviceId;

    private final Map<String, Duration> movies;

    private String currentMovie;
    private LocalDateTime movieEndTime;

    private final ActorRef<Blinds.Command> blinds;

    private MediaStation(ActorContext<Command> context, TimerScheduler<Command> timers, String groupId, String deviceId, ActorRef<Blinds.Command> blinds) {
        super(context);

        this.timers = timers;

        this.groupId = groupId;
        this.deviceId = deviceId;

        // titles must be lower case for case insensitive matching
        this.movies = Map.of(
                "test", Duration.ofSeconds(10),
                "water world trailer", Duration.ofSeconds(30),
                "sharknado", Duration.ofHours(1).plusMinutes(30)
        );
        this.movieEndTime = LocalDateTime.MIN;

        this.blinds = blinds;
    }

    public static Behavior<Command> create(String groupId, String deviceId, ActorRef<Blinds.Command> blinds) {
        return Behaviors.setup(context -> Behaviors.withTimers(timers -> new MediaStation(context, timers, groupId, deviceId, blinds)));
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(MovieRequest.class, this::onMovieRequest)
                .onMessage(MovieOver.class, this::onMovieOver)
                .build();
    }

    private Behavior<Command> onMovieRequest(MovieRequest movieRequest) {

        String requested = movieRequest.movieName;

        Duration requestedMovieDuration = this.movies.get(requested.toLowerCase());

        LocalDateTime now = LocalDateTime.now();

        if(requestedMovieDuration == null) {

            getContext().getLog().info("cannot play {}, unknown movie", requested);

        } else if(this.movieEndTime.isAfter(now)) {

            getContext().getLog().info("cannot play {}, still playing {} ({} remaining)",
                    requested, this.currentMovie, Duration.between(now, this.movieEndTime));

        } else {

            this.currentMovie = requested;
            this.movieEndTime = now.plus(requestedMovieDuration);

            this.timers.cancel(this);
            this.timers.startSingleTimer(this, MovieOver.INST, requestedMovieDuration);

            getContext().getLog().info("now playing {} ({})", requested, requestedMovieDuration);

            this.blinds.tell(Blinds.MovieStarted.INST);
        }

        return this;
    }

    private Behavior<Command> onMovieOver(MovieOver movieOver) {
        getContext().getLog().info("finished playing {}", this.currentMovie);
        this.currentMovie = null;
        this.blinds.tell(Blinds.MovieEnded.INST);
        return this;
    }
}
