package at.fhv.sysarch.lab2.homeautomation.ui;

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

import java.util.Scanner;

public class UI extends AbstractBehavior<Void> {

    private final ActorRef<TemperatureSensor.Command> tempSensor;
    private final ActorRef<AirCondition.Command> airCondition;
    private final ActorRef<AmbientTemperature.Command> ambientTemp;

    public static Behavior<Void> create(ActorRef<TemperatureSensor.Command> tempSensor, ActorRef<AirCondition.Command> airCondition, ActorRef<AmbientTemperature.Command> ambientTemp) {
        return Behaviors.setup(context -> new UI(context, tempSensor, airCondition, ambientTemp));
    }

    private UI(ActorContext<Void> context, ActorRef<TemperatureSensor.Command> tempSensor, ActorRef<AirCondition.Command> airCondition, ActorRef<AmbientTemperature.Command> ambientTemp) {
        super(context);

        this.airCondition = airCondition;
        this.tempSensor = tempSensor;
        this.ambientTemp = ambientTemp;

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
            }
            if (command[0].equals("ac")) {
                if (command[1].equals("on"))
                    this.airCondition.tell(new AirCondition.StartCooling());
                if (command[1].equals("off"))
                    this.airCondition.tell(new AirCondition.StopCooling());
            }

        }

        getContext().getLog().info("UI done");
    }
}
