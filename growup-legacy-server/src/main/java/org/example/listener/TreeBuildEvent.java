package org.example.listener;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TreeBuildEvent {
    private final String email;
    private final int year;
}
