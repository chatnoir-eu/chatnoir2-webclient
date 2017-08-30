/*
 * ChatNoir 2 Web Frontend.
 * Copyright (C) 2014-2017 Janek Bevendorff, Webis Group
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package de.webis.chatnoir2.webclient.util;


/**
 * Text cleansing tools.
 */
public class TextCleanser
{
    private static final String[] WESTERN_UNICODE_CHARS = {
            "¢", "¿", "…", "·", "“", "”", "«", "»", "§", "¶", "´", "¯", "°", "©",
            "®", "±", "÷", "×", "¬", "£", "¥", "€", "¹", "½", "¼", "²", "³", "¾",
            "ª", "Á", "á", "À", "à", "Ă", "ă", "Â", "â", "Å", "å", "Ä", "ä", "Ã",
            "ã", "Ā", "ā", "Æ", "æ", "Ḃ", "ḃ", "Č", "č", "Ċ", "ċ", "Ç", "ç", "Ď",
            "ď", "Ḋ", "ḋ", "Ð", "ð", "É", "é", "È", "è", "Ê", "ê", "Ě", "ě", "Ë",
            "ë", "Ē", "ē", "Ḟ", "ḟ", "Ğ", "ğ", "Ġ", "ġ", "Í", "í", "Ì", "ì", "Î",
            "î", "Ï", "ï", "İ", "Ī", "ī", "ı", "Ṁ", "ṁ", "Ň", "ň", "Ñ", "ñ", "º",
            "Ó", "ó", "Ò", "ò", "Ô", "ô", "Ö", "ö", "Õ", "õ", "Ø", "ø", "Ō", "ō",
            "Œ", "œ", "Ř", "ř", "Š", "š", "Š", "š", "Ṡ", "ṡ", "Ş", "ş", "Ș", "ș",
            "ß", "Ť", "ť", "Ṫ", "ṫ", "Ţ", "ţ", "Ț", "ț", "Ú", "ú", "Ù", "ù", "Û",
            "û", "Ů", "ů", "Ü", "ü", "Ū", "ū", "Ẃ", "ẃ", "Ẁ", "ẁ", "Ŵ", "ŵ", "Ý",
            "ý", "Ŷ", "ŷ", "Ÿ", "ÿ", "Ž", "ž", "Ž", "ž", "Þ", "þ", "µ"
    };

    private static final String[] BROKEN_ISO_8859_1_CHARS = {
            "Â¢", "Â¿", "â¦", "Â·", "â", "â", "Â«", "Â»", "Â§", "Â¶", "Â´", "Â¯", "Â°", "Â©",
            "Â®", "Â±", "Ã·", "Ã", "Â¬", "Â£", "Â¥", "â¬", "Â¹", "Â½", "Â¼", "Â²", "Â³", "Â¾",
            "Âª", "Ã", "Ã¡", "Ã", "Ã ", "Ä", "Ä", "Ã", "Ã¢", "Ã", "Ã¥", "Ã", "Ã¤", "Ã",
            "Ã£", "Ä", "Ä", "Ã", "Ã¦", "á¸", "á¸", "Ä", "Ä", "Ä", "Ä", "Ã", "Ã§", "Ä",
            "Ä", "á¸", "á¸", "Ã", "Ã°", "Ã", "Ã©", "Ã", "Ã¨", "Ã", "Ãª", "Ä", "Ä", "Ã",
            "Ã«", "Ä", "Ä", "á¸", "á¸", "Ä", "Ä", "Ä ", "Ä¡", "Ã", "Ã­", "Ã", "Ã¬", "Ã",
            "Ã®", "Ã", "Ã¯", "Ä°", "Äª", "Ä«", "Ä±", "á¹", "á¹", "Å", "Å", "Ã", "Ã±", "Âº",
            "Ã", "Ã³", "Ã", "Ã²", "Ã", "Ã´", "Ã", "Ã¶", "Ã", "Ãµ", "Ã", "Ã¸", "Å", "Å",
            "Å", "Å", "Å", "Å", "Å ", "Å¡", "Å ", "Å¡", "á¹ ", "á¹¡", "Å", "Å", "È", "È",
            "Ã", "Å¤", "Å¥", "á¹ª", "á¹«", "Å¢", "Å£", "È", "È", "Ã", "Ãº", "Ã", "Ã¹", "Ã",
            "Ã»", "Å®", "Å¯", "Ã", "Ã¼", "Åª", "Å«", "áº", "áº", "áº", "áº", "Å´", "Åµ", "Ã",
            "Ã½", "Å¶", "Å·", "Å¸", "Ã¿", "Å½", "Å¾", "Å½", "Å¾", "Ã", "Ã¾", "Âµ"
    };

