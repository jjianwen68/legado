package io.legado.app.ui.chapterlist

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.VMBaseFragment
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.lib.theme.backgroundColor
import io.legado.app.utils.getViewModelOfActivity
import kotlinx.android.synthetic.main.fragment_chapter_list.*
import org.jetbrains.anko.sdk27.listeners.onClick

class ChapterListFragment : VMBaseFragment<ChapterListViewModel>(R.layout.fragment_chapter_list),
    ChapterListAdapter.Callback,
    ChapterListViewModel.CallBack {
    override val viewModel: ChapterListViewModel
        get() = getViewModelOfActivity(ChapterListViewModel::class.java)

    lateinit var adapter: ChapterListAdapter
    private var chapterData: LiveData<List<BookChapter>>? = null
    private var durChapterIndex = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRecyclerView()
        initView()
        initData()
    }

    private fun initRecyclerView() {
        adapter = ChapterListAdapter(requireContext(), this)
        recycler_view.layoutManager = LinearLayoutManager(requireContext())
        recycler_view.addItemDecoration(
            DividerItemDecoration(
                requireContext(),
                LinearLayout.VERTICAL
            )
        )
        recycler_view.adapter = adapter
    }

    private fun initData() {
        viewModel.bookUrl?.let { bookUrl ->
            App.db.bookChapterDao().observeByBook(bookUrl).observe(viewLifecycleOwner, Observer {
                adapter.setItems(it)
                viewModel.book?.let { book ->
                    durChapterIndex = book.durChapterIndex
                    tv_current_chapter_info.text = book.durChapterTitle
                    recycler_view.scrollToPosition(durChapterIndex)
                }
            })
        }
    }

    private fun initView() {
        ll_chapter_base_info.setBackgroundColor(backgroundColor)
        iv_chapter_top.onClick { recycler_view.scrollToPosition(0) }
        iv_chapter_bottom.onClick {
            if (adapter.itemCount > 0) {
                recycler_view.scrollToPosition(adapter.itemCount - 1)
            }
        }
        tv_current_chapter_info.onClick {
            viewModel.book?.let {
                recycler_view.scrollToPosition(it.durChapterIndex)
            }
        }
        viewModel.callBack = this
    }

    override fun startSearch(newText: String?) {
        chapterData?.removeObservers(this)
        if (newText.isNullOrBlank()) {
            initData()
        } else {
            chapterData = App.db.bookChapterDao().liveDataSearch(newText)
            chapterData?.observe(viewLifecycleOwner, Observer {
                adapter.setItems(it)
            })
            recycler_view.scrollToPosition(0)
        }
    }

    override fun durChapterIndex(): Int {
        return durChapterIndex
    }

    override fun openChapter(bookChapter: BookChapter) {
        activity?.setResult(RESULT_OK, Intent().putExtra("index", bookChapter.index))
        activity?.finish()
    }

    override fun book(): Book? {
        return viewModel.book
    }
}