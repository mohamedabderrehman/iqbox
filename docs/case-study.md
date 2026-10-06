# Android file and video sharing app with an admin panel

## From the problem to the implementation

Upload and share files and videos from Android, with public sharing pages and an admin panel for accounts, storage and sharing controls.

Authenticated Android upload → API stores bytes and metadata → owner creates a sharing token → public viewer handles its gate → recipient streams or downloads; administrator manages the platform.

## Decisions and tradeoffs

PostgreSQL stores metadata and business records; uploaded bytes stay in filesystem storage. Backups must cover both.

Sharing tokens grant public access independently from owner-only mutation rights. A valid token does not grant deletion permission.

The maintained web viewer is `video-page/`; the duplicate historical `iqbox-video/` copy is excluded. `routes/sharing.js` exists but is not mounted in `server.js`.

The development command uses Node watch mode instead of undeclared nodemon. Duplicate legacy admin settings/stats registrations were removed.

Executable upload extensions are rejected and video extension and MIME checks must both pass. This is not malware scanning.

## What the publication preparation established

API startup and admin production build passed on a fresh PostgreSQL demo database. HTTP checks passed user/admin token separation, foreign folder rejection, generated text upload, public share lookup, invalid token rejection, unauthorized deletion rejection, executable upload rejection, and quota enforcement. Browser verification passed the administration loading state and opened the generated share link in the local PHP viewer. Optional store links stay hidden until configured. A null statistics reference during initial loading was corrected.

## Deployment experience and evidence limits

Previously deployed and tested. The source shows an evolution from videos to general files and folders; no revenue or audience figures are asserted.

Android builds/background transfers/deep links, repeated-view accounting and complete withdrawal/payment state transitions need separate verification. External advertisements and payment processing are dependencies, not demonstrated income.

## Next steps

Complete the uncovered checks above, record the results, and update the demonstration. Retain the existing architecture and add reproducible synthetic cases before claiming performance improvements or another provider integration.
