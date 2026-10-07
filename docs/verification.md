# Current release verification

Recorded on 2026-10-06 using disposable local data. Historical deployment and these development checks are recorded separately.

## Passed locally

API startup and admin production build passed on a fresh PostgreSQL demo database. HTTP checks passed user/admin token separation, foreign folder rejection, generated text upload, public share lookup, invalid token rejection, unauthorized deletion rejection, executable upload rejection, and quota enforcement. Browser verification passed the administration loading state and opened the generated share link in the local PHP viewer. Optional store links stay hidden until configured. A null statistics reference during initial loading was corrected.

## Checks and commands

```sh
npm ci
node tools/bootstrap-demo.js
npm run dev
# Separate terminal:
node tools/check-demo.js
cd admin-panel
npm ci
npm run dev
# PHP viewer, separate terminal from root:
php -S 127.0.0.1:8088 -t video-page
```

## CI status

The configured GitHub Actions workflows are registered, but the initial runs ended with startup_failure before any jobs or check annotations were created. Local results above are independent of CI. No passing CI badge is shown; the service supplied no further diagnostic message through the available API.

## Remaining platform and coverage limits

Android builds/background transfers/deep links, repeated-view accounting and complete withdrawal/payment state transitions need separate verification. External advertisements and payment processing are dependencies, not demonstrated income.

PHP checks used PHP 8.4.26; Node builds used Node 24.19; Python checks used Python 3.12.10 where applicable. This record does not claim production hardening, paid provider verification or tests on every platform.
