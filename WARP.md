# WARP.md

This file provides guidance to WARP (warp.dev) when working with code in this repository.

## Build, run, and test

This is a single-module Android application built with Gradle Kotlin DSL and the `com.android.application` plugin (`:app` module).

Use the Gradle CLI that is available in your environment (for example, `./gradlew` / `./gradlew.bat` if a wrapper exists, otherwise `gradle`). Replace `gradle` below with the appropriate command.

### Build

- Assemble debug APK for the app module:
  - `gradle :app:assembleDebug`
- Assemble release APK (uses the `release` buildType configured in `app/build.gradle.kts`):
  - `gradle :app:assembleRelease`

### Unit tests (JVM)

- Run all unit tests for the `debug` variant:
  - `gradle :app:testDebugUnitTest`
- Run a single unit test (class or method):
  - `gradle :app:testDebugUnitTest --tests "com.example.portableevm.YourTestClass"`
  - `gradle :app:testDebugUnitTest --tests "com.example.portableevm.YourTestClass.yourTestMethod"`

### Instrumentation tests (Android)

Instrumentation tests use `androidx.test.runner.AndroidJUnitRunner` as configured in `defaultConfig.testInstrumentationRunner`.

- Run all connected device/emulator tests for the `debug` variant:
  - `gradle :app:connectedDebugAndroidTest`

### Lint

The Android Gradle Plugin provides standard lint tasks.

- Run lint for the `debug` variant of the app module:
  - `gradle :app:lintDebug`
- Run a full lint check for all variants:
  - `gradle :app:lint`

## Project structure and architecture

### Modules

- Root project: `PortableEvmAndroid` (see `settings.gradle.kts`).
- App module: `:app` — the only module, configured as an Android application with Jetpack Compose enabled, targeting SDK 34 and min SDK 24.

### Application and dependency container

- `PortableEvmApp` (`app/src/main/java/com/example/portableevm/PortableEvmApp.kt`)
  - Custom `Application` subclass that stores a static `instance` for global access.
  - On startup, it constructs a single `AppContainer` instance and exposes it via `container`.

- `AppContainer` (`app/src/main/java/com/example/portableevm/data/AppContainer.kt`)
  - Simple manual dependency container for the data layer.
  - Creates the singleton `EvmDatabase` and wires `ElectionRepository` and `AdminRepository` from the DAOs.
  - All higher layers (e.g., `EvmViewModel`) obtain repositories by going through `PortableEvmApp.instance.container`.

This pattern is the central place where new repositories or shared services should be registered if you extend the app.

### Data layer (Room + repositories)

- `EvmDatabase` and entities (`app/src/main/java/com/example/portableevm/data/EvmDatabase.kt`):
  - Entities:
    - `ElectionEntity` (`elections` table): represents an election with `startTimestamp`, optional `endTimestamp`, and `isCompleted` flag.
    - `CandidateEntity` (`candidates` table): candidates are linked to elections via `electionId` and store `buttonNumber` and `votes`.
    - `AdminSettingsEntity` (`admin_settings` table): single-row table (`id = 0`) for admin password and whether a password is required to start new elections.
  - Relationship model:
    - `ElectionWithCandidates` combines an `ElectionEntity` with its list of `CandidateEntity` via a `@Relation`.
  - DAOs:
    - `ElectionDao`:
      - Observes the active election (`observeActiveElection`) and all elections (`observeElections`) as `Flow<ElectionWithCandidates?>` / `Flow<List<ElectionWithCandidates>>`.
      - Mutating operations: `insertElection`, `insertCandidates`, `incrementVote`, `completeElection`.
    - `AdminSettingsDao`:
      - Observes current settings (`observeSettings`) as a `Flow<AdminSettingsEntity?>`.
      - `upsert` settings with `OnConflictStrategy.REPLACE`.
  - Database configuration:
    - Singleton `getInstance(context)` with `Room.databaseBuilder` and `fallbackToDestructiveMigration()`.

