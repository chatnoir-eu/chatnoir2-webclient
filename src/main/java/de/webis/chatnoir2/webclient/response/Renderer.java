/*
 * ChatNoir 2 Web frontend.
 *
 * Copyright (C) 2014-2017 Webis Group @ Bauhaus University Weimar
 * Main Contributor: Janek Bevendorff
 */

package de.webis.chatnoir2.webclient.response;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import de.webis.chatnoir2.webclient.ChatNoirServlet;

public class Renderer
{
    private static final String MIME_TYPE_TEXT_HTML_CHARSET_UTF8 = "text/html; charset=UTF-8";
    
    public static void render(final ServletContext context, HttpServletRequest request,
                              HttpServletResponse response, String template, Object... scopes) throws IOException
    {
        File f             = new File(context.getRealPath(template));
        File resourceBase  = f.getParentFile().getParentFile();
        MustacheFactory mf = new DefaultMustacheFactory(resourceBase);
        Mustache m         = mf.compile(template);
        response.setContentType(MIME_TYPE_TEXT_HTML_CHARSET_UTF8);

        // add global default template variables
        Map<String, String> vars = new HashMap<>();
        vars.put("contextPath", request.getContextPath());
        vars.put("requestUri", ChatNoirServlet.getStrippedRequestURI(request));
        Object[] newScopes = new Object[scopes.length + 1];
        System.arraycopy(scopes, 0, newScopes, 1, scopes.length);
        newScopes[0] = vars;

        String year = String.valueOf(Calendar.getInstance().get(Calendar.YEAR));
        if (!year.equals("2017")) {
            year = "2017-" + year;
        }
        vars.put("copyrightYear", year);

        m.execute(new PrintWriter(response.getOutputStream()), newScopes).flush();
    }
}
