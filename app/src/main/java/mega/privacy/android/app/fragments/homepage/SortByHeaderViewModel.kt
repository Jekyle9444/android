package mega.privacy.android.app.fragments.homepage

import android.content.Context
import android.content.Intent
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import com.jeremyliao.liveeventbus.LiveEventBus
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.R
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.EVENT_LIST_GRID_CHANGE
import mega.privacy.android.app.utils.Constants.EVENT_ORDER_CHANGE
import nz.mega.sdk.MegaApiJava.*

class SortByHeaderViewModel @ViewModelInject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    // Pair<Int, Int>: First is order Cloud, second order Others (Incoming root)
    var order = Pair(ORDER_DEFAULT_ASC, ORDER_DEFAULT_ASC)
        private set
    var isList = true
        private set

    private val _showDialogEvent = MutableLiveData<Event<Unit>>()
    val showDialogEvent: LiveData<Event<Unit>> = _showDialogEvent

    private val _orderChangeEvent = MutableLiveData<Event<Pair<Int, Int>>>()
    val orderChangeEvent: LiveData<Event<Pair<Int, Int>>> = _orderChangeEvent

    private val _listGridChangeEvent = MutableLiveData<Event<Boolean>>()
    val listGridChangeEvent: LiveData<Event<Boolean>> = _listGridChangeEvent

    private val orderChangeObserver = Observer<Pair<Int, Int>> {
        order = it
        _orderChangeEvent.value = Event(it)
    }

    private val listGridChangeObserver = Observer<Boolean> {
        isList = it
        _listGridChangeEvent.value = Event(it)
    }

    init {
        // Use "sticky" to observe the value set in ManagerActivity onCreate()
        @Suppress("UNCHECKED_CAST")
        LiveEventBus.get(EVENT_ORDER_CHANGE)
            .observeStickyForever(orderChangeObserver as Observer<Any>)
        LiveEventBus.get(
            EVENT_LIST_GRID_CHANGE,
            Boolean::class.java
        ).observeStickyForever(listGridChangeObserver)
    }

    fun showSortByDialog() {
        _showDialogEvent.value = Event(Unit)
    }

    fun switchListGrid() {
        val intent = Intent(Constants.BROADCAST_ACTION_INTENT_UPDATE_VIEW)
        intent.putExtra(Constants.INTENT_EXTRA_KEY_IS_LIST, !isList)
        context.sendBroadcast(intent)
    }

    override fun onCleared() {
        @Suppress("UNCHECKED_CAST")
        LiveEventBus.get(EVENT_ORDER_CHANGE)
            .removeObserver(orderChangeObserver as Observer<Any>)
        LiveEventBus.get(
            EVENT_LIST_GRID_CHANGE,
            Boolean::class.java
        ).removeObserver(listGridChangeObserver)
    }

    fun getOrderMap(): HashMap<Int, Int> = orderNameMap

    companion object {
        val orderNameMap = hashMapOf(
            ORDER_DEFAULT_ASC to R.string.sortby_name,
            ORDER_DEFAULT_DESC to R.string.sortby_name,
            ORDER_MODIFICATION_ASC to R.string.sortby_date,
            ORDER_MODIFICATION_DESC to R.string.sortby_date,
            ORDER_SIZE_ASC to R.string.sortby_size,
            ORDER_SIZE_DESC to R.string.sortby_size,
            ORDER_FAV_ASC to R.string.file_properties_favourite,
            ORDER_LABEL_ASC to R.string.title_label
        )
    }
}
