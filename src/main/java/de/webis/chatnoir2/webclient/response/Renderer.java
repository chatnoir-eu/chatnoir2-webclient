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

        File maintenanceFile = new File("/etc/chatnoir2/maintenance");
        if (maintenanceFile.exists()) {
            vars.put("maintenance", "true");
        }

        String year = String.valueOf(Calendar.getInstance().get(Calendar.YEAR));
        if (!year.equals("2017")) {
            year = "2017-" + year;
        }
        vars.put("copyrightYear", year);

        m.execute(new PrintWriter(response.getOutputStream()), newScopes).flush();
    }
}
