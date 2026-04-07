package com.mycelium.wallet.domain

/**
 * Interface for platform-specific sharing and clipboard operations. Provides methods to share text content, share files, copy to clipboard, and paste from clipboard across different platforms.
 */
interface ShareProvider {
    fun shareText(text: String, subject: String? = null)
    fun shareFile(filePath: String, title: String = "Wallet Logs"): Boolean

    /**
     * Creates a file with the given content and shares it.
     * @param fileName The name of the file to create
     * @param content The content to write to the file
     * @param title The title for the share dialog
     * @param mimeType Optional MIME type (default: text/plain)
     * @return true if sharing was successful
     */
    fun shareFileContent(
        fileName: String,
        content: String,
        title: String,
        mimeType: String = "text/plain"
    ): Boolean

    fun copyToClipboard(text: String, label: String? = null)
    fun pasteFromClipboard(): String?
    fun openUrl(url: String): Boolean
}
