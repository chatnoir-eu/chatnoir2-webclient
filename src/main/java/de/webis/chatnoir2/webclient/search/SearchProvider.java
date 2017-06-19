/*
 * ChatNoir 2 Web frontend.
 *
 * Copyright (C) 2014-2017 Webis Group @ Bauhaus University Weimar
 * Main Contributor: Janek Bevendorff
 */

package de.webis.chatnoir2.webclient.search;

import de.webis.chatnoir2.webclient.util.TextCleanser;
import org.apache.commons.lang.StringEscapeUtils;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.search.SearchHit;

import java.nio.file.Paths;
import java.util.*;

/**
 * Public interface for search providers.
 *
 * @author Janek Bevendorff
 */
public abstract class SearchProvider extends IndexRetrievalOperator
{
    /**
     * (Default) snippet length.
     */
    private int mSnippetLength = 200;

    /**
     * (Default) title length.
     */
    private int mTitleLength = 60;

    /**
     * Search result language.
     */
    private String mSearchLanguage = "en";

    /**
     * Whether to group results by hostname.
     */
    private boolean mGroupByHostname = true;

    public SearchProvider(final String[] indices)
    {
        super(indices);
        mGroupByHostname = getConf().getBoolean("serp.group_by_hostname", true);
        mSnippetLength = getConf().getInteger("serp.snippet_length", mSnippetLength);
        mTitleLength   = getConf().getInteger("serp.title_length", mTitleLength);
    }

    public SearchProvider()
    {
        this(null);
    }

    /**
     * Run a search based on given search fields.
     *
     * @param query search query
     * @param from first result to return
     * @param size number of results to return
     */
    public abstract void doSearch(String query, int from, int size);

    /**
     * Get Elasticsearch SearchResponse object for the current search.
     *
     * @return search response
     */
    protected abstract SearchResponse getResponse();

    /**
     * Return a list of {@link SearchResultBuilder.SearchResult} objects for the most recent search.
     * The list will be empty if search yielded no hits or no search has been performed.
     *
     * @return list of search results
     */
    public List<SearchResultBuilder.SearchResult> getResults()
    {
        ArrayList<SearchResultBuilder.SearchResult> results = new ArrayList<>();

        if (null == getResponse()) {
            return results;
        }

        for (SearchHit hit: getResponse().getHits()) {
            final Map<String, Object> source = hit.getSource();

            String snippet = "";
            if (null != hit.getHighlightFields().get("body_lang." + getSearchLanguage())) {
                final Text[] fragments = hit.getHighlightFields().get("body_lang." + getSearchLanguage()).fragments();
                if (1 >= fragments.length) {
                    snippet = fragments[0].string();
                }
            }

            // use meta description or first body part if no highlighted snippet available
            if (snippet.isEmpty()) {
                if (!source.get("meta_desc_lang." + getSearchLanguage()).toString().isEmpty()) {
                    snippet = StringEscapeUtils.escapeHtml(
                            truncateSnippet((String) source.get("meta_desc_lang." + getSearchLanguage()), mSnippetLength));
                } else {
                    snippet = StringEscapeUtils.escapeHtml(
                            truncateSnippet((String) source.get("body_lang." + getSearchLanguage()), mSnippetLength));
                }
            }
            snippet = TextCleanser.cleanseAll(snippet, true);

            // use highlighted title if available
            String title = StringEscapeUtils.escapeHtml(
                    truncateSnippet((String) source.get("title_lang." + getSearchLanguage()), mTitleLength));
            if (null != hit.getHighlightFields().get("title_lang." + getSearchLanguage())) {
                final Text[] fragments = hit.getHighlightFields().get("title_lang." + getSearchLanguage()).fragments();
                if (1 >= fragments.length) {
                    title = fragments[0].string();
                }
            }
            title = TextCleanser.cleanseAll(title, true);

            String targetPath = (String) source.get("warc_target_path");
            if (null != targetPath) {
                targetPath = Paths.get("/", targetPath).normalize().toString();
            } else {
                targetPath = "/";
            }

            final SearchResultBuilder.SearchResult result = new SearchResultBuilder()
                    .score(hit.getScore())
                    .index(hit.getIndex())
                    .documentId(hit.getId())
                    .trecId((String) source.get("warc_trec_id"))
                    .title(title)
                    .targetHostname((String) source.get("warc_target_hostname"))
                    .targetPath(targetPath)
                    .targetUri((String) source.get("warc_target_uri"))
                    .snippet(snippet)
                    .fullBody((String) source.get("body_lang." + getSearchLanguage()))
                    .pageRank((Double) source.get("page_rank"))
                    .spamRank((Integer) source.get("spam_rank"))
                    .explanation(hit.getExplanation())
                    .build();
            results.add(result);
        }

        return results;
    }

