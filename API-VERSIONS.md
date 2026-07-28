# API Versioning

> API Versioning lets addon developers know exactly what version of the Coder API their addon is running against, and whether it is stable enough for production use.

## What is API Versioning?

Each release of Coder ships with an API version string defined in `version-api.yml`. This string tells addon developers:

- What feature set is available at runtime
- Whether the API is stable or still under active development
- Whether their addon may break on the current version

Addon developers can check the current API version at runtime using `/coder api version`, or by reading the `version-api.yml` bundled in the Coder jar.

## Why does it matter?

When Coder updates, the internal API may change. New methods get added, old ones may be removed or behave differently. The API version string gives addon publishers a clear signal of what to expect — so they can decide whether to update, wait, or keep their users on an older Coder version.

If you are building an addon, always check the API version your addon was built against and document it clearly for your users.

## API Path

The current canonical API path is:
dev.codestuff.coder.api

The legacy path `me.coder.api` is still accepted for backwards compatibility, but is deprecated. Addon publishers should migrate as soon as possible.

## Versions Table

| API Version | Status |
|---|---|
| api-2.4.stable-3 | Stable |
| api-2.5.experimental-4 | Under Development |

---

*Coder | CodeStuff | 2026*