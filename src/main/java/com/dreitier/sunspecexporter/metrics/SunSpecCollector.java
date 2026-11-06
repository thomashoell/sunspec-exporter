package com.dreitier.sunspecexporter.metrics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.dreitier.sunspecexporter.sunspec.DeviceInfo;
import com.dreitier.sunspecexporter.sunspec.DeviceReading;
import com.dreitier.sunspecexporter.sunspec.DeviceState;
import com.dreitier.sunspecexporter.sunspec.InverterValue;
import com.dreitier.sunspecexporter.sunspec.MpptInput;
import com.dreitier.sunspecexporter.sunspec.OperatingState;
import com.dreitier.sunspecexporter.sunspec.SunSpecDevice;
import io.prometheus.metrics.model.registry.MultiCollector;
import io.prometheus.metrics.model.snapshots.CounterSnapshot;
import io.prometheus.metrics.model.snapshots.CounterSnapshot.CounterDataPointSnapshot;
import io.prometheus.metrics.model.snapshots.GaugeSnapshot;
import io.prometheus.metrics.model.snapshots.GaugeSnapshot.GaugeDataPointSnapshot;
import io.prometheus.metrics.model.snapshots.InfoSnapshot;
import io.prometheus.metrics.model.snapshots.InfoSnapshot.InfoDataPointSnapshot;
import io.prometheus.metrics.model.snapshots.Labels;
import io.prometheus.metrics.model.snapshots.MetricSnapshot;
import io.prometheus.metrics.model.snapshots.MetricSnapshots;
import io.prometheus.metrics.model.snapshots.StateSetSnapshot;
import io.prometheus.metrics.model.snapshots.StateSetSnapshot.StateSetDataPointSnapshot;

/**
 * Exposes the latest state of all devices to Prometheus.
 * <p>
 * The metrics are built from the latest poll on every scrape, so values the device doesn't implement are simply
 * missing, and series of unreachable devices disappear (except {@code sunspec_up}, {@code sunspec_info} and the
 * poll metrics) instead of reporting stale values.
 */
public class SunSpecCollector implements MultiCollector {
    static final String LABEL_HOST = "host";
    static final String LABEL_INPUT = "input";

    /** All operating states of the SunSpec inverter models, exposed as state set. */
    static final List<String> OPERATING_STATES = Arrays.stream(OperatingState.values()).map(Enum::name).toList();

    private final List<SunSpecDevice> devices;

    public SunSpecCollector(List<SunSpecDevice> devices) {
        this.devices = devices;
    }

