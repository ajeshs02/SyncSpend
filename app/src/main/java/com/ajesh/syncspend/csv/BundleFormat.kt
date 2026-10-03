package com.ajesh.syncspend.csv

/** Which parts of the backup bundle a given export/import call touches. */
enum class BundleSection { TRANSACTIONS, REMINDERS, SUBSCRIPTIONS, FORECASTS, TRANSFERS }
