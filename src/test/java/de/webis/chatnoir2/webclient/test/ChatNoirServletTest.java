/*
 * ChatNoir 2 Web frontend test suite.
 *
 * Copyright (C) 2014 Webis Group @ Bauhaus University Weimar
 * Main Contributor: Janek Bevendorff
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
