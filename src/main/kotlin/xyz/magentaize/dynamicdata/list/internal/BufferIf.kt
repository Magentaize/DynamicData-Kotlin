package xyz.magentaize.dynamicdata.list.internal

import xyz.magentaize.dynamicdata.kernel.subscribeBy
import xyz.magentaize.dynamicdata.list.AnonymousChangeSet
import xyz.magentaize.dynamicdata.list.ChangeSet
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.disposables.SerialDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.PublishSubject
import xyz.magentaize.dynamicdata.kernel.ObservableEx
import kotlin.time.Duration

internal class BufferIf<T>(
    private val _source: Observable<ChangeSet<T>>,
    private val _pauseIfTrueSelector: Observable<Boolean>,
    private val _initialPauseState: Boolean,
    private val _duration: Duration = Duration.ZERO,
    private val _scheduler: Scheduler = Schedulers.computation()
) {
    fun run(): Observable<ChangeSet<T>> =
        ObservableEx.create { emitter ->
            var paused = _initialPauseState
            var buffer = AnonymousChangeSet<T>()
            val timeoutSubscriber = SerialDisposable()
            val timeoutSubject = PublishSubject.create<Boolean>()

            val bufferSelector =
                Observable.just(_initialPauseState)
                    .concatWith(_pauseIfTrueSelector.mergeWith(timeoutSubject))
                    .observeOn(_scheduler)
                    .serialize()
                    .publish()

            val pause =
                bufferSelector.filter { it }
                    .subscribe {
                        paused = true
                        //add pause timeout if required
                        if (_duration != Duration.ZERO) {
                            timeoutSubscriber.set(
                                ObservableEx.timer(_duration, _scheduler)
                                    .map { false }
                                    .subscribeBy(timeoutSubject)
                            )
                        }
                    }

            val resume =
                bufferSelector.filter { !it }
                    .subscribe {
                        paused = false
                        //publish changes and clear buffer
                        if (buffer.size == 0)
                            return@subscribe

                        emitter.onNext(buffer)
                        buffer = AnonymousChangeSet<T>()

                        //kill off timeout if required
                        timeoutSubscriber.set(Disposable.empty())
                    }

            val updateSubscriber =
                _source.serialize()
                    .subscribe { updates ->
                        if (paused)
                            buffer.addAll(updates)
                        else
                            emitter.onNext(updates)
                    }

            val connected = bufferSelector.connect()

            return@create Disposable.fromAction {
                connected.dispose()
                pause.dispose()
                resume.dispose()
                updateSubscriber.dispose()
                timeoutSubject.onComplete()
                timeoutSubscriber.dispose()
            }
        }
}
