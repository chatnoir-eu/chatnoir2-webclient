package de.webis.chatnoir2.webclient.util;

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
                .field("value", explanation.getValue())
                .field("description", explanation.getDescription());

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
