package org.example.volonterhoursapp.controller;

import java.util.function.Consumer;

public interface StatusAware {
    void setStatusConsumer(Consumer<String> status);
}
