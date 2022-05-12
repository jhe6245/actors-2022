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

import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.Scanner;

public class UI extends AbstractBehavior<UI.Command> {

    interface Command {}
    record FridgeContents(Map<Fridge.Product, Integer> contents) implements Command {}

    private final ActorRef<TemperatureSensor.Command> tempSensor;
    private final ActorRef<AirCondition.Command> airCondition;
    private final ActorRef<AmbientTemperature.Command> ambientTemp;
    private final ActorRef<MediaStation.Command> mediaStation;
    private final ActorRef<Weather.Command> weather;
    private final ActorRef<Blinds.Command> blinds;
    private final ActorRef<Fridge.Command> fridge;

    public static Behavior<Command> create(ActorRef<TemperatureSensor.Command> tempSensor, ActorRef<AirCondition.Command> airCondition, ActorRef<AmbientTemperature.Command> ambientTemp, ActorRef<MediaStation.Command> mediaStation, ActorRef<Weather.Command> weather, ActorRef<Blinds.Command> blinds, ActorRef<Fridge.Command> fridge) {
        return Behaviors.setup(context -> new UI(context, tempSensor, airCondition, ambientTemp, mediaStation, weather, blinds, fridge));
    }

    private UI(ActorContext<Command> context, ActorRef<TemperatureSensor.Command> tempSensor, ActorRef<AirCondition.Command> airCondition, ActorRef<AmbientTemperature.Command> ambientTemp, ActorRef<MediaStation.Command> mediaStation, ActorRef<Weather.Command> weather, ActorRef<Blinds.Command> blinds, ActorRef<Fridge.Command> fridge) {
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
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(FridgeContents.class, this::onFridgeContents)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<Command> onFridgeContents(FridgeContents fridgeContents) {
        System.out.println("Contents:");
        fridgeContents.contents.forEach((key, value) -> System.out.println(value + " " + key));
        return this;
    }

    private UI onPostStop() {
        getContext().getLog().info("UI stopped");
        return this;
    }

    public void runCommandLine() {
        Scanner scanner = new Scanner(System.in);
        String line;

        while (scanner.hasNextLine() && !(line = scanner.nextLine()).equalsIgnoreCase("exit")) {

            try {
                executeCommand(line);
            }
            catch (Throwable x) {
                x.printStackTrace();
            }
        }

        getContext().getLog().info("UI done");
    }

    private void executeCommand(String line) {
        String[] command = line.split("\s+");

        switch (command[0]) {
            case "env":
                if (command[1].equals("temp")) {
                    this.ambientTemp.tell(new AmbientTemperature.SetTemp(Double.parseDouble(command[2])));
                }
                if (command[1].equals("weather")) {
                    this.weather.tell(new Weather.SetWeather(WeatherType.valueOf(command[2])));
                }
                break;
            case "media":
                if (command[1].equals("play")) {
                    String movie = String.join(" ", Arrays.copyOfRange(command, 2, command.length));
                    this.mediaStation.tell(new MediaStation.MovieRequest(movie));
                }
                break;
            case "fridge":
                if (command.length == 1) {
                    getContext().ask(
                            Fridge.CurrentContentsResponse.class,
                            this.fridge,
                            Duration.ofMillis(100),
                            Fridge.CurrentContentsRequest::new,
                            (res, err) -> new FridgeContents(res.contents())
                    );
                } else if (command[1].equals("take")) {
                    String product = String.join(" ", Arrays.copyOfRange(command, 2, command.length));
                    this.fridge.tell(new Fridge.RemoveProduct(product));
                } else if (command[1].equals("order")) {
                    int amount = Integer.parseInt(command[2]);
                    String name = command[3];
                    double price = Double.parseDouble(command[4]);
                    double weight = Double.parseDouble(command[5]);
                    this.fridge.tell(new Fridge.RequestOrder(new Fridge.Product(name, price, weight), amount));
                }
                break;
            default:
                System.out.println("command not recognized");
                break;
        }
    }
}
