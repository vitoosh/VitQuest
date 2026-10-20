# Project-specific R8 rules for the release build.
#
# None are needed today: Room, Hilt, WorkManager, kotlinx.serialization, Navigation
# and Health Connect ship their own consumer rules. CI builds the release APK on
# every push to catch shrinking problems; add a rule here only with a comment on
# what breaks without it.
