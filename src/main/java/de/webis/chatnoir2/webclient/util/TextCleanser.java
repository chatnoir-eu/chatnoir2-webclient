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
    private static final String[] WESTERN_UNICODE_CHARS = {
            "à", "á", "â", "ã", "ä", "å", "æ", "ç", "è", "é", "ê", "ë", "í", "î",
            "ï", "ò", "ó", "ô", "õ", "ö", "ø", "ù", "ú", "û", "ü", "ý", "ÿ", "œ", "ß",
            "À", "Á", "Â", "Ã", "Ä", "Å", "Æ", "Ç", "È", "É", "Ê", "Ë", "Í", "Î",
            "Ï", "Ò", "Ó", "Ô", "Õ", "Ö", "Ø", "Ù", "Ú", "Û", "Ü", "Ý", "Œ", "Ÿ"
    };

    private static final String[] BROKEN_ISO_8859_1_CHARS = {
            "Ã ", "Ã¡", "Ã¢", "Ã£", "Ã¤", "Ã¥", "Ã¦", "Ã§", "Ã¨", "Ã©", "Ãª", "Ã«", "Ã­", "Ã®",
            "Ã¯", "Ã²", "Ã³", "Ã´", "Ãµ", "Ã¶", "Ã¸", "Ã¹", "Ãº", "Ã»", "Ã¼", "Ã½", "Ã¿", "Å", "Ã",
            "Ã", "Ã", "Ã", "Ã", "Ã", "Ã", "Ã", "Ã", "Ã", "Ã", "Ã", "Ã", "Ã", "Ã",
            "Ã", "Ã", "Ã", "Ã", "Ã", "Ã", "Ã", "Ã", "Ã", "Ã", "Ã", "Ã", "Å", "Å¸"
    };

    private static final String[] BROKEN_ISO_8859_15_CHARS = {
            "Ã ", "Ã¡", "Ã¢", "Ã£", "Ã€", "Ã¥", "ÃŠ", "Ã§", "Ãš", "Ã©", "Ãª", "Ã«", "Ã­", "Ã®",
            "Ã¯", "Ã²", "Ã³", "ÃŽ", "Ãµ", "Ã¶", "Ãž", "Ã¹", "Ãº", "Ã»", "ÃŒ", "Ãœ", "Ã¿", "Å", "Ã",
            "Ã", "Ã", "Ã", "Ã", "Ã", "Ã", "Ã", "Ã", "Ã", "Ã", "Ã", "Ã", "Ã", "Ã",
            "Ã", "Ã", "Ã", "Ã", "Ã", "Ã", "Ã", "Ã", "Ã", "Ã", "Ã", "Ã", "Å", "Åž"
    };

    private boolean mIsHtml;
    private String mString;

    /**
     * Cleanse the given string by removing any clutter such as runs of special characters, hyphens or spaces.
     *
     * @param str String to cleanse
     * @return cleansed and more readable string
     */
    public static String cleanseAll(String str)
    {
        return cleanseAll(str, false);
    }

    /**
     * Cleanse the given string by removing any clutter such as runs of hyphens or spaces.
     *
     * @param str String to cleanse
     * @param html input is HTML escaped
     * @return cleansed and more readable string
     */
    public static String cleanseAll(String str, boolean html)
    {
        return new TextCleanser(str, html)
                .encodingErrors()
                .nonWordChars()
                .repeatedWords()
                .whitespace()
                .get();
    }

    /**
     * @param str string to cleanse
     * @param html whether string is HTML
     */
    public TextCleanser(String str, boolean html)
    {
        mString = str;
        mIsHtml = html;
    }

    /**
     * Return cleansed string.
     *
     * @return cleansed string
     */
    public String get()
    {
        return mString;
    }

    /**
     * Remove question mark place holder characters caused by broken encoding
     * and repair western multi-byte characters which were interpreted as single-byte.
     */
    public TextCleanser encodingErrors() {
        // repair western unicode characters which were interpreted as ISO 8859-1 or ISO 8859-15
        for (int i = 0; i < WESTERN_UNICODE_CHARS.length; ++i) {
            mString = mString.replace(BROKEN_ISO_8859_1_CHARS[i], WESTERN_UNICODE_CHARS[i]);
            if (!BROKEN_ISO_8859_1_CHARS[i].equals(BROKEN_ISO_8859_15_CHARS[i])) {
                mString = mString.replace(BROKEN_ISO_8859_15_CHARS[i], WESTERN_UNICODE_CHARS[i]);
            }
        }

        // strip unicode replacement characters
        mString =  mString.replace("\ufffd", "");

        return this;
    }

    /**
     * Remove consecutive runs of white space.
     */
    public TextCleanser whitespace()
    {
        if (!mIsHtml) {
            mString = mString.replaceAll("[ \\s]+", " ");
        } else {
            mString = mString.replaceAll("(?:[ \\s]|&nbsp;)+", " ");
        }

        return this;
    }

    /**
     * Remove consecutive runs of non-word special characters.
     */
    public TextCleanser nonWordChars()
    {
        if (!mIsHtml) {
            mString = mString.trim()
                // runs of special characters
                .replaceAll("(([,;.:\\-_#'+~*^°!\"§$%&/()={}<>|])\\2)(?:\\w+\\1)*", "").trim()
                // non-word characters at the beginning or end
                .replaceAll("^[,;.:\\-_#'+~*^°!\"§$%&/()={}<>|]+|[,;.:\\-_#'+~*^°!\"§$%&/()={}<>|]+$", "").trim();
        } else {
            mString = mString.trim()
                // runs of special characters
                .replaceAll("((&amp;|&lt;|&gt;|[,;.:\\-_#'+~*^°!\"§$%/()={}|])\\2)(?:\\w+\\1)*", "").trim()
                // non-word characters at the beginning or end
                .replaceAll("^(&amp;|&lt;|&gt;|[,;.:\\-_#'+~*^°!\"§$%/()={}|])+|(&amp;|&lt;|&gt;|[,;.:\\-_#'+~*^°!\"§$%/()={}|])+$", "").trim();
        }

        return this;
    }

    /**
     * Remove repeated words.
     */
    public TextCleanser repeatedWords()
    {
        if (!mIsHtml) {
            mString = mString.replaceAll("([^\\s]+?) \\1{2,}", "").trim();
        } else {
            mString = mString.replaceAll("((?:<(em|strong)>)?([^\\s])+?(?:</\\2>)?)(\\s+\\1){2,}", "$1 $1").trim();
        }

        return this;
    }
}