    @Override
    public MetricSnapshots collect() {
        var up = GaugeSnapshot.builder().name("sunspec_up")
                .help("Whether the latest poll of the SunSpec device succeeded (1) or not (0)");
        var pollDuration = GaugeSnapshot.builder().name("sunspec_poll_duration_seconds")
                .help("Duration of the latest poll of the SunSpec device");
        var lastSuccess = GaugeSnapshot.builder().name("sunspec_last_poll_success_timestamp_seconds")
                .help("Unix time of the latest successful poll of the SunSpec device");
        var info = InfoSnapshot.builder().name("sunspec")
                .help("Information about the SunSpec device from its common model");
        var operatingState = StateSetSnapshot.builder().name("sunspec_operating_state")
                .help("Operating state of the inverter");
        Map<InverterValue.Metric, List<Point>> inverter = new EnumMap<>(InverterValue.Metric.class);
        Map<MpptMetric, List<Point>> mppt = new EnumMap<>(MpptMetric.class);
        int infoPoints = 0;
        int statePoints = 0;

        for (SunSpecDevice device : devices) {
            DeviceState state = device.state();
            Labels host = Labels.of(LABEL_HOST, device.host());

            up.dataPoint(GaugeDataPointSnapshot.builder().labels(host).value(state.up() ? 1 : 0).build());
            if (state.pollDuration() != null) {
                pollDuration.dataPoint(GaugeDataPointSnapshot.builder().labels(host)
                        .value(state.pollDuration().toNanos() / 1e9).build());
            }
            if (state.lastSuccess() != null) {
                lastSuccess.dataPoint(GaugeDataPointSnapshot.builder().labels(host)
                        .value(state.lastSuccess().toEpochMilli() / 1e3).build());
            }
            if (state.info() != null) {
                info.dataPoint(InfoDataPointSnapshot.builder().labels(infoLabels(device.host(), state.info()))
                        .build());
                infoPoints++;
            }

            DeviceReading reading = state.reading();
            if (reading == null) {
                continue;
            }
            if (reading.operatingState() != null) {
                var point = StateSetDataPointSnapshot.builder().labels(host);
                for (String s : OPERATING_STATES) {
                    point.state(s, s.equals(reading.operatingState()));
                }
                operatingState.dataPoint(point.build());
                statePoints++;
            }
            reading.inverter().forEach((value, v) -> inverter
                    .computeIfAbsent(value.metric(), k -> new ArrayList<>())
                    .add(new Point(value.labelName() == null ? host
                            : host.add(value.labelName(), value.labelValue()), v)));
            for (MpptInput input : reading.mpptInputs()) {
                Labels labels = host.add(LABEL_INPUT, Integer.toString(input.index()));
                for (MpptMetric metric : MpptMetric.values()) {
                    Double v = metric.value.apply(input);
                    if (v != null) {
                        mppt.computeIfAbsent(metric, k -> new ArrayList<>()).add(new Point(labels, v));
                    }
                }
            }
        }

        var snapshots = MetricSnapshots.builder()
                .metricSnapshot(up.build())
                .metricSnapshot(pollDuration.build())
                .metricSnapshot(lastSuccess.build());
        if (infoPoints > 0) {
            snapshots.metricSnapshot(info.build());
        }
        if (statePoints > 0) {
            snapshots.metricSnapshot(operatingState.build());
        }
        inverter.forEach((metric, points) ->
                snapshots.metricSnapshot(snapshot(metric.metricName(), metric.help(), metric.counter(), points)));
        mppt.forEach((metric, points) ->
                snapshots.metricSnapshot(snapshot(metric.metricName, metric.help, metric.counter, points)));
        return snapshots.build();
    }

    private static Labels infoLabels(String host, DeviceInfo info) {
        var labels = new LinkedHashMap<String, String>();
        labels.put(LABEL_HOST, host);
        labels.put("manufacturer", info.manufacturer());
        labels.put("model", info.model());
        labels.put("serial", info.serialNumber());
        labels.put("version", info.version());
        labels.put("sunspec_model", Integer.toString(info.inverterModel()));
        return Labels.of(new ArrayList<>(labels.keySet()), new ArrayList<>(labels.values()));
    }

    private static MetricSnapshot snapshot(String name, String help, boolean counter, List<Point> points) {
        if (counter) {
            var builder = CounterSnapshot.builder().name(name).help(help);
            points.forEach(p -> builder.dataPoint(CounterDataPointSnapshot.builder().labels(p.labels)
                    .value(p.value).build()));
            return builder.build();
        }
        var builder = GaugeSnapshot.builder().name(name).help(help);
        points.forEach(p -> builder.dataPoint(GaugeDataPointSnapshot.builder().labels(p.labels)
                .value(p.value).build()));
        return builder.build();
    }

    private record Point(Labels labels, double value) {
    }

    /** Metrics of the DC inputs (model 160). */
    enum MpptMetric {
        CURRENT("sunspec_mppt_current_amperes", "DC current of the input", false, MpptInput::current),
        VOLTAGE("sunspec_mppt_voltage_volts", "DC voltage of the input", false, MpptInput::voltage),
        POWER("sunspec_mppt_power_watts", "DC power of the input", false, MpptInput::power),
        ENERGY("sunspec_mppt_energy_watthours", "DC energy of the input over its lifetime", true,
                MpptInput::energy);

        final String metricName;
        final String help;
        final boolean counter;
        final Function<MpptInput, Double> value;

        MpptMetric(String metricName, String help, boolean counter, Function<MpptInput, Double> value) {
            this.metricName = metricName;
            this.help = help;
            this.counter = counter;
            this.value = value;
        }
    }
}
