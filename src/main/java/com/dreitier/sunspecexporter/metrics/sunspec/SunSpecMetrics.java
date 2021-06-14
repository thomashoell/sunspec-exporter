package com.dreitier.sunspecexporter.metrics.sunspec;

import com.dreitier.sunspecexporter.sunspec.SunSpecException;

/**
 * @author tho[at]dreitier[dot]com
 */
public interface SunSpecMetrics {

    void refresh() throws SunSpecException;
    void drop();
}
