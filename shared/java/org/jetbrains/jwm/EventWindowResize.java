package org.jetbrains.jwm;

import lombok.Data;

@Data
public class EventWindowResize implements Event {
    public final int _width;
    public final int _height;
}