    private static final String[] BROKEN_ISO_8859_15_CHARS = {
            "Â¢", "Â¿", "âŠ", "Â·", "â", "â", "Â«", "Â»", "Â§", "Â¶", "ÂŽ", "Â¯", "Â°", "Â©",
            "Â®", "Â±", "Ã·", "Ã", "Â¬", "Â£", "Â¥", "â¬", "Â¹", "Âœ", "ÂŒ", "Â²", "Â³", "ÂŸ",
            "Âª", "Ã", "Ã¡", "Ã", "Ã ", "Ä", "Ä", "Ã", "Ã¢", "Ã", "Ã¥", "Ã", "Ã€", "Ã",
            "Ã£", "Ä", "Ä", "Ã", "ÃŠ", "áž", "áž", "Ä", "Ä", "Ä", "Ä", "Ã", "Ã§", "Ä",
            "Ä", "áž", "áž", "Ã", "Ã°", "Ã", "Ã©", "Ã", "Ãš", "Ã", "Ãª", "Ä", "Ä", "Ã",
            "Ã«", "Ä", "Ä", "áž", "áž", "Ä", "Ä", "Ä ", "Ä¡", "Ã", "Ã­", "Ã", "Ã¬", "Ã",
            "Ã®", "Ã", "Ã¯", "Ä°", "Äª", "Ä«", "Ä±", "á¹", "á¹", "Å", "Å", "Ã", "Ã±", "Âº",
            "Ã", "Ã³", "Ã", "Ã²", "Ã", "ÃŽ", "Ã", "Ã¶", "Ã", "Ãµ", "Ã", "Ãž", "Å", "Å",
            "Å", "Å", "Å", "Å", "Å ", "Å¡", "Å ", "Å¡", "á¹ ", "á¹¡", "Å", "Å", "È", "È",
            "Ã", "Å€", "Å¥", "á¹ª", "á¹«", "Å¢", "Å£", "È", "È", "Ã", "Ãº", "Ã", "Ã¹", "Ã",
            "Ã»", "Å®", "Å¯", "Ã", "ÃŒ", "Åª", "Å«", "áº", "áº", "áº", "áº", "ÅŽ", "Åµ", "Ã",
            "Ãœ", "Å¶", "Å·", "Åž", "Ã¿", "Åœ", "ÅŸ", "Åœ", "ÅŸ", "Ã", "ÃŸ", "Âµ"
    };

    private static final String[] BROKEN_ISO_8859_14_CHARS = {
            "Âḃ", "Âṡ", "âḊ", "ÂṖ", "â", "â", "Âḋ", "ÂṠ", "Â§", "Â¶", "ÂṀ", "ÂŸ", "ÂḞ", "Â©",
            "Â®", "Âḟ", "ÃṖ", "Ã", "ÂỲ", "Â£", "Âċ", "âỲ", "Âṗ", "ÂẄ", "Âỳ", "ÂĠ", "Âġ", "Âẅ",
            "ÂẂ", "Ã", "ÃḂ", "Ã", "Ã ", "Ä", "Ä", "Ã", "Ãḃ", "Ã", "Ãċ", "Ã", "ÃĊ", "Ã",
            "Ã£", "Ä", "Ä", "Ã", "ÃḊ", "áẁ", "áẁ", "Ä", "Ä", "Ä", "Ä", "Ã", "Ã§", "Ä",
            "Ä", "áẁ", "áẁ", "Ã", "ÃḞ", "Ã", "Ã©", "Ã", "ÃẀ", "Ã", "ÃẂ", "Ä", "Ä", "Ã",
            "Ãḋ", "Ä", "Ä", "áẁ", "áẁ", "Ä", "Ä", "Ä ", "ÄḂ", "Ã", "Ã­", "Ã", "ÃỲ", "Ã",
            "Ã®", "Ã", "ÃŸ", "ÄḞ", "ÄẂ", "Äḋ", "Äḟ", "áṗ", "áṗ", "Å", "Å", "Ã", "Ãḟ", "Âẃ",
            "Ã", "Ãġ", "Ã", "ÃĠ", "Ã", "ÃṀ", "Ã", "Ã¶", "Ã", "Ãṁ", "Ã", "Ãẁ", "Å", "Å",
            "Å", "Å", "Å", "Å", "Å ", "ÅḂ", "Å ", "ÅḂ", "áṗ ", "áṗḂ", "Å", "Å", "È", "È",
            "Ã", "ÅĊ", "Åċ", "áṗẂ", "áṗḋ", "Åḃ", "Å£", "È", "È", "Ã", "Ãẃ", "Ã", "Ãṗ", "Ã",
            "ÃṠ", "Å®", "ÅŸ", "Ã", "Ãỳ", "ÅẂ", "Åḋ", "áẃ", "áẃ", "áẃ", "áẃ", "ÅṀ", "Åṁ", "Ã",
            "ÃẄ", "Å¶", "ÅṖ", "Åẁ", "Ãṡ", "ÅẄ", "Åẅ", "ÅẄ", "Åẅ", "Ã", "Ãẅ", "Âṁ"
    };

