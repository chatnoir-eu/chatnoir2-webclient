/*
 * ChatNoir 2 Web frontend.
 *
 * Copyright (C) 2014-2016 Webis Group @ Bauhaus University Weimar
 * Main Contributor: Janek Bevendorff
 */

package de.webis.chatnoir2.webclient.util;

import com.cybozu.labs.langdetect.Detector;
import com.cybozu.labs.langdetect.DetectorFactory;
import com.cybozu.labs.langdetect.LangDetectException;

import java.io.File;
import java.net.URISyntaxException;

/**
 * Language detector wrapper class.
 */
public class LangDetector
{
    private static boolean mInitialized = false;

    /**
     * Detect language of given string and return language code.
     * If language detection fails, "en" is returned.
     *
     * @param string string to detect
     * @return language code
     */
    public static String detect(final String string)
    {
        return detect(string, "en");
    }

    /**
     * Detect language of given string and return language code.
     *
     * @param string string to detect
     * @param defaultLang default language to return if language detection fails
     * @return language code
     */
    public static String detect(final String string, final String defaultLang)
    {
        /*if (!mInitialized) {
            try {
                DetectorFactory.loadProfile(new File(DetectorFactory.class.getResource("profiles").toURI()));
                mInitialized = true;
            } catch (LangDetectException e) {
                e.printStackTrace();
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }

        String lang = defaultLang;
        try {
            final Detector langDetector = DetectorFactory.create();
            langDetector.append(string);
            lang = langDetector.detect();
        } catch (LangDetectException ignored) {}

        return lang;*/
        return defaultLang;
    }
}
