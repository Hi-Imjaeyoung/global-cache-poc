package org.example.listener;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class PrefixBuildEvent {
    private final String email;
    private final int year;
}
