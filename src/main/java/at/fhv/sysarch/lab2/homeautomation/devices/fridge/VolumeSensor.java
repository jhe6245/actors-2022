package at.fhv.sysarch.lab2.homeautomation.devices.fridge;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Receive;

import java.time.Duration;

public class VolumeSensor extends AbstractBehavior<VolumeSensor.Command> {

    public interface Command { }
    public record VolumeRequest(ActorRef<VolumeResponse> receiver) implements Command { }
    public record VolumeResponse(int amount) implements Command { }

    private record Forward(VolumeRequest req, Fridge.CurrentContentsResponse res) implements Command { }

    private final ActorRef<Fridge.Command> fridge;

    public VolumeSensor(ActorContext<Command> context, ActorRef<Fridge.Command> fridge) {
        super(context);
        this.fridge = fridge;
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(VolumeRequest.class, this::onVolumeRequest)
                .onMessage(Forward.class, this::onForward)
                .build();
    }

    public Behavior<Command> onVolumeRequest(VolumeRequest volumeRequest) {
        getContext().ask(
                Fridge.CurrentContentsResponse.class,
                fridge,
                Duration.ofMillis(100),
                Fridge.CurrentContentsRequest::new,
                (res, err) -> new Forward(volumeRequest, res)
        );
        return this;
    }

    public Behavior<Command> onForward(Forward forward) {
        forward.req.receiver.tell(
                new VolumeResponse(
                        forward.res.contents().values().stream().mapToInt(Integer::intValue).sum()
                )
        );
        return this;
    }
}
