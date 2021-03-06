package io.legado.app.help

import io.legado.app.App
import io.legado.app.constant.PreferKey
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.ReplaceRule
import io.legado.app.utils.MD5Utils
import io.legado.app.utils.getPrefInt
import io.legado.app.utils.getPrefString
import org.apache.commons.text.similarity.JaccardSimilarity
import java.io.File
import kotlin.math.min

object BookHelp {

    private var downloadPath: String =
        App.INSTANCE.getPrefString(PreferKey.downloadPath)
            ?: App.INSTANCE.getExternalFilesDir(null)?.absolutePath
            ?: App.INSTANCE.cacheDir.absolutePath

    fun upDownloadPath() {
        downloadPath =
            App.INSTANCE.getPrefString(PreferKey.downloadPath)
                ?: App.INSTANCE.getExternalFilesDir(null)?.absolutePath
                        ?: App.INSTANCE.cacheDir.absolutePath
    }

    private fun getBookCachePath(): String {
        return "$downloadPath${File.separator}book_cache"
    }

    fun clearCache() {
        FileHelp.deleteFile(getBookCachePath())
        FileHelp.getFolder(getBookCachePath())
    }

    @Synchronized
    fun saveContent(book: Book, bookChapter: BookChapter, content: String) {
        if (content.isEmpty()) return
        FileHelp.getFolder(getBookFolder(book)).listFiles()?.forEach {
            if (it.name.startsWith(String.format("%05d", bookChapter.index))) {
                it.delete()
                return@forEach
            }
        }
        val filePath = getChapterPath(book, bookChapter)
        val file = FileHelp.getFile(filePath)
        file.writeText(content)
    }

    fun getChapterCount(book: Book): Int {
        return FileHelp.getFolder(getBookFolder(book)).list()?.size ?: 0
    }

    fun hasContent(book: Book, bookChapter: BookChapter): Boolean {
        val filePath = getChapterPath(book, bookChapter)
        runCatching {
            val file = File(filePath)
            if (file.exists()) {
                return true
            }
        }
        return false
    }

    fun getContent(book: Book, bookChapter: BookChapter): String? {
        val filePath = getChapterPath(book, bookChapter)
        runCatching {
            val file = File(filePath)
            if (file.exists()) {
                return file.readText()
            }
        }
        return null
    }

    fun delContent(book: Book, bookChapter: BookChapter) {
        val filePath = getChapterPath(book, bookChapter)
        kotlin.runCatching {
            val file = File(filePath)
            if (file.exists()) {
                file.delete()
            }
        }
    }

    private fun getBookFolder(book: Book): String {
        val bookFolder = formatFolderName(book.name + MD5Utils.md5Encode16(book.bookUrl))
        return "${getBookCachePath()}${File.separator}$bookFolder"
    }

    private fun getChapterPath(book: Book, bookChapter: BookChapter): String {
        val chapterFile =
            String.format("%05d-%s", bookChapter.index, MD5Utils.md5Encode(bookChapter.title))
        return "${getBookFolder(book)}${File.separator}$chapterFile.nb"
    }

    private fun formatFolderName(folderName: String): String {
        return folderName.replace("[\\\\/:*?\"<>|.]".toRegex(), "")
    }

    fun formatAuthor(author: String?): String {
        return author
            ?.replace("作\\s*者[\\s:：]*".toRegex(), "")
            ?.replace("\\s+".toRegex(), " ")
            ?.trim { it <= ' ' }
            ?: ""
    }

    /**
     * 找到相似度最高的章节
     */
    fun getDurChapterIndexByChapterTitle(
        title: String?,
        index: Int,
        chapters: List<BookChapter>
    ): Int {
        if (title.isNullOrEmpty()) {
            return min(index, chapters.lastIndex)
        }
        if (chapters.size > index && title == chapters[index].title) {
            return index
        }

        var newIndex = 0
        val jaccardSimilarity = JaccardSimilarity()
        var similarity = if (chapters.size > index) {
            jaccardSimilarity.apply(title, chapters[index].title)
        } else 0.0
        if (similarity == 1.0) {
            return index
        } else {
            for (i in 1..50) {
                if (index - i in chapters.indices) {
                    jaccardSimilarity.apply(title, chapters[index - i].title).let {
                        if (it > similarity) {
                            similarity = it
                            newIndex = index - i
                            if (similarity == 1.0) {
                                return newIndex
                            }
                        }
                    }
                }
                if (index + i in chapters.indices) {
                    jaccardSimilarity.apply(title, chapters[index + i].title).let {
                        if (it > similarity) {
                            similarity = it
                            newIndex = index + i
                            if (similarity == 1.0) {
                                return newIndex
                            }
                        }
                    }
                }
            }
        }
        return newIndex
    }

    var bookName: String? = null
    var bookOrigin: String? = null
    var replaceRules: List<ReplaceRule> = arrayListOf()

    fun disposeContent(
        title: String,
        name: String,
        origin: String?,
        content: String,
        enableReplace: Boolean
    ): String {
        var c = content
        synchronized(this) {
            if (enableReplace && (bookName != name || bookOrigin != origin)) {
                replaceRules = if (origin.isNullOrEmpty()) {
                    App.db.replaceRuleDao().findEnabledByScope(name)
                } else {
                    App.db.replaceRuleDao().findEnabledByScope(name, origin)
                }
            }
        }
        if (!content.substringBefore("\n").contains(title)) {
            c = title + "\n" + c
        }
        for (item in replaceRules) {
            item.pattern.let {
                if (it.isNotEmpty()) {
                    c = if (item.isRegex) {
                        c.replace(it.toRegex(), item.replacement)
                    } else {
                        c.replace(it, item.replacement)
                    }
                }
            }
        }
        val indent = App.INSTANCE.getPrefInt("textIndent", 2)
        return c.replace("\\s*\\n+\\s*".toRegex(), "\n" + "　".repeat(indent))
    }
}