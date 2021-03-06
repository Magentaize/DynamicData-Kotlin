package xyz.magentaize.dynamicdata.list.internal

import xyz.magentaize.dynamicdata.list.*
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.internal.functions.Functions

internal class FilterStatic<T>(
    private val source: Observable<ChangeSet<T>>,
    private val predicate: (T) -> Boolean
) {

    fun run(): Observable<ChangeSet<T>> =
        source.scan(ChangeAwareList<T>()) { state, changes ->
            process(state, changes)
            return@scan state
        }
            .map { it.captureChanges() }
            .notEmpty()

    private fun process(filtered: ChangeAwareList<T>, changes: ChangeSet<T>) {
        changes.forEach { item ->
            when (item.reason) {
                ListChangeReason.Add -> {
                    val change = item.item
                    if (predicate(change.current))
                        filtered.add(change.current)
                }

                ListChangeReason.AddRange -> {
                    val matches = item.range.filter { predicate(it) }
                    filtered.addAll(matches)
                }

                ListChangeReason.Replace -> {
                    val change = item.item
                    val match = predicate(change.current)
                    if (match)
                        filtered.replaceOrAdd(change.previous.value, change.current)
                    else
                        filtered.remove(change.previous.value)
                }

                ListChangeReason.Remove -> filtered.remove(item.item.current)

                ListChangeReason.RemoveRange -> filtered.removeMany(item.range)

                ListChangeReason.Clear -> filtered.clearOrRemoveMany(item)

                else -> Functions.EMPTY_ACTION
            }
        }
    }
}
