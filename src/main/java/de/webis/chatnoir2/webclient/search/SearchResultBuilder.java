/*
 * ChatNoir 2 Web frontend.
 *
 * Copyright (C) 2014-2017 Webis Group @ Bauhaus University Weimar
 * Main Contributor: Janek Bevendorff
 */

package de.webis.chatnoir2.webclient.search;

import de.webis.chatnoir2.webclient.resources.ConfigLoader;
import de.webis.chatnoir2.webclient.util.Configured;
import org.apache.lucene.search.Explanation;
import org.elasticsearch.common.xcontent.ToXContent;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

/**
 * Search result builder.
 *
 * @author Janek Bevendorff
 */
public class SearchResultBuilder
{
    /**
     * Builder for {@link SearchResultBuilder.SearchResult}.
     */
    protected final SearchResult mSearchResult;

    public SearchResultBuilder()
    {
        mSearchResult = new SearchResult();
    }

    /**
     * @return configured {@link SearchResultBuilder.SearchResult}
     */
    public SearchResult build()
    {
        return mSearchResult;
    }

    public SearchResultBuilder score(float score)
    {
        mSearchResult.setScore(score);
        return this;
    }

    public SearchResultBuilder index(String index)
    {
        String displayIndex = index;
        ConfigLoader.Config[] conf = Configured.getInstance().getConf().getArray("cluster.index_aliases");
        if (conf.length != 0) {
            for (ConfigLoader.Config c : conf) {
                if (c.getString("index", "").equals(index)) {
                    index = c.getString("alias", "");
                    if (c.contains("display_name")) {
                        displayIndex = c.getString("display_name");
                    }

                    break;
                }
            }
        }
        mSearchResult.mIndex = index;
        mSearchResult.mDisplayIndex = displayIndex;
        return this;
    }

    public SearchResultBuilder documentId(String documentId)
    {
        mSearchResult.setDocumentId(documentId);
        return this;
    }

    public SearchResultBuilder trecId(@Nullable String trecId)
    {
        mSearchResult.setTrecId(trecId);
        return this;
    }

    public SearchResultBuilder title(String title)
    {
        mSearchResult.setTitle(title);
        return this;
    }

    public SearchResultBuilder spamRank(@Nullable Integer spamRank)
    {
        mSearchResult.setSpamRank(spamRank);
        return this;
    }

    public SearchResultBuilder pageRank(@Nullable Double pageRank)
    {
        mSearchResult.setPageRank(pageRank);
        return this;
    }

    public SearchResultBuilder targetHostname(String targetHostname)
    {
        mSearchResult.setTargetHostname(targetHostname);
        return this;
    }

    public SearchResultBuilder targetPath(String targetPath)
    {
        mSearchResult.setTargetPath(targetPath);
        return this;
    }

    public SearchResultBuilder targetUri(String targetUri)
    {
        mSearchResult.setTargetUri(targetUri);
        return this;
    }

    public SearchResultBuilder snippet(String snippet)
    {
        mSearchResult.setSnippet(snippet);
        return this;
    }

    public SearchResultBuilder fullBody(@Nullable String fullBody)
    {
        mSearchResult.setFullBody(fullBody);
        return this;
    }

    /**
     * Whether displaying a "more like this" or "more from this host" link is suggested
     */
    public SearchResultBuilder isMoreSuggested(boolean moreSuggested)
    {
        mSearchResult.setMoreSuggested(moreSuggested);
        return this;
    }

    /**
     * Whether grouping with previous result is suggested
     */
    public SearchResultBuilder isGroupingSuggested(boolean groupingSuggested)
    {
        mSearchResult.setGroupingSuggested(groupingSuggested);
        return this;
    }

    public SearchResultBuilder explanation(@Nullable Explanation explanation)
    {
        mSearchResult.setExplanation(explanation);
        return this;
    }

