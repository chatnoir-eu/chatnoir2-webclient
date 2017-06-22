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

package de.webis.chatnoir2.webclient.api.exceptions;

import java.util.Arrays;
import java.util.List;

/**
 * Exception to be thrown when a user requests an unsupported method.
 */
public class MethodNotAllowedException extends UserErrorException
{
    private final List<String> mAllowedMethods;

    /**
     * @param message custom error message
     * @param allowedMethods allowed HTTP methods (GET and HEAD must always be
     *                       allowed and are automatically added if missing)
     */
    public MethodNotAllowedException(String message, String... allowedMethods)
    {
        super(message);
        mAllowedMethods = Arrays.asList(allowedMethods);
        if (!mAllowedMethods.contains("GET") && !mAllowedMethods.contains("get")) {
            mAllowedMethods.add("GET");
        }
        if (!mAllowedMethods.contains("HEAD") && !mAllowedMethods.contains("head")) {
            mAllowedMethods.add("HEAD");
        }
    }

    public List<String> getAllowedMethods()
    {
        return mAllowedMethods;
    }
}
