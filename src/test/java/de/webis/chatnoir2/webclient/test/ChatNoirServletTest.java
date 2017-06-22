/*
 * ChatNoir 2 Web Frontend Test Suite.
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

package de.webis.chatnoir2.webclient.test;

import de.webis.chatnoir2.webclient.ChatNoirServlet;
import org.junit.runner.RunWith;
import org.junit.Test;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import javax.servlet.http.HttpServletRequest;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ChatNoirServletTest
{
    @Spy
    private ChatNoirServlet mServlet;

    @Test
    public void testGetStrippedRequestURI()
    {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/test/");
        when(request.getContextPath()).thenReturn("");

        assertThat(ChatNoirServlet.getStrippedRequestURI(request), is("/test/"));
    }

    @Test
    public void testGetStrippedRequestURINonRootContext()
    {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/test/");
        when(request.getContextPath()).thenReturn("/test");

        assertThat(ChatNoirServlet.getStrippedRequestURI(request), is("/"));

        when(request.getRequestURI()).thenReturn("/test/foo");
        when(request.getContextPath()).thenReturn("/test");
        assertThat(ChatNoirServlet.getStrippedRequestURI(request), is("/foo"));
    }

    @Test
    public void testIsForwardedFrom()
    {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getContextPath()).thenReturn("");

        assertThat(mServlet.isForwardedForm(request, "/test"), is(false));

        when(request.getAttribute("javax.servlet.forward.request_uri")).thenReturn("/test");
        assertFalse(mServlet.isForwardedForm(request, "/xyz"));
        assertTrue(mServlet.isForwardedForm(request, "/test"));

        verify(request, atLeastOnce()).getAttribute("javax.servlet.forward.request_uri");
    }

    @Test
    public void testIsForwardedFromNonRootContext()
    {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getContextPath()).thenReturn("/test");

        assertThat(mServlet.isForwardedForm(request, "/test"), is(false));

        when(request.getAttribute("javax.servlet.forward.request_uri")).thenReturn("/test/test");
        assertFalse(mServlet.isForwardedForm(request, "/xyz"));
        assertTrue(mServlet.isForwardedForm(request, "/test"));

        verify(request, atLeastOnce()).getAttribute("javax.servlet.forward.request_uri");
    }
}
