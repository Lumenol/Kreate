package me.knighthat.utils

import app.kreate.android.BuildConfig

object Repository {

    const val GITHUB = "https://github.com"
    const val GITHUB_API = "https://api.github.com"

    const val OWNER = "knighthat"
    const val REPO = "$OWNER/${BuildConfig.APP_NAME}"
    const val REPO_URL = "$GITHUB/$REPO"

    const val LATEST_TAG_URL = "$REPO/releases/latest"

    const val ISSUE_TEMPLATE_PATH = "/issues"

    /** Where this fork's bugs and ideas go; updates still come from [REPO] */
    const val ISSUES_URL = "$GITHUB/Lumenol/Kreate/issues"

    /** Longest crash log put in an issue link: GitHub fails on longer links (its sign-in page breaks past about 4 000 characters) */
    private const val MAX_LOG_IN_URL = 2_500

    /**
     * New issue already filled: title from the crash's root cause, the log in the body.
     * A longer log is cut, the whole one being in the clipboard to paste.
     */
    fun crashIssueUrl( log: String ): String {
        val rootCause = log.lines().lastOrNull { it.startsWith( "Caused by: " ) }?.removePrefix( "Caused by: " )
            ?: log.lines().firstOrNull { it.isNotBlank() && !it.startsWith( "\t" ) && ("Exception" in it || "Error" in it) }
            ?: "Crash"
        val cut = log.length > MAX_LOG_IN_URL
        val body = buildString {
            appendLine( "**Ce que je faisais :** " )
            appendLine()
            appendLine( "<details><summary>Journal de crash</summary>" )
            appendLine()
            appendLine( "```" )
            appendLine( if( cut ) log.take( MAX_LOG_IN_URL ) + "\n[…]" else log )
            appendLine( "```" )
            appendLine( "</details>" )
            if( cut ) {
                appendLine()
                appendLine( "_Journal coupé : le journal complet est dans le presse-papiers, colle-le en commentaire._" )
            }
        }
        return "$ISSUES_URL/new?title=${android.net.Uri.encode( "Crash : " + rootCause.take( 120 ) )}&body=${android.net.Uri.encode( body )}"
    }
    const val FEATURE_REQUEST_TEMPLATE_PATH = "/issues/new?assignees=&labels=feature_request&template=feature_request.yaml"
}