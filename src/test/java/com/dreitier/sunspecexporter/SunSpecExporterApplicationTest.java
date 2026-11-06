package com.dreitier.sunspecexporter;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

import com.dreitier.sunspecexporter.sunspec.SunSpecSimulator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Starts the exporter against a simulated inverter and an unreachable device and scrapes {@code /actuator/prometheus}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SunSpecExporterApplicationTest {
    private static final SunSpecSimulator SIMULATOR;

    static {
        try {
            SIMULATOR = SunSpecSimulator.start(SunSpecSimulator.threePhaseInverter());
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("modbus.poll-interval", () -> "200ms");
        registry.add("modbus.timeout", () -> "500ms");
        registry.add("modbus.devices[0].host", () -> "127.0.0.1");
        registry.add("modbus.devices[0].port", SIMULATOR::port);
        // nothing listens on port 1
        registry.add("modbus.devices[1].host", () -> "localhost");
        registry.add("modbus.devices[1].port", () -> 1);
    }

    @AfterAll
    static void stopSimulator() {
        SIMULATOR.close();
    }

    @LocalServerPort
    int port;

    private final HttpClient http = HttpClient.newHttpClient();

    private HttpResponse<String> get(String path) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void exposesTheMetricsOfTheConfiguredDevices() throws Exception {
        String metrics = "";
        Instant deadline = Instant.now().plus(Duration.ofSeconds(15));
        while (Instant.now().isBefore(deadline)) {
            metrics = get("/actuator/prometheus").body();
            if (metrics.contains("sunspec_ac_power_watts")) {
                break;
            }
            Thread.sleep(100);
        }

        String simulator = "host=\"127.0.0.1\"";
        assertThat(metrics).contains(
                "sunspec_up{" + simulator + "} 1.0",
                "sunspec_ac_power_watts{" + simulator + "} 2840.0",
                "sunspec_ac_voltage_volts{" + simulator + ",phase=\"an\"} 230.1",
                "sunspec_ac_energy_watthours_total{" + simulator + "} 1234560.0",
                "sunspec_operating_state{" + simulator + ",sunspec_operating_state=\"MPPT\"} 1",
                "sunspec_mppt_power_watts{" + simulator + ",input=\"1\"} 1501.0",
                "serial=\"12345678\"",
                // Micrometer's own metrics are still there
                "jvm_memory_used_bytes");
        assertThat(metrics).contains("sunspec_up{host=\"localhost\"} 0.0");

        // CI lints the exposed metrics with `promtool check metrics`
        Files.writeString(Path.of("target", "prometheus-scrape.txt"), metrics);
    }

    @Test
    void healthProbes() throws Exception {
        assertThat(get("/actuator/health/liveness").statusCode()).isEqualTo(200);
        assertThat(get("/actuator/health/readiness").statusCode()).isEqualTo(200);
    }

    @Test
    void infoContainsTheBuildVersion() throws Exception {
        assertThat(get("/actuator/info").body()).contains("\"build\"", "\"version\"");
    }
}