- Repositories (still in `EvmDatabase.kt`):
  - `ElectionRepository`:
    - Provides read-only flows for active and historical elections.
    - Encapsulates write operations:
      - `startNewElection` creates an `ElectionEntity`, inserts associated `CandidateEntity` rows, and returns the new election ID.
      - `registerVote` increments the vote count for a candidate via `ElectionDao.incrementVote`.
      - `endElection` marks an election as completed and sets `endTimestamp`.
  - `AdminRepository`:
    - Wraps `AdminSettingsDao` to provide an `observeSettings` flow.
    - `setPassword` writes the single `AdminSettingsEntity` row with the given password and `requirePasswordForNewElection` flag.

Data flows into the UI exclusively via these repositories, which are the abstraction boundary over Room.

### View model and UI state

- `EvmViewModel` (`app/src/main/java/com/example/portableevm/ui/EvmViewModel.kt`):
  - Injected with `ElectionRepository` and `AdminRepository` through a custom `ViewModelProvider.Factory` that uses `PortableEvmApp.instance.container`.
  - Internal state is a `MutableStateFlow<EvmUiState>`, exposed as `StateFlow<EvmUiState>`.
  - On initialization, it `combine`s three flows:
    - `electionRepository.observeActiveElection()`
    - `electionRepository.observeElections()`
    - `adminRepository.observeSettings()`
    into a single `EvmUiState` containing:
    - `activeElection: UiElection?`
    - `previousElections: List<UiElection>`
    - `admin: AdminUiState` (admin password + requirement flag).
  - UI-layer models:
    - `UiCandidate`, `UiElection`, `AdminUiState`, and `EvmUiState` provide a UI-focused projection of database entities.
  - Key operations (called from composables):
    - `setAdminPassword` and `changeAdminPassword`: update admin password and requirement flag.
    - `updateAdminSettings`: toggles the `requirePasswordForNewElection` setting while preserving the current password.
    - `startNewElection`: starts a new election given a name and list of `(candidateName, buttonNumber)` pairs.
    - `endActiveElection`: completes the currently active election.
    - `registerVote`: increments the vote count for the currently active election based on Arduino button input.

The `EvmViewModel` is the single source of truth for UI state; composables read `uiState` and call these methods to perform mutations.

### Hardware / Arduino integration

- `ArduinoManager` (`app/src/main/java/com/example/portableevm/arduino/ArduinoManager.kt`):
  - Responsible for discovering and managing a USB serial connection to an Arduino-like device using the `usb-serial-for-android` library (`com.github.mik3y:usb-serial-for-android`).
  - Exposes two reactive streams:
    - `connectionState: StateFlow<ArduinoConnectionState>` to represent `Disconnected`, `Connecting`, or `Connected(device)`.
    - `buttonEvents: SharedFlow<Int>` that emits the logical button number (1–4) based on incoming serial characters.
  - Lifecycle:
    - `start()` registers a `BroadcastReceiver` for USB permission and attach/detach events, and probes existing devices.
    - `stop()` cancels ongoing scope, unregisters the receiver, and closes the port.
  - Connection logic:
    - Uses `UsbSerialProber.getDefaultProber()` to obtain a `UsbSerialPort` for a `UsbDevice`.
    - Uses a simple heuristic to treat devices with vendor IDs `0x2341` or `0x2A03` as Arduino-like.
    - Configures the serial port at 9600 baud, 8N1.
  - Reading loop:
    - Runs on a dedicated `CoroutineScope(Dispatchers.IO)`.
    - Interprets bytes as characters `'1'`–`'4'`, emitting corresponding button numbers into `buttonEvents`.
    - On read error, logs and calls `disconnect()`.

### UI layer and navigation

- Entry point activity:
  - `MainActivity` (`app/src/main/java/com/example/portableevm/MainActivity.kt`):
    - Hosts the composable `PortableEvmAppRoot()`.

- Root composable and navigation:
  - `PortableEvmAppRoot`:
    - Applies `PortableEvmTheme` and a Material 3 `Surface`.
    - Creates a `NavHost` with `rememberNavController()`.
    - Uses a local `showSplash` state and `LaunchedEffect` (1.5s delay) to show `SplashScreen` before navigating into the main graph.
    - Obtains a `EvmViewModel` instance via `viewModel(factory = EvmViewModel.Factory)` and subscribes to `uiState` with `collectAsStateWithLifecycle()`.
    - Defines routes:
      - `"home"` → `HomeScreen`
      - `"admin_settings"` → `AdminSettingsScreen`
      - `"new_election"` → `NewElectionScreen`
      - `"voting"` → `VotingScreen`
      - `"results"` → `ResultsScreen`
      - `"previous_elections"` → `PreviousElectionsScreen`

