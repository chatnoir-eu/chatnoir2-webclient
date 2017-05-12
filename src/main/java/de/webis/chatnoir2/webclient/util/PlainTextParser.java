/*
 * ChatNoir 2 Web frontend.
 *
 * Copyright (C) 2014-2017 Webis Group @ Bauhaus University Weimar
 * Main Contributor: Janek Bevendorff
 */

package de.webis.chatnoir2.webclient.util;

import org.apache.commons.lang.StringEscapeUtils;
import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;

/**
 * Parse HTML into structured plain text.
 *
 * Based on JSoup's HtmlToPlainText class by Jonathan Hedley:
 * https://github.com/jhy/jsoup/blob/master/src/main/java/org/jsoup/examples/HtmlToPlainText.java
 */
public class PlainTextParser {
    /**
     * Format an Element to plain-text
     * @param element the root element to format
     * @return formatted text
     */
    public static String getPlainText(Element element) {
        FormattingVisitor formatter = new FormattingVisitor();
        NodeTraversor traversor     = new NodeTraversor(formatter);
        traversor.traverse(element);

        return formatter.toString();
    }

    private static class FormattingVisitor implements NodeVisitor {
        private static final int mMaxWidth = 80;
        private int mWidth                 = 0;
        private StringBuilder mFinalText   = new StringBuilder();
        private String mTitle              = "";
        private String[] mAllowedBlockElements = {
                "p",
                "pre",
                "h1", "h2", "h3", "h4", "h5",
                "ul", "ol", "dl", "li", "dt"
        };
        private String[] mAllowedInlineElements = {
                "b", "i", "em", "strong", "code"
        };

        // hit when the node is first seen
        public void head(Node node, int depth) {
            String name = node.nodeName();
            if (node instanceof TextNode) {
                append(((TextNode) node).text(), true); // TextNodes carry all user-readable text in the DOM.
            } else if (StringUtil.in(name, mAllowedBlockElements)) {
                append("\n<" + name + ">", false);
            } else if (StringUtil.in(name, mAllowedInlineElements)) {
                append("<" + name + ">", false);
            } else if (name.equals("title")) {
                StringBuilder sb = new StringBuilder();
                for (Node child : node.childNodes()) {
                    if (child instanceof TextNode) {
                        sb.append(((TextNode) child).text());
                    }
                }
                mTitle = sb.toString();
            }
        }

        // hit when all of the node's children (if any) have been visited
        public void tail(Node node, int depth) {
            String name = node.nodeName();
             if (StringUtil.in(name, mAllowedBlockElements) || StringUtil.in(name, mAllowedInlineElements)) {
                append("</" + name + ">", false);
            }
        }

        // appends text to the string builder with a simple word wrap method
        private void append(String text, boolean escape) {
            if (text.startsWith("\n")) {
                mWidth = 0; // reset counter if starts with a newline. only from formats above, not in natural text
            }
            if (text.equals(" ") &&
                    (mFinalText.length() == 0 || StringUtil.in(mFinalText.substring(mFinalText.length() - 1), " ", "\n"))) {
                return; // don't accumulate long runs of empty spaces
            }

            if (text.length() + mWidth > mMaxWidth) { // won't fit, needs to wrap
                String words[] = text.split("\\s+");
                for (int i = 0; i < words.length; i++) {
                    String word = words[i];
                    boolean last = i == words.length - 1;
                    if (!last) // insert a space if not the last word
                        word = word + " ";
                    if (word.length() + mWidth > mMaxWidth) { // wrap and reset counter
                        if (escape) {
                            word = StringEscapeUtils.escapeHtml(word);
                        }
                        mFinalText.append("\n").append(word);
                        mWidth = word.length();
                    } else {
                        if (escape) {
                            word = StringEscapeUtils.escapeHtml(word);
                        }
                        mFinalText.append(word);
                        mWidth += word.length();
                    }
                }
            } else { // fits as is, without need to wrap text
                if (escape) {
                    text = StringEscapeUtils.escapeHtml(text);
                }
                mFinalText.append(text);
                mWidth += text.length();
            }
        }

        @Override
        public String toString() {
            return "<!doctype html>\n" +
                    "<meta charset=\"utf-8\">\n" +
                    "<title>" + StringEscapeUtils.escapeHtml(mTitle) + "</title>\n" +
                    "<body>\n" + mFinalText.toString() + "\n</body>";
        }
    }
}