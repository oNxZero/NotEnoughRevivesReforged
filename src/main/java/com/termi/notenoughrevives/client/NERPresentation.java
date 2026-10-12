package com.termi.notenoughrevives.client;
import com.termi.notenoughrevives.config.NERConfig;
public final class NERPresentation {
    public static final java.util.Map<java.util.UUID, com.termi.notenoughrevives.network.payload.SyncKnockedPayload> states = new java.util.HashMap<>();
    public static final java.util.Map<java.util.UUID, Long> received = new java.util.HashMap<>();
    public static long downedImpactStarted;
    public static NERConfig config = new NERConfig();
    public static void reset() { config = new NERConfig(); states.clear(); received.clear(); downedImpactStarted = 0; }
}
