# Contributing

Thanks for helping! Bug reports, ideas and pull requests are welcome.

## Before you start

- Check [existing issues](https://github.com/zibirik/streamdeck/issues) first.
- For larger changes, open an issue to discuss the idea before writing code.
- Security problems: see [`SECURITY.md`](SECURITY.md) (do not use public issues).

## Development setup

Requirements: Windows, .NET 8 SDK (or newer), JDK 17, Android SDK (platform 35).

```powershell
.\tools\check-prereqs.ps1
.\tools\test-server.ps1        # server unit tests
.\tools\build-android.ps1      # debug APK
.\tools\run-server.ps1         # run the server locally
```

## Pull request checklist

- [ ] `.\tools\test-server.ps1` passes and new server logic has tests.
- [ ] `.\tools\build-android.ps1` succeeds.
- [ ] Behaviour changes are reflected in `docs/` and the README where relevant.
- [ ] Nothing secret is committed (PINs, tokens, keystores).
- [ ] The change is small and focused; unrelated refactors go in separate PRs.

## Licensing of contributions

StreamPanel is distributed under the [PolyForm Noncommercial License 1.0.0](LICENSE) and is also offered under a
separate commercial license ([`COMMERCIAL.md`](COMMERCIAL.md)). By submitting a pull request you confirm that you wrote
the contribution (or have the right to submit it) and you grant the maintainer a perpetual, worldwide, irrevocable,
royalty-free license to use, modify, sublicense and distribute it under these or any other terms, including commercial ones.
If you do not agree, please do not submit code.
