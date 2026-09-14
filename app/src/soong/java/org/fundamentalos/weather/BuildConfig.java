/*
 * SPDX-FileCopyrightText: The FundamentalOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.fundamentalos.weather;

/**
 * Hand-written for the in-tree (Soong) build, which -- unlike AGP -- does not
 * generate BuildConfig. Values mirror the "inline" distribution that
 * FundamentalOS ships (Weather built into the OS). It lives under src/soong,
 * outside gradle's sourceSets, so the gradle build never sees it and keeps using
 * its own generated BuildConfig.
 */
public final class BuildConfig {
    private BuildConfig() {}

    public static final boolean INLINE = true;
    public static final String APPLICATION_ID = "org.fundamentalos.weather";
    public static final String FOS_API_BASE_URL = "https://api.fundamentalos.org";
    public static final int VERSION_CODE = 94;
    public static final String VERSION_NAME = "0.1.1";
}
