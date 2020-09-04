//package dynamicdata.list.internal
//
//import dynamicdata.kernel.indexOfMany
//import dynamicdata.list.*
//import io.reactivex.rxjava3.core.Observable
//import io.reactivex.rxjava3.core.Scheduler
//import io.reactivex.rxjava3.disposables.CompositeDisposable
//import io.reactivex.rxjava3.schedulers.Schedulers
//import java.util.concurrent.TimeUnit
//
//internal class AutoRefresh<T, R>(
//    private val _source: Observable<IChangeSet<T>>,
//    private val _evaluator: (T) -> Observable<R>,
//    private val _bufferTimeSpan: Long? = null,
//    private val _unit: TimeUnit? = null,
//    private val _scheduler: Scheduler? = null
//) {
//    fun run(): Observable<IChangeSet<T>> =
//        Observable.create { observer ->
//            val allItems = mutableListOf<T>()
//            val shared = _source.serialize()
//                //clone all items so we can look up the index when a change has been made
//                .clone(allItems)
//                .publish()
//
//            //monitor each item observable and create change
//            val itemHasChanged = shared.mergeMany { t -> _evaluator(t).map { t } }
//
//            //create a changeset, either buffered or one item at the time
//            val itemsChanged = if (_bufferTimeSpan == null)
//                itemHasChanged.map { listOf(it) }
//            else {
//                require(_unit != null)
//                itemHasChanged.buffer(_bufferTimeSpan, _unit, _scheduler ?: Schedulers.computation())
//                    .filter { it.any() }
//            }
//
//            val requiresRefresh = itemsChanged
//                .serialize()
//                .map {
//                    //catch all the indices of items which have been refreshed
//                    allItems.indexOfMany(it) { idx, t ->
//                        Change(ListChangeReason.Refresh, t, idx)
//                    }
//                }
//                .map { ChangeSet(it) }
//
//            //publish refreshes and underlying changes
//            val publisher = shared
//                .mergeWith(requiresRefresh)
//                .subscribe(observer::onNext, observer::onError, observer::onComplete)
//
//            CompositeDisposable(publisher, shared.connect())
//        }
//}
