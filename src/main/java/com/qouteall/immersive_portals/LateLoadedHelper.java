package com.qouteall.immersive_portals;

import java.util.function.Function;

public class LateLoadedHelper {
    //to avoid interfering with class loading sequence
    public static Function<Object, String> dimensionTypeToString;
    
}