    /**
     * Search result.
     */
    public class SearchResult
    {
        private float mScore = 0.0f;
        private String mIndex = "";
        private String mDisplayIndex = "";
        private String mDocumentId = "";
        private String mTrecId = null;
        private String mTitle = "";
        private Integer mSpamRank = null;
        private Double mPageRank = null;
        private String mTargetHostname = "";
        private String mTargetPath = "";
        private String mTargetUri = "";
        private String mSnippet = "";
        private String mFullBody = null;
        private boolean mMoreSuggested = false;
        private boolean mGroupingSuggested = false;
        private Explanation mExplanation = null;

        public Float score()
        {
            return mScore;
        }

        public String scoreFormatted()
        {
            return String.format("%.03f", mScore);
        }

        public void setScore(float score)
        {
            mScore = score;
        }

        public String index()
        {
            return mIndex;
        }

        public String indexUrlEnc()
        {
            try {
                return URLEncoder.encode(mIndex, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                return "";
            }
        }

        public void setIndex(String index)
        {
            mIndex = index;
        }

        public String displayIndex()
        {
            return mDisplayIndex;
        }

        public void setDisplayIndex(String index)
        {
            mDisplayIndex = index;
        }

        public String documentId()
        {
            return mDocumentId;
        }

        public String documentIdUrlEnc()
        {
            try {
                return URLEncoder.encode(mDocumentId, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                return "";
            }
        }

        public void setDocumentId(String documentId)
        {
            mDocumentId = documentId;
        }

        @CheckForNull
        public String trecId()
        {
            return mTrecId;
        }

        public void setTrecId(@Nullable String trecId)
        {
            mTrecId = trecId;
        }

        public String title()
        {
            return mTitle;
        }

        public void setTitle(String title)
        {
            mTitle = title;
        }

        @CheckForNull
        public Integer spamRank()
        {
            return mSpamRank;
        }

        public String spamRankFormatted()
        {
            if (null != mSpamRank && 0 != mSpamRank) {
                return mSpamRank.toString();
            }
            return "none";
        }

        public void setSpamRank(@Nullable Integer spamRank)
        {
            mSpamRank = spamRank;
        }

        @CheckForNull
        public Double pageRank()
        {
            return mPageRank;
        }

        public String pageRankFormatted()
        {
            if (null != mPageRank) {
                if (0.001 > mPageRank) {
                    return String.format("%.03e", mPageRank);
                }
                return String.format("%.03f", mPageRank);
            }
            return "none";
        }

        public void setPageRank(@Nullable Double pageRank)
        {
            mPageRank = pageRank;
        }

        public String targetHostname()
        {
            return mTargetHostname;
        }

        public void setTargetHostname(String targetHostname)
        {
            mTargetHostname = targetHostname;
        }

        public String targetPath()
        {
            return mTargetPath;
        }

        public void setTargetPath(String targetPath)
        {
            mTargetPath = targetPath;
        }

        public String targetUri()
        {
            return mTargetUri;
        }

        public void setTargetUri(String targetUri)
        {
            mTargetUri = targetUri;
        }

        public String snippet()
        {
            return mSnippet;
        }

        public void setSnippet(String snippet)
        {
            mSnippet = snippet;
        }

        @CheckForNull
        public String fullBody()
        {
            return mFullBody;
        }

        public void setFullBody(@Nullable String fullBody)
        {
            mFullBody = fullBody;
        }

        /**
         * @return whether displaying a "more like this" or "more from this host" link is suggested
         */
        public boolean isMoreSuggested()
        {
            return mMoreSuggested;
        }

        public void setMoreSuggested(boolean moreSuggested)
        {
            mMoreSuggested = moreSuggested;
        }

        /**
         * @return whether grouping with previous result is suggested
         */
        public boolean isGroupingSuggested()
        {
            return mGroupingSuggested;
        }

        public void setGroupingSuggested(boolean groupingSuggested)
        {
            mGroupingSuggested = groupingSuggested;
        }

        @CheckForNull
        public Explanation explanation()
        {
            return mExplanation;
        }

        public String explanationString()
        {
            if (null != mExplanation) {
                try {
                    return new ExplanationXContent(mExplanation).toXContent(jsonBuilder(), ToXContent.EMPTY_PARAMS).string();
                } catch (IOException ignored) {}
            }

            return "";
        }

        public void setExplanation(@Nullable Explanation explanation)
        {
            mExplanation = explanation;
        }
    }
}