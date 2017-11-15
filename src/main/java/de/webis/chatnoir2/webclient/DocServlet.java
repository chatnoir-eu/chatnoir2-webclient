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

package de.webis.chatnoir2.webclient;

import com.vladsch.flexmark.ast.Node;
import com.vladsch.flexmark.ext.anchorlink.AnchorLinkExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.parser.ParserEmulationProfile;
import com.vladsch.flexmark.util.options.MutableDataSet;
import de.webis.chatnoir2.webclient.response.Renderer;
import de.webis.chatnoir2.webclient.util.CacheManager;
import org.apache.shiro.cache.Cache;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.collect.Tuple;
import org.elasticsearch.common.xcontent.XContentHelper;
import org.elasticsearch.common.xcontent.XContentType;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

/**
 * Documentation Servlet for Chatnoir 2.
 */
@WebServlet(DocServlet.ROUTE)
public class DocServlet extends ChatNoirServlet
{
    /**
     * URL Routing for this servlet.
     */
    public static final String ROUTE = "/doc/*";

    /**
     * Name of EH document cache.
     */
    private static final String CACHE_NAME = DocServlet.class.getName() + "-0-docs";

    /**
     * Default Mustache template.
     */
    private static final String TEMPLATE_INDEX = "/templates/chatnoir2-docs.mustache";

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException
    {
        Path requestURI = Paths.get(getStrippedRequestURI(request)).normalize();

        if (requestURI.getNameCount() < 1) {
            forwardError(request, response, HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        if (!request.getRequestURI().endsWith("/")) {
            redirect(request, response, request.getRequestURI() + "/");
            return;
        }

        if (requestURI.getNameCount() >= 2) {
            requestURI = requestURI.subpath(1, requestURI.getNameCount());
        } else {
            requestURI = Paths.get("");
        }
        Map<String, Object> docParams = getDocument(requestURI, request);
        if (null == docParams) {
            forwardError(request, response, HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        Renderer.render(getServletContext(), request, response, TEMPLATE_INDEX, docParams);
    }

    /**
     * Parse and return document with the given name. Returns a map with a "content" key for the parsed contents
     * and further parameters from the YAML front matter, which can be passed to the template.
     *
     * Parsed documents are cached.
     *
     * @param docPath document path
     * @param request HTTP request for URL rewriting
     * @return map containing parsed document and meta data, null if document does not exist
     */
    private Map<String, Object> getDocument(Path docPath, HttpServletRequest request) throws IOException
    {
        String docName = docPath.toString();
        if (docName.isEmpty() || Files.isDirectory(Paths.get(getServletContext().getRealPath("/docs/" + docName)))) {
            docName += "/index";
        }

        // try to get parsed document from cache
        CacheManager cacheManager = new CacheManager();
        Cache<String, Map<String, Object>> cache = cacheManager.getCache(CACHE_NAME);
        Map<String, Object> docParams = cache.get(docName);
        if (null != docParams) {
            return docParams;
        }

        Path markdownFilePath = Paths.get(getServletContext().getRealPath("/docs/" + docName + ".md"));
        if (null == markdownFilePath || !Files.exists(markdownFilePath)) {
            return null;
        }

        final StringBuilder contents = new StringBuilder();
        try (Stream<String> stream = Files.lines(markdownFilePath)) {
            stream.forEach(l -> contents.append(l).append("\n"));
        }

        String[] contentSplit = contents.toString().split("(?:^|\n)---\n", 3);

        if (contentSplit.length != 3) {
            return null;
        }

        contentSplit[1] = "---\n" + contentSplit[1];

        Tuple<XContentType, Map<String, Object>> xContent = XContentHelper
                .convertToMap(new BytesArray(contentSplit[1].getBytes()), false, XContentType.YAML);
        docParams = xContent.v2();
        docParams.computeIfAbsent("breadcrumbs", k -> new ArrayList<>());

        // generate breadcrumbs
        ArrayList<Object> breadcrumbs = new ArrayList<>();
        if (!docName.equals("/index")) {
            Map<String, String> entry = new HashMap<>();
            entry.put("path", "");
            entry.put("title", "ChatNoir Documentation");
            breadcrumbs.add(entry);
        }
        Collection breadCrumbsRaw = (Collection) docParams.get("breadcrumbs");
        if (null != breadCrumbsRaw) {
            breadcrumbs.addAll(breadCrumbsRaw);
        }
        int size = breadcrumbs.size();
        StringBuilder breadcrumbPaths = new StringBuilder();
        for (int i = 1; i < size; ++i) {
            if (i - 1 < docPath.getNameCount() ) {
                breadcrumbPaths.append(docPath.getName(i - 1)).append("/");
            }
            Map<String, String> entry = new HashMap<>();
            entry.put("path", breadcrumbPaths.toString());
            entry.put("title", (String) breadcrumbs.get(i));
            breadcrumbs.add(i, entry);
            breadcrumbs.remove(i + 1);
        }
        docParams.put("breadcrumbs", breadcrumbs);

        MutableDataSet options = new MutableDataSet();
        options.setFrom(ParserEmulationProfile.MARKDOWN);
        options.set(Parser.EXTENSIONS, Arrays.asList(
                AnchorLinkExtension.create(),
                TablesExtension.create()
        ));
        Parser markdownParser = Parser.builder(options).build();
        HtmlRenderer htmlRenderer = HtmlRenderer.builder(options).build();

        Node document = markdownParser.parse(contentSplit[2]);
        String html = htmlRenderer.render(document);

        // rewrite links to match servlet context
        Document htmlDoc = Jsoup.parse(html);
        Elements links = htmlDoc.select("[href]");
        for (Element link: links) {
            if (link.attr("href").startsWith("/")) {
                link.attr("href", request.getContextPath() + link.attr("href"));
            }
        }

        docParams.put("content", htmlDoc.toString());
        docParams.put("isIndex", docName.equals("/index"));

        // cache document
        cache.put(docName, docParams);

        return docParams;
    }
}
