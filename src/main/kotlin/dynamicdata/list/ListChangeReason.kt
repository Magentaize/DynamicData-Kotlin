package dynamicdata.list

enum class ListChangeReason {
    Add,
    AddRange,
    Replace,
    Remove,
    RemoveRange,
    Refresh,
    Moved,
    Clear,
}
