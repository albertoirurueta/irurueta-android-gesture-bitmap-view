// 32 MiB for the decoded levels
view.cachedLevelsMemoryBudget = 32L * 1024L * 1024L

// only the preview is kept: lowest memory use, details are decoded as patches
view.cachedLevelsMemoryBudget = 0L

// back to the default: 4 times the pixels of the view, 4 bytes each
view.cachedLevelsMemoryBudget = GestureBitmapView.DEFAULT_CACHED_LEVELS_MEMORY_BUDGET
