/*
 * ChatNoir 2 Web frontend.
 *
 * Copyright (C) 2014 Webis Group @ Bauhaus University Weimar
 * Main Contributor: Janek Bevendorff
 */

package de.webis.chatnoir2.webclient.response;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

public class Renderer
{
    private static final String MIME_TYPE_TEXT_HTML_CHARSET_UTF8 = "text/html; charset=UTF-8";
    
    public static void render(final ServletContext context, HttpServletRequest req, HttpServletResponse resp, String template, Object... scopes) throws IOException
    {
        File f             = new File(context.getRealPath(template));
        File resourceBase  = f.getParentFile().getParentFile();
        MustacheFactory mf = new DefaultMustacheFactory(resourceBase);
        Mustache m         = mf.compile(template);
        resp.setContentType(MIME_TYPE_TEXT_HTML_CHARSET_UTF8);
        m.execute(new PrintWriter(resp.getOutputStream()), scopes).flush();
    }
}
