# Synthetic demonstration

Authenticated Android upload → API stores bytes and metadata → owner creates a sharing token → public viewer handles its gate → recipient streams or downloads; administrator manages the platform.

## Walkthrough

1. Register two synthetic accounts and upload harmless sample text/video.
2. Share a file and folder; open the public viewer, then test Android deep-link handling.
3. Attempt cross-user deletion and folder access; exceed a small demo quota and inspect cleanup.
4. Inspect view, wallet and withdrawal records without connecting payment/ad providers.

## Acceptance checklist

- [ ] Node syntax and route registration checks
- [ ] Unauthorized mutations and sharing-token rejection
- [ ] Quota rejection and failed-upload cleanup
- [ ] Android and viewer integration need separate runtime verification

## Evidence discipline

Screenshots must come from the running application with synthetic records. Record the component, viewport and configuration. A storyboard is not a recorded walkthrough. Benchmark only generated data and include hardware, input size, configuration, elapsed time and cache conditions.

External ad/payment integrations require provider configuration. Wallet records do not prove completed payouts. Storage cleanup, repeated view events and financial state transitions need database integration checks.
