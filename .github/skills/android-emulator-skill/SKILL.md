---
name: android-emulator-skill
description: Use Maestro CLI, Maestro MCP, and Maestro flows for Android emulator management, app launching, semantic UI interaction, hierarchy inspection, screenshots, and test execution.
---

# Android Emulator Skill

Use **Maestro** as the default tool for Android device automation in this repo.

## MeepleBook repo note

For build, lint, and unit/instrumented test tasks, use the repo's normal Gradle commands unless the
user explicitly asks for Maestro flows.

## When to use this skill

Use this skill when the user asks to:

- start or target an Android emulator
- launch the app and navigate through UI flows
- tap, type, scroll, or go back semantically
- inspect the current screen hierarchy
- take screenshots during an automation flow
- run local E2E flows against a device
- produce Maestro test artifacts or reports

## Maestro tools to prefer

### Maestro CLI

Use Maestro CLI for local execution and artifacts:

```bash
# Start a Maestro-compatible Android emulator
maestro start-device --platform android

# Run a flow
maestro test .maestro/login.yaml

# Store artifacts in a predictable repo folder
maestro test --test-output-dir=build/maestro-results .maestro/login.yaml

# Store debug logs as well
maestro test --test-output-dir=build/maestro-results --debug-output=build/maestro-debug .maestro/login.yaml

# Generate JUnit output for CI
maestro test --format junit --output build/maestro-report.xml .maestro
```

### Maestro MCP

When Maestro MCP is configured, prefer these commands for direct automation:

- `start_device`
- `list_devices`
- `launch_app`
- `stop_app`
- `inspect_view_hierarchy`
- `tap_on`
- `input_text`
- `back`
- `take_screenshot`
- `run_flow`
- `run_flow_files`
- `check_flow_syntax`
- `query_docs`

### Maestro flows

Use YAML flows for repeatable app journeys.

Example:

```yaml
appId: app.meeplebook
---
- launchApp
- tapOn: "Log Play"
- assertVisible: "Player Name"
- tapOn:
    id: "playerNameField"
- inputText: "Alice"
- tapOn: "Save"
```

## Common Maestro patterns

### 1. Start a device

```bash
maestro start-device --platform android
```

If multiple devices are running, target one explicitly:

```bash
maestro --device emulator-5554 test .maestro/smoke.yaml
```

### 2. Launch / stop the app

```yaml
appId: app.meeplebook
---
- launchApp
- stopApp
```

You can also reset app state on launch:

```yaml
appId: app.meeplebook
---
- launchApp:
    clearState: true
```

### 3. Tap and type semantically

Prefer visible text, IDs, or other stable selectors over coordinates:

```yaml
appId: app.meeplebook
---
- launchApp
- tapOn: "Username"
- inputText: "alice"
- tapOn:
    id: "loginButton"
```

### 4. Navigate and scroll

```yaml
appId: app.meeplebook
---
- launchApp
- scroll
- swipe:
    direction: UP
- back
```

### 5. Inspect the current screen

Use Maestro hierarchy tools instead of custom UIAutomator XML parsing:

```bash
maestro hierarchy
```

Or via MCP:

- `inspect_view_hierarchy`

### 6. Take screenshots and collect artifacts

```yaml
appId: app.meeplebook
---
- launchApp
- takeScreenshot: "add-play-screen"
```

Artifact-oriented CLI run:

```bash
maestro test \
  --test-output-dir=build/maestro-results \
  --debug-output=build/maestro-debug \
  .maestro
```

This gives you access to:

- screenshots and video (`--test-output-dir`)
- `commands-*.json`
- `maestro.log` (`--debug-output`)
- optional JUnit / HTML reports

## Debugging guidance

When a flow fails:

1. inspect the hierarchy with `maestro hierarchy` or Maestro MCP
2. rerun with `--debug-output`
3. save screenshots / reports with `--test-output-dir`
4. use stable selectors (`id`, exact visible text) before falling back to looser matching

## Selector guidance

Prefer selectors in this order:

1. stable view ID
2. exact visible text
3. constrained selector objects
4. custom swipe/repeat logic only when necessary

Avoid coordinate-based automation unless the user explicitly asks for it and Maestro selectors are
not viable.

Use Maestro where it is a direct fit, and use the repo's normal Gradle / Android tooling for
non-Maestro concerns outside UI automation.
