package com.example.demo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
public class Bean {
    private String id;
    private String clazz;
    //talvez criar elemento Property
    private Map<String, String> properties = new HashMap<>();


}