## From an Android upload to a public sharing page

IQBox connects a Kotlin and Jetpack Compose application, an Express API, PostgreSQL, a PHP sharing viewer and a React administration panel. Keeping these surfaces together makes the upload-to-share boundary visible: private file ownership belongs to the authenticated API, while a generated sharing token permits the public viewer to locate a share. A public link must not grant deletion or unrelated folder access.

The release checks therefore include another user's folder, unauthorized deletion, invalid tokens, rejected executable content and storage quota limits. The viewer needs its own server-reachable API address; the address used by an Android emulator is different. A small local PHP router now reproduces the actual /file and /folder link shapes without a VPS configuration.

## Separating product workflows from provider dependencies

The source contains ad gates, view accounting, subscriptions, referrals, wallet records and withdrawals. These are engineering workflows, not evidence of real revenue or a connected payment service. Store buttons remain hidden until a real store URL is configured, and the public demonstration identifies its generated file.

The administration review also caught an initial-loading error: statistics were still null when the folder counter rendered. Fixing that boundary let the fresh database, file list and public viewer form a coherent demonstration. Native background uploads, repeated-view policy and provider state transitions are the next verification priorities.