- Screen responsibilities (all in `app/src/main/java/com/example/portableevm/ui/Screens.kt`):
  - `SplashScreen`:
    - Simple centered title screen shown briefly at app launch.
  - `HomeScreen`:
    - Presents high-level navigation actions: start new election, view previous elections, admin settings, exit.
    - Enforces admin password protection when `admin.requirePasswordForNewElection` and a password is set, showing an inline password dialog before allowing navigation to `"new_election"`.
  - `AdminSettingsScreen`:
    - Lets the admin set or change the password and toggle `requirePasswordForNewElection`.
    - Uses `EvmViewModel` methods `setAdminPassword`, `changeAdminPassword`, and `updateAdminSettings` to persist changes.
  - `NewElectionScreen`:
    - Configures a new election:
      - Election name.
      - Number of candidates (1–4) via `FilterChip`s.
      - For each candidate: name and a unique Arduino button assignment (1–4).
    - Performs validation:
      - Non-empty candidate names.
      - All candidates have assigned buttons.
      - Each candidate has a unique button.
    - On success, calls `viewModel.startNewElection` and navigates to `"voting"` (popping back to `"home"` non-inclusively).
  - `VotingScreen`:
    - Obtains the active election from `uiState.activeElection`; if none, it shows a message and an action to return to `"home"`.
    - Creates an `ArduinoManager` bound to the current `Context`’s `UsbManager` and starts it in a `DisposableEffect`.
    - Uses the host `Activity`'s lifecycle scope to collect:
      - `connectionState` → local `connectionState` state.
      - `buttonEvents` → calls `viewModel.registerVote` once per voter (gated by `hasVoted`).
    - Displays current connection state, last message (e.g., waiting or vote accepted), and the list of candidates with their current vote counts.
    - Provides two main actions:
      - "Next Voter" resets `hasVoted` and the status message.
      - "End Election" calls `viewModel.endActiveElection` and navigates to `"results"`.
  - `ResultsScreen`:
    - Shows results for the most relevant election, picking the first entry from `previousElections` or falling back to `activeElection` if none.
    - Renders candidates and vote counts in a `LazyColumn` of `Card`s.
  - `PreviousElectionsScreen`:
    - Lists `uiState.previousElections` in a `LazyColumn`.
    - Each item shows summary information: name, number of candidates, and whether the election is completed.

### End-to-end data and control flow

At a high level, the system behaves as follows:

1. App startup:
   - `PortableEvmApp` initializes `AppContainer` and `EvmDatabase`.
   - `MainActivity` launches `PortableEvmAppRoot`, which creates `EvmViewModel` using the container.
   - `EvmViewModel` combines flows from `ElectionRepository` and `AdminRepository` into `EvmUiState`.

2. Election lifecycle:
   - From `HomeScreen`, the admin starts `NewElectionScreen` (optionally behind a password gate).
   - `NewElectionScreen` validates candidate names and button assignments, then calls `EvmViewModel.startNewElection` → `ElectionRepository.startNewElection` → `ElectionDao.insertElection` and `insertCandidates`.
   - `EvmViewModel` observes the new active election through `observeActiveElection`, updating `uiState.activeElection`.

3. Voting with Arduino:
   - `VotingScreen` starts `ArduinoManager`, which connects to an Arduino-like device over USB and emits `buttonEvents`.
   - On the first button press per voter, `VotingScreen` calls `EvmViewModel.registerVote(buttonNumber)` → `ElectionRepository.registerVote` → `ElectionDao.incrementVote` for the active election.
   - Room flows emit updated `ElectionWithCandidates`, which propagate through `EvmViewModel` back into `EvmUiState` and the UI.

4. Completing elections and viewing history:
   - When the election ends, `VotingScreen` invokes `EvmViewModel.endActiveElection`, which sets `isCompleted` and `endTimestamp` in the database.
   - Historical results are surfaced via `PreviousElectionsScreen` and `ResultsScreen`, both of which read from `uiState.previousElections` / `uiState.activeElection`.

This architecture centers around a single `EvmViewModel` with a Room-backed repository layer, and an Arduino/USB integration that translates hardware button presses into vote registration events.