    /**
     * @return the total number of results found for the last search request.
     */
    public long getTotalResultNumber()
    {
        return getResponse().getHits().getTotalHits();
    }

    /**
     * @return length of body snippets
     */
    public int getSnippetLength()
    {
        return mSnippetLength;
    }

    /**
     * @param snippetLength length of body snippets
     */
    public void setSnippetLength(int snippetLength)
    {
        mSnippetLength = Math.min(0, snippetLength);
    }

    /**
     * @return length of returned result titles
     */
    public int getTitleLength()
    {
        return mTitleLength;
    }

    /**
     * @param titleLength length of returned result titles
     */
    public void setTitleLength(int titleLength)
    {
        mTitleLength = Math.min(0, titleLength);
    }

    /**
     * @return current search language (two-characters language code)
     */
    public String getSearchLanguage()
    {
        return mSearchLanguage;
    }

    /**
     * @param language current search language (two-characters language code)
     */
    public void setSearchLanguage(String language)
    {
        mSearchLanguage = language.length() == 2 ? language.toLowerCase() : "en";
    }

    /**
     * @return whether to group results by hostname
     */
    public boolean isGroupByHostname()
    {
        return mGroupByHostname;
    }

    /**
     * @param groupByHostname whether to group results by hostname
     */
    public void setGroupByHostname(boolean groupByHostname)
    {
        mGroupByHostname = groupByHostname;
    }

    /**
     * Helper function to localize field names according to the current search language.
     *
     * @param fieldName field name with language placehoders
     * @return localized field name
     */
    protected String replaceLocalePlaceholders(String fieldName) {
        return fieldName.replace("%lang%", mSearchLanguage);
    }

    /**
     * Truncate a snippet after a certain number of characters, trying to preserve full words.
     * Will cut the string hard after the specified amount of characters if no spaces could be
     * found or cutting after words would reduce the size more than 2/3 of the desired length.
     *
     * @param snippet the snippet
     * @param numCharacters number of characters after which to truncate
     * @return the truncated snippet
     */
    protected String truncateSnippet(String snippet, final int numCharacters)
    {
        if (null == snippet) {
            return "";
        }

        if (snippet.length() > numCharacters) {
            final boolean wordEnded = (snippet.charAt(numCharacters) == ' ');
            snippet = snippet.substring(0, numCharacters);

            // get rid of incomplete words
            final int pos = snippet.lastIndexOf(' ');
            if (!wordEnded && -1 != pos) {
                // shorten snippet if it doesn't become too short then
                if ((int)(.6 * numCharacters) <= pos) {
                    snippet = snippet.substring(0, pos);
                }
            }
        }

        return snippet.trim();
    }

    /**
     * Group (bucket) results by host name, but preserve ranking order of first
     * element in a group as well as the order within a group.
     *
     * @param results ungrouped ordered search results
     * @return grouped ordered search results
     */
    protected List<SearchResultBuilder.SearchResult> groupResults(List<SearchResultBuilder.SearchResult> results)
    {
        // use ordered hash map for bucketing
        LinkedHashMap<String, List<SearchResultBuilder.SearchResult>> buckets = new LinkedHashMap<>();
        for (SearchResultBuilder.SearchResult result: results) {
            boolean suggestGrouping = true;

            // first element in a group
            if (!buckets.containsKey(result.targetHostname())) {
                buckets.put(result.targetHostname(), new ArrayList<>());
                suggestGrouping = false;
            }

            result.setGroupingSuggested(suggestGrouping);
            buckets.get(result.targetHostname()).add(result);
        }

        // serialize buckets
        List<SearchResultBuilder.SearchResult> groupedResults = new ArrayList<>();
        for (String bucketKey: buckets.keySet()) {
            // suggest "more from this host" for the last result in a bucket
            List<SearchResultBuilder.SearchResult> b = buckets.get(bucketKey);
            if (b.size() > 1) {
                b.get(b.size() - 1).setMoreSuggested(true);
            }

            groupedResults.addAll(b);
        }

        return groupedResults;
    }
}
