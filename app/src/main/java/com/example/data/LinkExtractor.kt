package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.io.IOException

class LinkExtractor {

    /**
     * Scrapes a third party aggregator server (e.g. WarezCDN or Vidsrc) using Jsoup
     * to resolve a direct HLS playback stream (.m3u8) for the given TMDB ID.
     */
    suspend fun extractM3u8Stream(tmdbId: String, aggregatorBaseUrl: String): String = withContext(Dispatchers.IO) {
        val cleanBaseUrl = aggregatorBaseUrl.trimEnd('/')
        
        // Assemble candidate query URL structures based on different aggregators
        val targetUrls = listOf(
            "$cleanBaseUrl/filme/$tmdbId",
            "$cleanBaseUrl/embed/movie?tmdb=$tmdbId",
            "$cleanBaseUrl?tmdb=$tmdbId"
        )
        
        var lastException: Exception? = null

        for (targetUrl in targetUrls) {
            try {
                val connection = Jsoup.connect(targetUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .timeout(6000)
                    .followRedirects(true)

                val document = connection.get()
                
                // Scan primary script elements of the body
                val scripts = document.select("script")
                for (script in scripts) {
                    val scriptContent = script.html()
                    val m3u8Match = findM3u8InString(scriptContent)
                    if (m3u8Match != null) return@withContext m3u8Match
                }

                // Check standard source and video tags
                val videoSources = document.select("source, video")
                for (source in videoSources) {
                    val src = source.attr("src")
                    if (src.contains(".m3u8")) {
                        return@withContext src
                    }
                }

                // Crawl nested iframe nodes
                val iframes = document.select("iframe")
                for (iframe in iframes) {
                    val iframeSrc = iframe.attr("src")
                    if (iframeSrc.isNotEmpty() && !iframeSrc.startsWith("about:")) {
                        val simpleMatch = findM3u8InString(iframeSrc)
                        if (simpleMatch != null) return@withContext simpleMatch

                        // Follow iframe embed source contents
                        try {
                            val iframeConnectUrl = if (iframeSrc.startsWith("//")) "https:$iframeSrc" else iframeSrc
                            val iframeDoc = Jsoup.connect(iframeConnectUrl)
                                .userAgent("Mozilla/5.0 (Android; Mobile)")
                                .timeout(3000)
                                .ignoreContentType(true)
                                .get()

                            val nestedScripts = iframeDoc.select("script")
                            for (nestedScript in nestedScripts) {
                                val content = nestedScript.html()
                                val match = findM3u8InString(content)
                                if (match != null) return@withContext match
                            }

                            val nestedSources = iframeDoc.select("source")
                            for (nSrc in nestedSources) {
                                val srcVal = nSrc.attr("src")
                                if (srcVal.contains(".m3u8")) {
                                    return@withContext srcVal
                                }
                            }
                        } catch (e: Exception) {
                            // Continue checking alternatives
                        }
                    }
                }

                // Fallback to searching the whole HTML document for matched strings
                val matchedStr = findM3u8InString(document.html())
                if (matchedStr != null) return@withContext matchedStr

            } catch (e: Exception) {
                lastException = e
            }
        }

        throw lastException ?: IOException("Nenhuma fonte de streaming encontrada no agregador para o TMDB ID: $tmdbId")
    }

    private fun findM3u8InString(text: String): String? {
        val regex = Regex("""https?://[^\s"'<>]+?\.m3u8[^\s"'<>]*""")
        return regex.find(text)?.value
    }
}
