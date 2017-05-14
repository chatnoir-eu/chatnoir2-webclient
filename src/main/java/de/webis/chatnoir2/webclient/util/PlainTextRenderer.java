/*
 * ChatNoir 2 Web frontend.
 *
 * Copyright (C) 2014-2017 Webis Group @ Bauhaus University Weimar
 * Main Contributor: Janek Bevendorff
 */

package de.webis.chatnoir2.webclient.util;

import org.apache.commons.lang.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;

/**
 * Render HTML to structured plain text or a basic HTML subset.
 *
 * Based on Jsoup's HtmlToPlainText converter by Jonathan Hedley
 * https://github.com/jhy/jsoup/blob/master/src/main/java/org/jsoup/examples/HtmlToPlainText.java
 */
public class PlainTextRenderer {
    /**
     * Convert an HTML string to plain text with a maximum line width of 80 characters.
     *
     * @param html HTML string
     * @return formatted text
     */
    public static String getPlainText(String html) {
        return convert(html, false, 80);
    }

    /**
     * Convert an HTML string to plain text.
     *
     * @param html HTML string
     * @param maxWidth maximum line width
     * @return formatted text
     */
    public static String getPlainText(String html, int maxWidth) {
        return convert(html, false, maxWidth);
    }

    /**
     * Convert an HTML string to basic HTML.
     * Basic HTML only has a very limited set of tags (p, pre, blockquote, h1-h6, ul, ol, dl, dt,
     * dd, em, strong, i, b, code, br), no tag attributes, no styles and no scripts.
     *
     * @param html HTML string
     * @return formatted text
     */
    public static String getBasicHtml(String html) {
        return convert(html, true, 80);
    }

    private static String convert(String html, boolean basicHtml, int maxWidth) {
        FormattingVisitor formatter = new FormattingVisitor(basicHtml, maxWidth);

        try {
            Element doc = Jsoup.parse(html);

            // extract document title
            Elements title = doc.getElementsByTag("title");
            if (!title.isEmpty()) {
                formatter.mTitle = title.first().text();
            }

            // traverse body
            NodeTraversor traversor = new NodeTraversor(formatter);
            Elements body = doc.getElementsByTag("body");
            traversor.traverse(!body.isEmpty() ? body.first() : doc);

            return formatter.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private static class FormattingVisitor implements NodeVisitor {
        private final int mMaxWidth;
        private final boolean mBasicHTML;
        private final StringBuilder mFinalText = new StringBuilder();

        private int mWidth  = 0;
        private String mTitle = "";

        private int mListIndent = 0;
        private boolean mCollapseBreak = false;

        private final String[] mAllowedBlockElements = {
                "p",
                "pre", "blockquote",
                "h1", "h2", "h3", "h4", "h5", "h6",
                "ul", "ol", "dl", "dt", "dd", "li"
        };
        private final String[] mAllowedInlineElements = {
                "b", "i", "em", "strong", "code"
        };
        private final String[] mBreakElements = {
                "br", "tr"
        };
        private final String[] mCollapseBreakElements = {
                "article", "aside", "button", "caption", "div", "fieldset", "figcaption",
                "figure", "footer", "form", "header", "hgroup", "output", "section", "table"
        };
        private final String[] mNoDoubleBreakElements = {
                "li", "dt", "dd"
        };
        private final String[] mListElements = {
                "ul", "ol", "dl"
        };
        private final String[] mListItemElements = {
                "li", "dt"
        };

        FormattingVisitor(boolean basicHtml, int maxWidth) {
            mBasicHTML = basicHtml;
            mMaxWidth = maxWidth;
        }

        @Override
        public void head(Node node, int depth) {
            String name = node.nodeName();

            if (node instanceof TextNode) {
                String text = ((TextNode) node).text();
                if (mBasicHTML) {
                    text = StringEscapeUtils.escapeHtml(text);
                }
                append(text);
            } else if (StringUtil.in(name, mAllowedBlockElements)) {
                if (mBasicHTML) {
                    append("\n<" + name + ">");
                } else {
                    append("\n");
                    if (StringUtil.in(name, mListElements) && mListIndent < 4) {
                        ++mListIndent;
                    } else if (StringUtil.in(name, mListItemElements)) {
                        for (int i = 0; i < mListIndent; ++i) {
                            append("  ");
                        }
                        append("* ");
                    }
                }
            } else if (StringUtil.in(name, mAllowedInlineElements)) {
                if (mBasicHTML) {
                    append("<" + name + ">");
                }
            }
        }

        @Override
        public void tail(Node node, int depth) {
            String name = node.nodeName();

             if (StringUtil.in(name, mAllowedBlockElements) || StringUtil.in(name, mAllowedInlineElements)) {
                 if (mBasicHTML) {
                     append("</" + name + ">");
                 } else {
                     if (!StringUtil.in(name, mNoDoubleBreakElements)) {
                         append("\n");
                     }

                     if (StringUtil.in(name, mListElements)) {
                         mListIndent = Math.max(0, mListIndent -1);
                     }
                 }
                 mCollapseBreak = true;
                 return;
             }

             if (StringUtil.in(name, mBreakElements)) {
                 if (mBasicHTML) {
                     append("<br>\n");
                 } else {
                     append("\n");
                 }
                 mCollapseBreak = true;
                 return;
             }

             boolean inCollapse = StringUtil.in(name, mCollapseBreakElements);
             if (inCollapse && !mCollapseBreak) {
                 if (mBasicHTML) {
                     append("<br>\n");
                 } else {
                     append("\n");
                 }
                 mCollapseBreak = true;
             } else if (!inCollapse && !(node instanceof TextNode)) {
                 mCollapseBreak = false;
             }

        }

        private void append(String text) {
            if (text.startsWith("\n")) {
                mWidth = 0;
            }
            if (text.equals(" ") &&
                    (mFinalText.length() == 0 || StringUtil.in(mFinalText.substring(mFinalText.length() - 1), " ", "\n"))) {
                return;
            }

            if (text.length() + mWidth > mMaxWidth) {
                String words[] = text.split("\\s+");
                for (int i = 0; i < words.length; ++i) {
                    String word = words[i];
                    boolean last = i == words.length - 1;
                    if (!last) {
                        word = word + " ";
                    }
                    if (word.length() + mWidth > mMaxWidth) {
                        mFinalText.append("\n").append(word);
                        mWidth = word.length();
                    } else {
                        mFinalText.append(word);
                        mWidth += word.length();
                    }
                }
            } else {
                mFinalText.append(text);
                mWidth += text.length();
            }
        }

        @Override
        public String toString() {
            if (mBasicHTML) {
                return "<!doctype html>\n" +
                        "<meta charset=\"utf-8\">\n" +
                        "<title>" + StringEscapeUtils.escapeHtml(mTitle) + "</title>\n" +
                        "<body>\n" + mFinalText.toString() + "\n</body>";
            }

            return mFinalText.toString();
        }
    }
}