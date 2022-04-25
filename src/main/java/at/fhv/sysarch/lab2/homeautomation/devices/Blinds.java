package at.fhv.sysarch.lab2.homeautomation.devices;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

public class Blinds extends AbstractBehavior<Blinds.Command> {

    public interface Command {}

    public enum Open implements Command { INST }
    public enum Close implements Command { INST }

    public enum CloseUntilReleased implements Command { INST }
    public enum ReleaseAndOpen implements Command { INST }

    private final String groupId;
    private final String deviceId;

    private enum State {
        OPEN,
        CLOSED,
        CLOSED_UNTIL_RELEASED
    }

    private State state;

    private Blinds(ActorContext<Command> context, String groupId, String deviceId) {
        super(context);

        this.groupId = groupId;
        this.deviceId = deviceId;
        this.state = State.OPEN;
    }

    public static Behavior<Command> create(String groupId, String deviceId) {
        return Behaviors.setup(context -> new Blinds(context, groupId, deviceId));
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(Open.class, this::onOpen)
                .onMessage(Close.class, this::onClose)
                .onMessage(CloseUntilReleased.class, this::onCloseUntilReleased)
                .onMessage(ReleaseAndOpen.class, this::onReleaseAndOpen)
                .build();
    }

    private Behavior<Command> onOpen(Open open) {
        if(state != State.CLOSED_UNTIL_RELEASED && state != State.OPEN) {
            getContext().getLog().info("{} opening...", this);
            state = State.OPEN;
            getContext().getLog().info("{}", this);
        }
        return this;
    }

    private Behavior<Command> onClose(Close close) {
        if(state != State.CLOSED_UNTIL_RELEASED && state != State.CLOSED) {
            getContext().getLog().info("{} closing...", this);
            state = State.CLOSED;
            getContext().getLog().info("{}", this);
        }
        return this;
    }

    private Behavior<Command> onCloseUntilReleased(CloseUntilReleased closeUntilReleased) {
        getContext().getLog().info("{} closing until released...", this);
        this.state = State.CLOSED_UNTIL_RELEASED;
        getContext().getLog().info("{}", this);
        return this;
    }

    private Behavior<Command> onReleaseAndOpen(ReleaseAndOpen releaseAndOpen) {
        getContext().getLog().info("{} releasing...", this);
        this.state = State.OPEN;
        getContext().getLog().info("{}", this);
        return this;
    }

    @Override
    public String toString() {
        return "blinds " + groupId + "-" + deviceId + " (currently " + state + ")";
    }
}
