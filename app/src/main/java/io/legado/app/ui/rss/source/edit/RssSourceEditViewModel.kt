package io.legado.app.ui.rss.source.edit

import android.app.Application
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import io.legado.app.App
import io.legado.app.base.BaseViewModel
import io.legado.app.data.entities.RssSource
import io.legado.app.utils.GSON
import io.legado.app.utils.fromJsonObject
import kotlinx.coroutines.Dispatchers

class RssSourceEditViewModel(application: Application) : BaseViewModel(application) {

    var rssSource: RssSource? = null
    private var oldSourceUrl: String? = null

    fun initData(intent: Intent, onFinally: () -> Unit) {
        execute {
            val key = intent.getStringExtra("data")
            var source: RssSource? = null
            if (key != null) {
                source = App.db.rssSourceDao().getByKey(key)
            }
            source?.let {
                oldSourceUrl = it.sourceUrl
                rssSource = it
            }
        }.onFinally {
            onFinally()
        }
    }

    fun save(source: RssSource, success: (() -> Unit)) {
        execute {
            oldSourceUrl?.let {
                if (oldSourceUrl != source.sourceUrl) {
                    App.db.rssSourceDao().delete(it)
                }
            }
            oldSourceUrl = source.sourceUrl
            App.db.rssSourceDao().insert(source)
            rssSource = source
        }.onSuccess {
            success()
        }.onError {
            toast(it.localizedMessage)
            it.printStackTrace()
        }
    }

    fun pasteSource(onSuccess: (source: RssSource) -> Unit) {
        execute(context = Dispatchers.Main) {
            var source: RssSource? = null
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
            clipboard?.primaryClip?.let {
                if (it.itemCount > 0) {
                    val json = it.getItemAt(0).text.toString().trim()
                    source = GSON.fromJsonObject<RssSource>(json)
                }
            }
            source
        }.onError {
            toast(it.localizedMessage)
        }.onSuccess {
            if (it != null) {
                onSuccess(it)
            } else {
                toast("格式不对")
            }
        }
    }
}