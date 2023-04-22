package ru.jamsys.broker;

import lombok.Data;

@Data
public class BrokerQueueStatistic {

    String name;
    int tpsInput;
    int tpsOutput;
    int size;
    int timeAvg;

    public BrokerQueueStatistic(String name, int tpsInput, int tpsOutput, int size, int timeAvg) {
        this.name = name;
        this.tpsInput = tpsInput;
        this.tpsOutput = tpsOutput;
        this.size = size;
        this.timeAvg = timeAvg;
    }

}
