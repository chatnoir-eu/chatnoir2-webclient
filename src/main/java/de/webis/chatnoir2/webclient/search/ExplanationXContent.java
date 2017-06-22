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

package de.webis.chatnoir2.webclient.search;

import org.apache.lucene.search.Explanation;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;

import java.io.IOException;

/**
 * XContent wrapper for Lucene Explanation objects.
 */
public class ExplanationXContent implements ToXContent
{
    private final Explanation mExplanation;

    public ExplanationXContent(Explanation explanation)
    {
        mExplanation = explanation;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException
    {
        return toXContent(builder, params, mExplanation);
    }

    private XContentBuilder toXContent(XContentBuilder builder, Params params, Explanation explanation) throws IOException
    {
        if (null == explanation) {
            builder.nullValue();
            return builder;
        }

        try {
            builder.startObject()
                .field("description", explanation.getDescription())
                .field("value", explanation.getValue());

                Explanation[] details = explanation.getDetails();
                if (details.length > 0) {
                    builder.startArray("details");
                        for (Explanation detail : details) {
                            toXContent(builder, params, detail);
                        }
                    builder.endArray();
                }
            builder.endObject();

            return builder;
        } catch (IOException e) {
            return null;
        }
    }
}
