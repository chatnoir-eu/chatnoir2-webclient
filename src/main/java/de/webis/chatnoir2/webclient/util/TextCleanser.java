/*
 * ChatNoir 2 Web frontend.
 *
 * Copyright (C) 2014-2017 Webis Group @ Bauhaus University Weimar
 * Main Contributor: Janek Bevendorff
 */

package de.webis.chatnoir2.webclient.util;


/**
 * Text cleansing tools.
 */
public class TextCleanser
{
    /**
     * Cleanse the given string by removing any clutter such as runs of special characters, hyphens or spaces.
     *
     * @param str String to cleanse
     * @return cleansed and more readable string
     */
    public static String cleanse(String str)
    {
        return cleanse(str, false);
    }

    /**
     * Cleanse the given string by removing any clutter such as runs of hyphens or spaces.
     *
     * @param str String to cleanse
     * @param html input is HTML escaped
     * @return cleansed and more readable string
     */
    public static String cleanse(String str, boolean html)
    {
        str = cleanseEncodingErrors(str);
        str = specialChars(str, html);
        str = repeatedWords(str, html);
        str = whitespace(str, html);

        return str;
    }

    /**
     * Remove question mark place holder characters caused by broken encoding.
     *
     * @param str String to cleanse
     * @return cleansed string
     */
    public static String cleanseEncodingErrors(String str) {
        return str.replace("\ufffd", "");
    }

    private static String whitespace(final String str, boolean html)
    {
        if (!html)
            return str.replaceAll("[ \\s]+", " ");
        else
            return str.replaceAll("(?:[ \\s]|&nbsp;)+", " ");
    }

    private static String specialChars(final String str, boolean html)
    {
        if (!html) {
            return str
                    // runs of special characters
                    .replaceAll("(([,;.:\\-_#'+~*^°!\"§$%&/()={}<>|])\\2)(?:\\w+\\1)*", "")
                    // special characters at the beginning or end
                    .replaceAll("^[,;.:\\-_#'+~*^°!\"§$%&/()={}<>|]+|[,;.:\\-_#'+~*^°!\"§$%&/()={}<>|]+$", "");
        } else {
            return str
                    // runs of special characters
                    .replaceAll("((&amp;|&lt;|&gt;|[,;.:\\-_#'+~*^°!\"§$%/()={}|])\\2)(?:\\w+\\1)*", "")
                    // special characters at the beginning or end
                    .replaceAll("^(&amp;|&lt;|&gt;|[,;.:\\-_#'+~*^°!\"§$%/()={}|])+|(&amp;|&lt;|&gt;|[,;.:\\-_#'+~*^°!\"§$%/()={}|])+$", "");
        }
    }

    private static String repeatedWords(final String str, boolean html)
    {
        if (!html)
            return str.replaceAll("([^\\s]+?) \\1{2,}", "");
        else
            return str.replaceAll("((?:<(em|strong)>)?([^\\s])+?(?:</\\2>)?)(\\s+\\1){2,}", "$1 $1");
    }
}
