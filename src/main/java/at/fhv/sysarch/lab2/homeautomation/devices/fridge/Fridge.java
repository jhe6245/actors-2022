package at.fhv.sysarch.lab2.homeautomation.devices.fridge;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class Fridge extends AbstractBehavior<Fridge.Command> {

    public interface Command {}
    public record CurrentContentsRequest(ActorRef<CurrentContentsResponse> receiver) implements Command { }
    public record CurrentContentsResponse(Map<Product, Integer> contents) implements Command { }
    public record RemoveProduct(Product product) implements Command { }
    public record OrderProduct(Product product) implements Command { }

    private static class SensorReadings implements Command {
        public int numberOfItems = -1;
        public double load = -1;

        public final Product productToOrder;

        public SensorReadings(Product productToOrder) {
            this.productToOrder = productToOrder;
        }

        public boolean isPopulated() {
            return numberOfItems != -1 && load != -1;
        }
    }

    public record Product(String name, double price, double weight) { }

    private final int maxNumberOfItems = 30;
    private final double maxLoad = 100;

    private final Map<Product, Integer> contents = new HashMap<>();

    private final ActorRef<VolumeSensor.Command> volumeSensor;

    public Fridge(ActorContext<Command> context, ActorRef<VolumeSensor.Command> volumeSensor) {
        super(context);
        this.volumeSensor = volumeSensor;
    }

    public static Behavior<Command> create(ActorRef<VolumeSensor.Command> volumeSensor) {
        return Behaviors.setup(context -> new Fridge(context, volumeSensor));
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(CurrentContentsRequest.class, this::onCurrentContentsRequest)
                .onMessage(RemoveProduct.class, this::onRemoveProduct)
                .onMessage(OrderProduct.class, this::onOrderProduct)
                .onMessage(SensorReadings.class, this::onSensorReadings)
                .build();
    }

    private Behavior<Command> onCurrentContentsRequest(CurrentContentsRequest currentContentsRequest) {
        currentContentsRequest.receiver().tell(new CurrentContentsResponse(Map.copyOf(contents)));
        return this;
    }

    private Behavior<Command> onRemoveProduct(RemoveProduct removeProduct) {

        var p = removeProduct.product();

        int current = contents.get(p);

        if(current <= 1)
            contents.remove(p);
        else
            contents.put(p, current - 1);

        return this;
    }

    private Behavior<Command> onOrderProduct(OrderProduct orderProduct) {

        var readings = new SensorReadings(orderProduct.product);

        getContext().ask(
                VolumeSensor.VolumeResponse.class,
                volumeSensor,
                Duration.ofMillis(100),
                VolumeSensor.VolumeRequest::new,
                (res, err) -> { readings.numberOfItems = res.amount(); return readings; }
        );

        return this;
    }

    private Behavior<Command> onSensorReadings(SensorReadings sensorReadings) {

        if(sensorReadings.isPopulated()) {
            if(sensorReadings.numberOfItems < maxNumberOfItems
                    && sensorReadings.load + sensorReadings.productToOrder.weight <= maxLoad) {

                getContext().getLog().info("{} ordering {}", this, sensorReadings.productToOrder);
            }
            getContext().getLog().info("{} cannot order {}, already full", this, sensorReadings.productToOrder);
        }

        return this;
    }


    @Override
    public String toString() {
        return "Fridge";
    }
}
