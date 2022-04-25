package at.fhv.sysarch.lab2.homeautomation;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import at.fhv.sysarch.lab2.homeautomation.devices.AirCondition;
import at.fhv.sysarch.lab2.homeautomation.devices.TemperatureSensor;
import at.fhv.sysarch.lab2.homeautomation.environmental.AmbientTemperature;
import at.fhv.sysarch.lab2.homeautomation.ui.UI;

import java.time.Duration;

public class HomeAutomationController extends AbstractBehavior<Void>{

    private ActorRef<TemperatureSensor.Command> tempSensor;
    private ActorRef<AirCondition.Command> airCondition;
    private ActorRef<AmbientTemperature.Command> ambientTemp;

    public static Behavior<Void> create() {
        return Behaviors.setup(HomeAutomationController::new);
    }

    private HomeAutomationController(ActorContext<Void> context) {
        super(context);

        // TODO: consider guardians and hierarchies. Who should create and communicate with which Actors?

        this.ambientTemp = getContext().spawn(AmbientTemperature.create(Duration.ofSeconds(10), 1), "AmbientTemperature");
        this.tempSensor = getContext().spawn(TemperatureSensor.create("1", "1", ambientTemp), "TemperatureSensor");
        this.airCondition = getContext().spawn(AirCondition.create("2", "1", tempSensor), "AirCondition");

        getContext().spawn(UI.create(this.tempSensor, this.airCondition, this.ambientTemp), "UI");
        getContext().getLog().info("HomeAutomation Application started");
    }

    @Override
    public Receive<Void> createReceive() {
        return newReceiveBuilder().onSignal(PostStop.class, signal -> onPostStop()).build();
    }

    private HomeAutomationController onPostStop() {
        getContext().getLog().info("HomeAutomation Application stopped");
        return this;
    }
}
