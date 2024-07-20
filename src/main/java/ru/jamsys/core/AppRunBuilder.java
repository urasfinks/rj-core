package ru.jamsys.core;

import org.springframework.boot.SpringApplication;

import java.util.ArrayList;
import java.util.List;

public class AppRunBuilder {

    private final List<String> arguments = new ArrayList<>();

    public AppRunBuilder addTestArguments() {
        return this
                .add("run.args.remote.log", false)
                .add("run.args.remote.statistic", false)
                .add("run.args.web", false)
                .add("spring.main.web-application-type", "none");
    }

    public AppRunBuilder add(String key, Object value) {
        arguments.add("--" + key + "=" + value);
        return this;
    }

    public void runCore() {
        App.run(arguments.toArray(new String[0]));
    }

    public void runSpring() {
        App.context = SpringApplication.run(App.class, arguments.toArray(new String[0]));
    }

}
