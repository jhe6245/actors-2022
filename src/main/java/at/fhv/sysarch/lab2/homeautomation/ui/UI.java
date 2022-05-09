package at.fhv.sysarch.lab2.homeautomation.ui;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import at.fhv.sysarch.lab2.homeautomation.devices.AirCondition;
import at.fhv.sysarch.lab2.homeautomation.devices.Blinds;
import at.fhv.sysarch.lab2.homeautomation.devices.MediaStation;
import at.fhv.sysarch.lab2.homeautomation.devices.TemperatureSensor;
import at.fhv.sysarch.lab2.homeautomation.devices.fridge.Fridge;
import at.fhv.sysarch.lab2.homeautomation.environmental.AmbientTemperature;
import at.fhv.sysarch.lab2.homeautomation.environmental.Weather;
import at.fhv.sysarch.lab2.homeautomation.environmental.WeatherType;

import java.util.Arrays;
import java.util.Scanner;

public class UI extends AbstractBehavior<Void> {

    private final ActorRef<TemperatureSensor.Command> tempSensor;
    private final ActorRef<AirCondition.Command> airCondition;
    private final ActorRef<AmbientTemperature.Command> ambientTemp;
    private final ActorRef<MediaStation.Command> mediaStation;
    private final ActorRef<Weather.Command> weather;
    private final ActorRef<Blinds.Command> blinds;
    private final ActorRef<Fridge.Command> fridge;

    public static Behavior<Void> create(ActorRef<TemperatureSensor.Command> tempSensor, ActorRef<AirCondition.Command> airCondition, ActorRef<AmbientTemperature.Command> ambientTemp, ActorRef<MediaStation.Command> mediaStation, ActorRef<Weather.Command> weather, ActorRef<Blinds.Command> blinds, ActorRef<Fridge.Command> fridge) {
        return Behaviors.setup(context -> new UI(context, tempSensor, airCondition, ambientTemp, mediaStation, weather, blinds, fridge));
    }

    private UI(ActorContext<Void> context, ActorRef<TemperatureSensor.Command> tempSensor, ActorRef<AirCondition.Command> airCondition, ActorRef<AmbientTemperature.Command> ambientTemp, ActorRef<MediaStation.Command> mediaStation, ActorRef<Weather.Command> weather, ActorRef<Blinds.Command> blinds, ActorRef<Fridge.Command> fridge) {
        super(context);

        this.airCondition = airCondition;
        this.tempSensor = tempSensor;
        this.ambientTemp = ambientTemp;
        this.mediaStation = mediaStation;
        this.weather = weather;
        this.blinds = blinds;
        this.fridge = fridge;

        new Thread(this::runCommandLine).start();

        getContext().getLog().info("UI started");
    }

    @Override
    public Receive<Void> createReceive() {
        return newReceiveBuilder().onSignal(PostStop.class, signal -> onPostStop()).build();
    }

    private UI onPostStop() {
        getContext().getLog().info("UI stopped");
        return this;
    }

    public void runCommandLine() {
        Scanner scanner = new Scanner(System.in);
        String line;

        while (scanner.hasNextLine() && !(line = scanner.nextLine()).equalsIgnoreCase("exit")) {

            String[] command = line.split("\s+");

            if (command[0].equals("env")) {
                if(command[1].equals("temp")) {
                    this.ambientTemp.tell(new AmbientTemperature.SetTemp(Double.parseDouble(command[2])));
                }
                if(command[1].equals("weather")) {
                    this.weather.tell(new Weather.SetWeather(WeatherType.valueOf(command[2])));
                }
            }
            if (command[0].equals("ac")) {
                if (command[1].equals("on"))
                    this.airCondition.tell(new AirCondition.StartCooling());
                if (command[1].equals("off"))
                    this.airCondition.tell(new AirCondition.StopCooling());
            }
            if(command[0].equals("media")) {
                if(command[1].equals("play")) {
                    String movie = String.join(" ", Arrays.copyOfRange(command, 2, command.length));
                    this.mediaStation.tell(new MediaStation.MovieRequest(movie));
                }
            }

        }

        getContext().getLog().info("UI done");
    }
}
