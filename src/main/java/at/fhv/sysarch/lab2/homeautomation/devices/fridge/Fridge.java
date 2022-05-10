package at.fhv.sysarch.lab2.homeautomation.devices.fridge;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Fridge extends AbstractBehavior<Fridge.Command> {


    public interface Command {}
    public record CurrentContentsRequest(ActorRef<CurrentContentsResponse> receiver) implements Command { }
    public record CurrentContentsResponse(Map<Product, Integer> contents) implements Command { }
    public record RemoveProduct(String productName) implements Command { }
    public record RequestOrder(Product product, int amount) implements Command { }

    public record Product(String name, double price, double weight) { }


    private final Map<Product, Integer> contents = new HashMap<>();

    private final ActorRef<CounterSensor.Command> counter;
    private final ActorRef<WeightSensor.Command> weightSensor;

    public Fridge(ActorContext<Command> context, ActorRef<CounterSensor.Command> counter, ActorRef<WeightSensor.Command> weightSensor) {
        super(context);
        this.counter = counter;
        this.weightSensor = weightSensor;
    }

    public static Behavior<Command> create(ActorRef<CounterSensor.Command> counter, ActorRef<WeightSensor.Command> weightSensor) {
        return Behaviors.setup(context -> new Fridge(context, counter, weightSensor));
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(CurrentContentsRequest.class, this::onCurrentContentsRequest)
                .onMessage(RemoveProduct.class, this::onRemoveProduct)
                .onMessage(RequestOrder.class, this::onRequestOrder)
                .build();
    }

    private Behavior<Command> onCurrentContentsRequest(CurrentContentsRequest currentContentsRequest) {
        currentContentsRequest.receiver().tell(new CurrentContentsResponse(Map.copyOf(contents)));
        return this;
    }

    private Behavior<Command> onRemoveProduct(RemoveProduct removeProduct) {

        contents.keySet()
                .stream()
                .filter(p -> p.name.equals(removeProduct.productName))
                .findAny()
                .ifPresent(p -> {
                    int current = contents.get(p);

                    if(current <= 1)
                        contents.remove(p);
                    else
                        contents.put(p, current - 1);

                    getContext().getSelf().tell(new RequestOrder(p, 1));
                });


        return this;
    }

    private Behavior<Command> onRequestOrder(RequestOrder requestOrder) {

        var processor = getContext().spawn(
                OrderProcessor.create(getContext().getSelf(), counter, weightSensor),
                "OrderProcessor" + UUID.randomUUID());

        processor.tell(new OrderProcessor.ReceiveOrder(requestOrder.product, requestOrder.amount));

        return this;
    }


    @Override
    public String toString() {
        return "fridge";
    }
}
