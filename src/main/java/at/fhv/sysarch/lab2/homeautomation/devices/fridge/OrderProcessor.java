package at.fhv.sysarch.lab2.homeautomation.devices.fridge;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

public class OrderProcessor extends AbstractBehavior<OrderProcessor.Command> {

    public interface Command { }
    public record Order(Fridge.Product product) implements Command { }

    public OrderProcessor(ActorContext<Command> context) {
        super(context);
    }

    public static Behavior<Command> create() {
        return Behaviors.setup(OrderProcessor::new);
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(Order.class, this::onOrder)
                .build();
    }

    public Behavior<Command> onOrder(Order order) {
        getContext().getLog().info("{} processing order for {}", this, order.product);
        return this;
    }

    @Override
    public String toString() {
        return "order processor";
    }
}