    private boolean mIsHtml;
    private String mString;

    /**
     * Cleanse the given string by removing any clutter such as runs of special characters, hyphens or spaces.
     * Performs all available text cleansing operations provided by this class.
     *
     * @param str non-HTML text to cleanse
     * @return cleansed and more readable string
     */
    public static String cleanseAll(String str)
    {
        return cleanseAll(str, false);
    }

    /**
     * Cleanse the given string by removing any clutter such as runs of special characters, hyphens or spaces.
     * Performs all available text cleansing operations provided by this class.
     *
     * @param str text to cleanse
     * @param html input is HTML escaped
     * @return cleansed and more readable string
     */
    public static String cleanseAll(String str, boolean html)
    {
        // order is important!
        return new TextCleanser(str, html)
                .encodingErrors()
                .doubleHtmlEscape()
                .unclosedBrackets()
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
    public TextCleanser encodingErrors()
    {
        // repair western unicode characters which were interpreted as ISO 8859-1 or ISO 8859-15
        for (int i = 0; i < WESTERN_UNICODE_CHARS.length; ++i) {
            mString = mString.replace(BROKEN_ISO_8859_1_CHARS[i], WESTERN_UNICODE_CHARS[i]);
            if (!BROKEN_ISO_8859_1_CHARS[i].equals(BROKEN_ISO_8859_15_CHARS[i])) {
                mString = mString.replace(BROKEN_ISO_8859_15_CHARS[i], WESTERN_UNICODE_CHARS[i]);
            }
            if (!BROKEN_ISO_8859_1_CHARS[i].equals(BROKEN_ISO_8859_14_CHARS[i])) {
                mString = mString.replace(BROKEN_ISO_8859_14_CHARS[i], WESTERN_UNICODE_CHARS[i]);
            }
        }

        // strip unicode replacement characters
        mString =  mString.replace("\ufffd", "");

        return this;
    }

    /**
     * Correct double HTML escaping.
     */
    public TextCleanser doubleHtmlEscape()
    {
        mString = mString.replaceAll("&amp;(\\w{1,8});", "&$1;");
        return this;
    }

    /**
     * Remove short passages (up to 10 characters) of text at the end if they are preceded by
     * an opening bracket which isn't closed.
     */
    public TextCleanser unclosedBrackets()
    {
        if (mIsHtml) {
            mString = mString.replaceFirst("(\\(|\\[|&lt;).{0,10}(?!(?:\\)|]|&gt;))\\s*$", "").trim();
        } else {
            mString = mString.replaceFirst("(\\(|\\[|<).{0,10}(?!(?:\\)|]|>))\\s*$", "").trim();
        }
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
    @Deprecated
    public TextCleanser nonWordChars()
    {
        if (!mIsHtml) {
            mString = mString.trim()
                // remove runs of special characters
                .replaceAll("(([,;.:\\-_#'+~*^°!\"§$%&/()={}<>|])\\2)(?:\\w+\\1)*", "").trim()
                // remove non-word characters at the beginning or end
                // (opening brackets at the beginning or closing at the end are okay)
                .replaceAll("^[,;.:\\-_#'+~*^°!\"§$%&/)=}\\]>|]+|[,;.:\\-_#'+~*^°!\"§$%&/(={\\[<|]+$", "").trim();
        } else {
            // do the same, but also consider HTML entities
            mString = mString.trim()
                .replaceAll("((&amp;|&lt;|&gt;|[,;.:\\-_#'+~*^°!\"§$%/()={}|])\\2)(?:\\w+\\1)*", "").trim()
                .replaceAll("^(&amp;|&gt;|[,;.:\\-_#'+~*^°!\"§$%/)=}\\]|])+|(&amp;|&lt;|[,;.:\\-_#'+~*^°!\"§$%/(={\\[|])+$", "").trim();
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
            mString = mString.replaceAll("((?:<(\\w+)>)?([^\\s])+?(?:</\\2>)?)(\\s+\\1){2,}", "$1 $1").trim();
        }

        return this;
    }
}
