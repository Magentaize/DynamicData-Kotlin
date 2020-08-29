package dynamicdata.cache

class ChangeSet<TObject, TKey> : ArrayList<Change<TObject, TKey>>, IChangeSet<TObject, TKey>{
    constructor()
    constructor(collection: Collection<Change<TObject, TKey>>) : super(collection)
    constructor(capacity: Int) : super(capacity)

    override val adds
        get() =
            count(ChangeReason.Add)

    override val updates
        get() =
            count(ChangeReason.Update)

    override val removes
        get() =
            count(ChangeReason.Remove)

    override val refreshes
        get() =
            count(ChangeReason.Refresh)

    override val moves
        get() =
            count(ChangeReason.Moved)

    private fun count(reason: ChangeReason) =
        count { it.reason == reason }

    override fun toString(): String =
        "ChangeSet: TODO"

    private inline fun <reified TObject> to() =
        TObject::class.java
}
