package at.fhv.sysarch.lab2.homeautomation;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import at.fhv.sysarch.lab2.homeautomation.devices.*;
import at.fhv.sysarch.lab2.homeautomation.devices.fridge.CounterSensor;
import at.fhv.sysarch.lab2.homeautomation.devices.fridge.Fridge;
import at.fhv.sysarch.lab2.homeautomation.devices.fridge.OrderProcessor;
import at.fhv.sysarch.lab2.homeautomation.devices.fridge.WeightSensor;
import at.fhv.sysarch.lab2.homeautomation.environmental.AmbientTemperature;
import at.fhv.sysarch.lab2.homeautomation.environmental.Weather;
import at.fhv.sysarch.lab2.homeautomation.ui.UI;

import java.time.Duration;

public class HomeAutomationController extends AbstractBehavior<Void>{

    private final ActorRef<TemperatureSensor.Command> tempSensor;
    private final ActorRef<AirCondition.Command> airCondition;
    private final ActorRef<AmbientTemperature.Command> ambientTemp;
    private final ActorRef<MediaStation.Command> mediaStation;
    private final ActorRef<Weather.Command> weather;
    private final ActorRef<Blinds.Command> blinds;
    private final ActorRef<WeatherSensor.Command> weatherSensor;
    private final ActorRef<Fridge.Command> fridge;

    public static Behavior<Void> create() {
        return Behaviors.setup(HomeAutomationController::new);
    }

    private HomeAutomationController(ActorContext<Void> context) {
        super(context);

        this.ambientTemp = getContext().spawn(AmbientTemperature.create(), "AmbientTemperature");
        this.tempSensor = getContext().spawn(TemperatureSensor.create("1", "1", ambientTemp), "TemperatureSensor");
        this.airCondition = getContext().spawn(AirCondition.create("2", "1", tempSensor), "AirCondition");
        this.blinds = getContext().spawn(Blinds.create("4", "1"), "Blinds");
        this.mediaStation = getContext().spawn(MediaStation.create("3", "1", blinds), "MediaStation");
        this.weather = getContext().spawn(Weather.create(), "Weather");
        this.weatherSensor = getContext().spawn(WeatherSensor.create(weather, blinds), "WeatherSensor");

        var counter = getContext().spawn(CounterSensor.create(), "CounterSensor");
        var weightSensor = getContext().spawn(WeightSensor.create(), "WeightSensor");
        this.fridge = getContext().spawn(Fridge.create(counter, weightSensor), "Fridge");

        getContext().spawn(UI.create(this.tempSensor, this.airCondition, this.ambientTemp, this.mediaStation, this.weather, this.blinds, this.fridge), "UI");
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
