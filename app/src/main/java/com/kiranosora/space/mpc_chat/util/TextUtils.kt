package com.kiranosora.space.mpc_chat.util

import android.text.Html
import android.text.Spanned
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.ast.Node
import com.vladsch.flexmark.util.data.MutableDataSet

class TextUtils {
    companion object{
        val options = MutableDataSet()

        // uncomment to set optional extensions
        //options.set(Parser.EXTENSIONS, Arrays.asList(TablesExtension.create(), StrikethroughExtension.create()));

        // uncomment to convert soft-breaks to hard breaks
        //options.set(HtmlRenderer.SOFT_BREAK, "<br />\n");

        val parser = Parser.builder(options).build()
        val renderer = HtmlRenderer.builder(options).build()
        fun markDown2Text(markDown: String): Spanned? {
            // You can re-use parser and renderer instances
            val document = parser.parse(markDown)
            val html = renderer.render(document)
            return Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT)
        }
    }
}