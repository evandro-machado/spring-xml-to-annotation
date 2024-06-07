package com.example.demo;

import lombok.NoArgsConstructor;
import lombok.Value;

@Value
@NoArgsConstructor(force = true)
public class Property {
    String name;
    String value;
    String ref;
    Bean bean;
}
