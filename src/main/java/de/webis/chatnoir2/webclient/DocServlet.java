/*
 * ChatNoir 2 Web frontend.
 *
 * Copyright (C) 2014-2017 Webis Group @ Bauhaus University Weimar
 * Main Contributor: Janek Bevendorff
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
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.collect.Tuple;
import org.elasticsearch.common.xcontent.XContentHelper;
import org.elasticsearch.common.xcontent.XContentType;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
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
     * Default Mustache template.
     */
    private static final String TEMPLATE_INDEX = "/templates/chatnoir2-docs.mustache";

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException
    {
        Path requestURI = Paths.get(request.getRequestURI()).normalize();

        String docName = "index";
        if (requestURI.getNameCount() < 1) {
            forwardError(request, response, HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        if (requestURI.getNameCount() >= 2) {
            docName = requestURI.getName(1).toString();
        }

        Path markdownFilePath = Paths.get(getServletContext().getRealPath("/docs/" + docName + ".md"));
        if (null == markdownFilePath || !Files.exists(markdownFilePath)) {
            forwardError(request, response, HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        final StringBuilder contents = new StringBuilder();
        try (Stream<String> stream = Files.lines(markdownFilePath)) {
            stream.forEach(l -> contents.append(l).append("\n"));
        }

        String[] contentSplit = contents.toString().split("(?:^|\n)---\n", 3);

        if (contentSplit.length != 3) {
            forwardError(request, response, HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        contentSplit[1] = "---\n" + contentSplit[1];

        Tuple<XContentType, Map<String, Object>> xContent = XContentHelper
                .convertToMap(new BytesArray(contentSplit[1].getBytes()), false, XContentType.YAML);
        Map<String, Object> docParams = xContent.v2();

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
        docParams.put("content", html);
        docParams.put("isIndex", docName.equals("index"));

        Renderer.render(getServletContext(), request, response, TEMPLATE_INDEX, docParams);
    }
}
