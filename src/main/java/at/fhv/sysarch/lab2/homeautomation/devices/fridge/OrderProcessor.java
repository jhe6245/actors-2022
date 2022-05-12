package at.fhv.sysarch.lab2.homeautomation.devices.fridge;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.time.Duration;
import java.util.Map;

public class OrderProcessor extends AbstractBehavior<OrderProcessor.Command> {

    public interface Command { }
    public record ReceiveOrder(Fridge.Product product, int amount) implements Command { }
    private record ReceiveContents(Map<Fridge.Product, Integer> contents) implements Command { }
    private record ReceiveCount(int remainingCount) implements Command {}
    private record ReceiveWeight(double remainingWeight) implements Command {}

    private final ActorRef<Fridge.Command> fridge;
    private final ActorRef<CounterSensor.Command> counter;
    private final ActorRef<WeightSensor.Command> weightSensor;

    private ReceiveOrder receiveOrder = null;
    private ReceiveCount receiveCount = null;
    private ReceiveWeight receiveWeight = null;

    public OrderProcessor(ActorContext<Command> context, ActorRef<Fridge.Command> fridge, ActorRef<CounterSensor.Command> counter, ActorRef<WeightSensor.Command> weightSensor) {
        super(context);
        this.fridge = fridge;
        this.counter = counter;
        this.weightSensor = weightSensor;
    }

    public static Behavior<Command> create(ActorRef<Fridge.Command> fridge, ActorRef<CounterSensor.Command> counter, ActorRef<WeightSensor.Command> weightSensor) {
        return Behaviors.setup(c -> new OrderProcessor(c, fridge, counter, weightSensor));
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(ReceiveOrder.class, this::onReceiveOrder)
                .onMessage(ReceiveContents.class, this::onReceiveContents)
                .onMessage(ReceiveWeight.class, this::onReceiveWeight)
                .onMessage(ReceiveCount.class, this::onReceiveCount)
                .build();
    }

    private Behavior<Command> onReceiveOrder(ReceiveOrder receiveOrder) {

        this.receiveOrder = receiveOrder;

        getContext().ask(
                Fridge.CurrentContentsResponse.class,
                fridge,
                Duration.ofMillis(100),
                Fridge.CurrentContentsRequest::new,
                (res, err) -> new ReceiveContents(res.contents())
        );
        return this;
    }

    private Behavior<Command> onReceiveContents(ReceiveContents receiveContents) {

        var contents = receiveContents.contents;

        getContext().ask(
                CounterSensor.Measurement.class,
                counter,
                Duration.ofMillis(100),
                r -> new CounterSensor.MeasurementRequest(r, contents),
                (res, err) -> new ReceiveCount(res.remainingAmount())
        );

        getContext().ask(
                WeightSensor.Measurement.class,
                weightSensor,
                Duration.ofMillis(100),
                r -> new WeightSensor.MeasurementRequest(r, contents),
                (res, err) -> new ReceiveWeight(res.remainingWeight())
        );

        return this;
    }


    private Behavior<Command> onReceiveCount(ReceiveCount receiveCount) {
        this.receiveCount = receiveCount;
        return checkAndFinish();
    }

    private Behavior<Command> onReceiveWeight(ReceiveWeight receiveWeight) {
        this.receiveWeight = receiveWeight;
        return checkAndFinish();
    }

    private Behavior<Command> checkAndFinish() {
        var rc = receiveCount;
        var rw = receiveWeight;

        if(rc == null || rw == null)
            return this;

        var amount = receiveOrder.amount;
        var product = receiveOrder.product;

        if(rc.remainingCount >= amount && rw.remainingWeight >= product.weight()) {
            getContext().getLog().info("confirming order. your total: €{}", amount * product.price());
            fridge.tell(new Fridge.AcceptDelivery(product, amount));
        }
        else {
            getContext().getLog().info("order not possible");
        }
        return Behaviors.stopped();
    }
}
