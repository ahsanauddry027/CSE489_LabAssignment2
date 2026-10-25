# Project guide: how the app works (viva notes)

This guide explains every idea used in the app, step by step, with short excerpts from the
real code. Read it top to bottom once; afterwards each section works on its own as a
reference. File names are relative to
`app/src/main/java/com/example/labassignment2_cse489/` unless they start with `res/`.

**Contents**
1. [The app in one picture](#1-the-app-in-one-picture)
2. [What "Jetpack" means here](#2-what-jetpack-means-here)
3. [Fragments](#3-fragments)
4. [Navigation component and the drawer](#4-navigation-component-and-the-drawer)
5. [Activities 2 and 3, explicit Intents](#5-activities-2-and-3-explicit-intents)
6. [Broadcast receivers](#6-broadcast-receivers)
7. [ViewModel and the lifecycle](#7-viewmodel-and-the-lifecycle)
8. [Image Scale: ScaleGestureDetector and Matrix](#8-image-scale-scalegesturedetector-and-matrix)
9. [MediaPlayer: audio and video](#9-mediaplayer-audio-and-video)
10. [Rules of modern Android versions](#10-rules-of-modern-android-versions)
11. [Tests](#11-tests)
12. [Likely viva questions](#12-likely-viva-questions)

---

## 1. The app in one picture

```
MainActivity  (DrawerLayout + MaterialToolbar + NavigationView + NavHostFragment)
│
├── BroadcastFragment ── Proceed ──► BroadcastInputActivity (Activity 2)
│        (start screen)                   │ custom: text field    │ battery: live percentage
│                                         ▼                       ▼
│                                  CustomReceiverActivity (Activity 3)
│                                    custom: registers CustomMessageReceiver, shows the text
│                                    battery: does nothing
├── ImageScaleFragment   (Coil download + ZoomableImageView)
├── VideoFragment        (VideoView + MediaController)
└── AudioFragment        (AudioPlayerViewModel owns a MediaPlayer)
```

- **One activity hosts the four drawer screens**, which are fragments swapped by the
  Navigation component.
- **Activities 2 and 3** are real activities (the brief calls them activities), opened with
  explicit Intents.
- Every screen keeps its state in a **ViewModel**, so rotation never loses anything.

## 2. What "Jetpack" means here

Android Jetpack is the set of AndroidX libraries Google recommends. This app uses:

| Library | Used for |
|---|---|
| `fragment` | The four drawer screens |
| `navigation-fragment`, `navigation-ui` | The navigation graph, drawer and toolbar wiring |
| `lifecycle-viewmodel`, `lifecycle-viewmodel-savedstate` | State that survives rotation and process death |
| `lifecycle-livedata`, `lifecycle-runtime` | Lifecycle-aware data (`LiveData`, `repeatOnLifecycle`) |
| `activity` | `enableEdgeToEdge()`, `OnBackPressedDispatcher`, `by viewModels()` |
| `core` | `ContextCompat.registerReceiver`, window insets helpers |
| `drawerlayout`, `appcompat`, Material Components | The UI widgets |

The "Jetpack philosophy" is about **separation of concerns**: the UI (activity or fragment)
only draws state and forwards clicks, state lives in a ViewModel, and anything that must be
started and stopped (a receiver, a player, a coroutine) is tied to a **lifecycle**, so it can
never leak.

## 3. Fragments

A **fragment** is a reusable piece of UI with its own lifecycle that lives inside an activity.
The drawer swaps fragments in one container instead of starting a new activity for every
screen, so the toolbar and drawer stay in place.

A fragment has **two lifecycles**: the fragment itself, and its **view** (`onCreateView` to
`onDestroyView`). When you switch drawer items, the fragment you leave can keep existing while
its view is destroyed. That is why every fragment here drops its ViewBinding in
`onDestroyView()` and observes data with `viewLifecycleOwner`:

```kotlin
// broadcast/BroadcastFragment.kt
class BroadcastFragment : Fragment(R.layout.fragment_broadcast) {

    // ViewBinding for the fragment's view. It only exists between onViewCreated and
    // onDestroyView, because a fragment can outlive its view (e.g. on the back stack).
    private var _binding: FragmentBroadcastBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BroadcastViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentBroadcastBinding.bind(view)
        ...
        viewModel.selectedMode.observe(viewLifecycleOwner) { mode -> ... }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
```

**ViewBinding** generates a class per layout (`fragment_broadcast.xml` →
`FragmentBroadcastBinding`) with a typed field per view ID, so there is no `findViewById` and
no wrong casts.

## 4. Navigation component and the drawer

**The graph** (`res/navigation/nav_graph.xml`) lists the four screens; the start destination
is the Broadcast Receiver screen:

```xml
<navigation ... app:startDestination="@id/broadcastFragment">
    <fragment
        android:id="@+id/broadcastFragment"
        android:name="com.example.labassignment2_cse489.broadcast.BroadcastFragment"
        android:label="@string/menu_broadcast" />
    <!-- imageScaleFragment, videoFragment, audioFragment -->
</navigation>
```

**The drawer menu** (`res/menu/drawer_menu.xml`) uses *the same IDs* as the destinations.
That is the whole trick: NavigationUI matches a tapped menu item to the destination with the
same ID.

```xml
<item android:id="@+id/broadcastFragment" android:title="@string/menu_broadcast" />
<item android:id="@+id/imageScaleFragment" android:title="@string/menu_image_scale" />
```

**The wiring** (`MainActivity.kt`):

```kotlin
val navHostFragment =
    supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
val navController = navHostFragment.navController

// Every drawer item is a top-level destination: all four show the hamburger icon (which
// opens the drawer) instead of an Up arrow.
val appBarConfiguration = AppBarConfiguration(binding.navView.menu, binding.drawerLayout)
binding.toolbar.setupWithNavController(navController, appBarConfiguration)
binding.navView.setupWithNavController(navController)
```

- `AppBarConfiguration(menu, drawerLayout)` declares all four menu items **top-level
  destinations**. On a top-level screen the toolbar shows the ☰ icon, which opens the drawer;
  on any other screen it would show an Up arrow.
- `toolbar.setupWithNavController` sets the title from each destination's `android:label`.
- `navView.setupWithNavController` navigates when an item is tapped, highlights the current
  item and closes the drawer.

**Multiple back stacks.** When you tap a drawer item, NavigationUI pops back to the start
screen and **saves** the state of the screen you leave (`saveState`), and restores it when you
come back (`restoreState`). So the Image Scale screen does not download the picture again, and
the Audio screen's ViewModel (and its player!) stays alive while you look at another screen.
Section 9 explains why that matters for audio.

**Back closes the drawer first.** On Android 16+ with targetSdk 36+ the old `onBackPressed()`
is never called (predictive back). The supported way is an `OnBackPressedCallback`, enabled
only while the drawer is open:

```kotlin
private val closeDrawerOnBack = object : OnBackPressedCallback(false) {
    override fun handleOnBackPressed() {
        binding.drawerLayout.closeDrawer(GravityCompat.START)
    }
}
...
onBackPressedDispatcher.addCallback(this, closeDrawerOnBack)
binding.drawerLayout.addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {
    override fun onDrawerOpened(drawerView: View) { closeDrawerOnBack.isEnabled = true }
    override fun onDrawerClosed(drawerView: View) { closeDrawerOnBack.isEnabled = false }
})
```

When the callback is disabled, Back does its normal job: from another drawer screen it
returns to the start screen, and from the start screen it leaves the app.

## 5. Activities 2 and 3, explicit Intents

An **explicit Intent** names the exact class to start. Each activity offers a small factory
so callers can't forget an extra:

```kotlin
// broadcast/CustomReceiverActivity.kt
fun newIntent(context: Context, mode: BroadcastMode, message: String? = null): Intent =
    Intent(context, CustomReceiverActivity::class.java)
        .putExtra(EXTRA_MODE, mode.name)
        .putExtra(EXTRA_MESSAGE, message)
```

- The mode travels as its name (`"CUSTOM"` / `"BATTERY"`) and is parsed back with
  `BroadcastMode.fromName()`, which returns `null` for anything unknown, so a bad Intent
  closes the screen instead of crashing.
- The toolbar's Up arrow calls `finish()`, so the previous activity reappears as it was, with
  its extras and the text you typed.
- In the manifest only `MainActivity` has `android:exported="true"` (it is the launcher entry
  point). Activities 2 and 3 are `exported="false"`: other apps cannot start them.

## 6. Broadcast receivers

### 6.1 What a broadcast is
A **broadcast** is a message (an `Intent` with an **action** string) sent to everyone who
listens for that action. A **`BroadcastReceiver`** is the listener: its `onReceive(context,
intent)` runs on the main thread when a matching broadcast arrives. The system sends
broadcasts (battery changed, airplane mode, …) and apps can send their own.

### 6.2 Manifest-declared vs context-registered

| | Manifest-declared | Context-registered (used here) |
|---|---|---|
| Declared with | `<receiver>` in `AndroidManifest.xml` | `registerReceiver(...)` in code |
| Lives | Always; the system can even start the app for it | Only between register and unregister |
| Restrictions | Since Android 8 most *implicit* broadcasts are no longer delivered to manifest receivers | Receives while registered |
| `ACTION_BATTERY_CHANGED` | **Not possible**: it can only be received by a receiver registered in code | Works |
| Must clean up? | No | **Yes**: unregister, or the receiver leaks |

Both receivers in this app are context-registered: the brief wants the custom receiver
*created in Activity 3*, and the battery broadcast can't be received any other way.

### 6.3 The battery broadcast is sticky
`Intent.ACTION_BATTERY_CHANGED` is a **sticky broadcast**: the system keeps the last one, and
`registerReceiver()` *returns it immediately*. So the screen shows a value at once instead of
waiting for the battery level to change.

```kotlin
// broadcast/BatteryStatusLiveData.kt
override fun onActive() {
    val stickyIntent: Intent? =
        appContext.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    stickyIntent?.let { value = BatteryStatus.fromIntent(it) }
}

override fun onInactive() {
    appContext.unregisterReceiver(receiver)
}
```

The extras are read and turned into a percentage, guarding against a missing level or a
scale of 0 (division by zero):

```kotlin
// broadcast/BatteryStatus.kt
fun percentOf(level: Int, scale: Int): Int? {
    if (level < 0 || scale <= 0) return null
    return (level * PERCENT_MAX / scale).coerceIn(0, PERCENT_MAX)
}
```

`EXTRA_STATUS` (charging, discharging, full, not charging) is mapped to the `ChargingStatus`
enum, and the screen also shows the time of the last update.

### 6.4 Listening only while visible (lifecycle-aware)
`BatteryStatusLiveData` is a `LiveData`. LiveData calls `onActive()` when it gets its first
*active* observer (one whose lifecycle is at least STARTED) and `onInactive()` when the last
one stops. Activity 2 observes it with itself as the LifecycleOwner:

```kotlin
batteryViewModel.batteryStatus.observe(this) { status -> renderBattery(status) }
```

So the receiver is registered in `onStart` and unregistered in `onStop`, automatically, and it
can never leak. `BatteryViewModel` keeps the LiveData across rotation, so the last value is
shown again immediately. The LiveData holds the **application** context, never an activity.

### 6.5 Export flags (Android 14+)
Since Android 14 (targetSdk 34+), registering a receiver in code for **non-system**
broadcasts must say whether other apps may send to it:

- `RECEIVER_EXPORTED`: other apps can send to this receiver.
- `RECEIVER_NOT_EXPORTED`: only the system and this app can.

Without a flag, `registerReceiver` throws a `SecurityException`. Our custom receiver uses
`ContextCompat.registerReceiver(..., ContextCompat.RECEIVER_NOT_EXPORTED)`; the `ContextCompat`
version also works on old Android versions, where it emulates the flag with a signature
permission that only this app holds.

The battery receiver needs no flag: `ACTION_BATTERY_CHANGED` is a **protected** broadcast that
only the system may send, so there is nothing to protect against, and such receivers are exempt.

On the sending side, `setPackage(packageName)` restricts delivery to this app, so no other app
can read the user's text.

### 6.6 The custom receiver

```kotlin
// broadcast/CustomMessageReceiver.kt
class CustomMessageReceiver(
    private val onMessage: (String) -> Unit,
) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_CUSTOM_MESSAGE) return
        val message = intent.getStringExtra(EXTRA_MESSAGE) ?: return
        Log.i(TAG, "Received via broadcast: $message")
        onMessage(message)
    }

    companion object {
        const val ACTION_CUSTOM_MESSAGE = "${BuildConfig.APPLICATION_ID}.action.CUSTOM_MESSAGE"
        const val EXTRA_MESSAGE = "${BuildConfig.APPLICATION_ID}.extra.MESSAGE"

        fun createBroadcast(context: Context, message: String): Intent =
            Intent(ACTION_CUSTOM_MESSAGE)
                .setPackage(context.packageName)
                .putExtra(EXTRA_MESSAGE, message)
    }
}
```

Prefixing the action with the application ID keeps it unique on the device. Activity 3
**creates** the receiver in `onCreate`, **registers** it in `onStart` and **unregisters** it in
`onStop`, the matching pair of callbacks:

```kotlin
// broadcast/CustomReceiverActivity.kt
override fun onStart() {
    super.onStart()
    val receiver = receiver ?: return // battery mode: nothing to register
    ContextCompat.registerReceiver(
        this,
        receiver,
        CustomMessageReceiver.intentFilter(),
        ContextCompat.RECEIVER_NOT_EXPORTED,
    )
    renderRegistration(registered = true)

    if (!viewModel.initialBroadcastSent) {
        viewModel.initialBroadcastSent = true
        sendCustomBroadcast()
    }
}

override fun onStop() {
    receiver?.let { registered ->
        unregisterReceiver(registered)
        renderRegistration(registered = false)
    }
    super.onStop()
}
```

When `onReceive` runs, Activity 3 stores the text and time in its ViewModel, shows
"Received via broadcast: …", a Toast and the Log line. **Broadcast again** sends it once more,
and the counter shows how many deliveries arrived.

### 6.7 Why Activity 3 sends the broadcast (not Activity 2)
A normal broadcast is delivered only to receivers that are registered **at the moment it is
sent**; it is not kept for receivers that register later. Look at the timeline if Activity 2
broadcast the text:

```
Activity 2: sendBroadcast("Hello")  ──► delivered to... nobody (no receiver exists yet)
Activity 2: startActivity(Activity 3)
Activity 3: onCreate → creates the receiver
Activity 3: onStart  → registers the receiver   (too late: the broadcast is gone)
```

So the text travels to Activity 3 **inside the Intent** (an extra), and Activity 3 registers
its receiver first and then broadcasts the text to it:

```
Activity 2: startActivity(Intent + "Hello")
Activity 3: onStart → register receiver → sendBroadcast("Hello") → onReceive("Hello") ✔
```

Sticky broadcasts would "solve" this, but they are deprecated and insecure (any app can read
or replace them), so we don't use them.

### 6.8 Not re-sending on rotation
Rotation destroys and recreates Activity 3, so `onStart` runs again. A flag in the ViewModel's
`SavedStateHandle` remembers that the first broadcast was already sent, and the last received
message is kept there too:

```kotlin
// broadcast/CustomReceiverViewModel.kt
var initialBroadcastSent: Boolean
    get() = savedStateHandle[KEY_INITIAL_SENT] ?: false
    set(value) { savedStateHandle[KEY_INITIAL_SENT] = value }
```

### 6.9 What we deliberately did not use
- **`LocalBroadcastManager`**: deprecated; it was an in-process event bus, not a real broadcast.
- **Our own sticky broadcasts** (`sendStickyBroadcast`): deprecated since API 21, insecure.
- **Manifest `<receiver>` entries**: the brief wants the receiver created in Activity 3, and the
  battery broadcast can't be received that way.

## 7. ViewModel and the lifecycle

**Rotation destroys the activity and its fragments** and creates new ones. A **ViewModel**
survives that: the new screen gets the same ViewModel object back. It is cleared
(`onCleared()`) only when the screen is gone for good.

```kotlin
private val viewModel: BroadcastViewModel by viewModels()
```

- In an activity, the ViewModel lives as long as the activity (across rotations).
- In a fragment, `by viewModels()` lives as long as the fragment is on the back stack, which
  includes the time its state is *saved* by NavigationUI (section 4).

**`SavedStateHandle`**: Android may kill the app's process in the background. A ViewModel does
not survive that, but values in its `SavedStateHandle` do (they are saved in a Bundle):

```kotlin
// broadcast/BroadcastViewModel.kt
class BroadcastViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {
    val selectedMode: LiveData<BroadcastMode> =
        savedStateHandle.getLiveData(KEY_SELECTED_MODE, BroadcastMode.CUSTOM)
    fun select(mode: BroadcastMode) { savedStateHandle[KEY_SELECTED_MODE] = mode }
}
```

**LiveData vs StateFlow.** Both hold the latest UI state. LiveData is lifecycle-aware by
itself; a `StateFlow` is collected in a coroutine that we tie to the lifecycle with
`repeatOnLifecycle`, which starts collecting in `onStart` and cancels in `onStop`:

```kotlin
// audio/AudioFragment.kt
viewLifecycleOwner.lifecycleScope.launch {
    viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.uiState.collect { state -> render(state) }
    }
}
```

**`AndroidViewModel`** gives a ViewModel the *Application* context (needed by Coil and
MediaPlayer). A ViewModel must never keep an Activity or View, because it outlives them.

| Screen | What survives rotation, and where |
|---|---|
| Broadcast Receiver | Spinner choice: `BroadcastViewModel` (SavedStateHandle) |
| Activity 2 (battery) | Last battery value: `BatteryViewModel` |
| Activity 3 (custom) | Received text, count, "already sent": `CustomReceiverViewModel` (SavedStateHandle) |
| Image Scale | Downloaded image: `ImageScaleViewModel`; zoom: `ZoomableImageView`'s saved state |
| Video | Position + "keep playing": `VideoViewModel` (SavedStateHandle) |
| Audio | The MediaPlayer itself: `AudioPlayerViewModel` |

## 8. Image Scale: ScaleGestureDetector and Matrix

### 8.1 Loading the picture
`ImageScaleViewModel` downloads the photo with **Coil 3**. Coil's core artifact cannot fetch
URLs by itself; `coil-network-okhttp` adds the HTTP fetcher. By default Coil decodes an image
at the size of its view, and zooming into that would look blurry, so we ask for the original:

```kotlin
val request = ImageRequest.Builder(context)
    .data(IMAGE_URL)
    .size(Size.ORIGINAL)
    .build()
_uiState.value = when (val result = context.imageLoader.execute(request)) {
    is SuccessResult -> ImageUiState.Success(result.image)
    is ErrorResult -> ImageUiState.Error
}
```

The state (`Loading` / `Success` / `Error`) is a `StateFlow`; the fragment shows a spinner, the
picture, or the error with Retry.

### 8.2 The image matrix
With `scaleType = MATRIX`, an ImageView draws its picture through a **`Matrix`**: a small
table of numbers that maps each picture pixel to a screen position (scale + translation here).
We change the matrix; the ImageView draws the result.

**Fit (1x)** is the scale that shows the whole picture, centred, like `FIT_CENTER`:

```kotlin
val fitScale = ZoomMath.fitScale(dw, dh, contentWidth, contentHeight) // min(vw/dw, vh/dh)
drawMatrix.setScale(fitScale, fitScale)
drawMatrix.postTranslate((contentWidth - dw * fitScale) / 2f, (contentHeight - dh * fitScale) / 2f)
```

The fit is recomputed whenever the drawable or the view size changes, because Coil sets the
picture *asynchronously*, usually after the first layout, and rotation changes the size.

### 8.3 Pinch: ScaleGestureDetector
`ScaleGestureDetector` turns raw two-finger touch events into a gesture. In `onScale` it
reports the **focal point** (`focusX`, `focusY`, between the fingers) and the **scale factor**
(current finger distance ÷ previous distance, e.g. 1.03 when the fingers move apart).

```kotlin
private fun zoomBy(factor: Float, focusX: Float, focusY: Float) {
    val target = ZoomMath.clampZoom(zoom * factor)   // keep 1x..5x
    val applied = target / zoom                      // what is left after clamping
    drawMatrix.postScale(applied, applied, focusX - paddingLeft, focusY - paddingTop)
    zoom = target
    clampTranslation()
    applyMatrix()
}
```

`postScale(s, s, px, py)` scales **around the pivot (px, py)**: a point p moves to
`px + s·(p − px)`, so the pivot itself does not move. Using the focal point as the pivot is
what keeps the part of the picture under your fingers *under your fingers*.

### 8.4 Keeping the picture in place
After every change `clampTranslation()` maps the picture's rectangle through the matrix and
moves it back if needed (`ZoomMath.edgeCorrection`):
- if the picture is narrower (or shorter) than the view on an axis, it is **centred**;
- otherwise it is pulled back so **no empty gap** shows at an edge.

### 8.5 Pan and double-tap: GestureDetector
A second detector, `GestureDetector`, handles one-finger gestures:
- `onScroll`: pan with `postTranslate(-distanceX, -distanceY)`, only while zoomed in;
- `onDoubleTap`: animate (`ValueAnimator`) to 2.5x at the tap point, or back to 1x.

While zoomed or pinching, the view calls `parent.requestDisallowInterceptTouchEvent(true)` so
the DrawerLayout doesn't take the drag as "open the drawer".

### 8.6 Rotation
The view saves the zoom and the picture point at the centre of the view (as fractions of the
picture size) in `onSaveInstanceState`, and re-applies them once the new view has a size and a
drawable, so the same area stays in view after rotating.

## 9. MediaPlayer: audio and video

### 9.1 MediaPlayer is a state machine
Calling a method in the wrong state throws an exception or reports an error, so the order
matters:

```
Idle --setDataSource--> Initialized --prepareAsync--> Preparing --(onPrepared)--> Prepared
Prepared / Paused / PlaybackCompleted --start--> Started --pause--> Paused
Started / Paused --stop--> Stopped --prepareAsync--> Preparing --> Prepared ...
Started --(end of track)--> PlaybackCompleted
any state --release--> End
```

Rules used in the code:
- `prepareAsync()` instead of `prepare()`: preparing can take time and must not block the UI;
  `onPrepared` is called when the player is ready.
- After `stop()` the player must be **prepared again** before it can play.
- `release()` frees the decoder and audio resources; forgetting it leaks them.

### 9.2 Audio: the player lives in the ViewModel

```kotlin
// audio/AudioPlayerViewModel.kt
private val player = MediaPlayer()

init {
    player.setAudioAttributes(
        AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build(),
    )
    application.resources.openRawResourceFd(R.raw.sample_audio).use { file ->
        player.setDataSource(file)
    }
    player.setOnPreparedListener { onPrepared() }
    player.setOnCompletionListener { onCompleted() }
    player.setOnErrorListener { _, what, extra -> onError(what, extra) }
    player.prepareAsync()
}

override fun onCleared() {
    stopProgressUpdates()
    player.release() // any state -> End
}
```

- Because the player is in the ViewModel, **rotation does not interrupt the music**: only the
  fragment and its views are recreated.
- While playing, a coroutine in `viewModelScope` publishes the position five times a second
  for the slider and the mm:ss labels; it is cancelled on pause, stop and `onCleared()`.
- At the end of the track `onCompleted()` rewinds and resets the UI.

### 9.3 Never two players at once
NavigationUI *saves* the Audio screen when you switch to another drawer item (section 4), so
`AudioPlayerViewModel` and its player stay alive. Without extra code the music would keep
playing behind the video. So the fragment pauses when it stops, except during a rotation:

```kotlin
// audio/AudioFragment.kt
override fun onStop() {
    super.onStop()
    if (!requireActivity().isChangingConfigurations) viewModel.pause()
}
```

The video, in turn, never starts by itself when you come back (section 9.4), so audio and
video can never play at the same time.

### 9.4 Video: VideoView + MediaController
`VideoView` is a `SurfaceView` that wraps an `android.media.MediaPlayer`; `MediaController` is
the standard control bar (play/pause, seek bar, times). The video is a bundled resource:

```kotlin
// video/VideoFragment.kt
binding.videoView.apply {
    setMediaController(controller)
    setOnPreparedListener { onVideoPrepared() }
    setOnErrorListener { _, what, extra -> onVideoError(what, extra) }
    setOnCompletionListener { onVideoCompleted() }
    setVideoURI("android.resource://${requireContext().packageName}/${R.raw.sample_video}".toUri())
}
```

- **Position:** when the screen goes away, `onPause` saves the position (and whether to keep
  playing) in `VideoViewModel`. This happens in `onPause` rather than `onStop` because the
  VideoView releases its player together with its surface, and by `onStop` the position could
  already read 0. When prepared again, `onVideoPrepared()` seeks to the saved position and
  only resumes playing if the screen was merely rotated.
- **Errors:** `onVideoError` shows a friendly message and returns `true`, so the system's
  "Can't play this video" dialog is not shown.
- **Cleanup:** `onDestroyView` hides the controller (it lives in its own window, which would
  otherwise leak) and calls `stopPlayback()`, which releases the MediaPlayer.
- The controller also hides when the drawer starts to move, because its window floats above
  the activity and the drawer would slide underneath it.

## 10. Rules of modern Android versions

| Rule | What the app does |
|---|---|
| **Edge-to-edge** is enforced from targetSdk 35: the app draws behind the status and navigation bars | `enableEdgeToEdge()` in every activity, and `View.applyWindowInsetsPadding()` (`util/WindowInsetsExt.kt`) pads toolbars, content and the drawer header by the bar insets, in both orientations |
| **Predictive back**: with targetSdk 36+ on Android 16+, `onBackPressed()` is never called | `OnBackPressedCallback` (section 4); `android:enableOnBackInvokedCallback="true"` in the manifest for Android 13–15 |
| **`android:exported`** must be explicit | Set on every activity; only `MainActivity` is `true` |
| **Receiver export flags** (Android 14) | `RECEIVER_NOT_EXPORTED` for the custom receiver (section 6.5) |
| Rotation via ViewModels, not `android:configChanges` | Section 7 |
| Avoid deprecated APIs | No `Handler()`, `startActivityForResult`, `setHasOptionsMenu`, untyped `getParcelableExtra`, `LocalBroadcastManager` or sticky broadcasts. `BundleCompat.getParcelable` is used for typed reads |

```kotlin
// util/WindowInsetsExt.kt (simplified)
ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
    val insets = windowInsets.getInsets(
        WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout(),
    )
    view.updatePadding(top = initialTop + if (top) insets.top else 0, ...)
    windowInsets
}
```

## 11. Tests

- **Unit tests** run on your computer (JVM): `.\gradlew.bat testDebugUnitTest`. They check
  the pure logic: battery percentage (including scale 0 and a missing level), the charging
  status mapping, mode parsing, the zoom maths and the mm:ss formatter.
- **Instrumented tests** run on an emulator or phone: `.\gradlew.bat connectedDebugAndroidTest`.
  Espresso clicks through the app like a user: every drawer item, Back closing the drawer,
  the whole Custom flow ("Hello CSE489" typed in Activity 2 appears in Activity 3), blank-input
  validation and the battery flow. `ZoomableImageViewTest` feeds synthetic two-finger touch
  events into the real `ScaleGestureDetector` to check that the zoom is anchored at the
  fingers and limited to 1x–5x, plus panning and double-tap.

## 12. Likely viva questions

**Why fragments instead of four activities?** The drawer and toolbar stay in one activity and
only the content changes. Navigation handles the back stack, titles and highlighting, and
fragments are lighter to switch.

**What is the difference between `onDestroyView` and `onDestroy` in a fragment?**
`onDestroyView` destroys only the fragment's views (for example when you switch drawer items);
the fragment object may live on. That's why we set `_binding = null` there.

**How does the drawer know which screen to open?** The menu item IDs equal the destination
IDs in `nav_graph.xml`; `NavigationView.setupWithNavController` navigates to the matching ID.

**Why does every screen show the hamburger and not a back arrow?** All four are top-level
destinations in `AppBarConfiguration(navView.menu, drawerLayout)`.

**Context-registered or manifest receiver, which did you use and why?** Context-registered,
for both. The brief wants the custom receiver created in Activity 3, and
`ACTION_BATTERY_CHANGED` can only be received by a receiver registered in code.

**What does "sticky" mean for the battery broadcast?** The system keeps the last battery
broadcast; `registerReceiver` returns it at once, so we can show the level without waiting.

**When is the battery receiver registered?** Only while Activity 2 is visible: the LiveData
registers in `onActive` (observer STARTED) and unregisters in `onInactive` (observer stopped).

**Why `RECEIVER_NOT_EXPORTED`?** Since Android 14 a receiver for non-system broadcasts must
declare whether other apps may send to it; ours is private to the app. The battery receiver
needs no flag because only the system can send that protected broadcast.

**Why doesn't Activity 2 send the custom broadcast?** At that moment no receiver exists (it is
created in Activity 3), and normal broadcasts are not stored, so it would be lost. The text
goes in the Intent, and Activity 3 broadcasts it after registering its receiver.

**What happens on rotation in Activity 3?** The activity is recreated, the receiver is
unregistered in `onStop` and registered again in `onStart`, and the ViewModel remembers that
the broadcast was already sent and what was received, so nothing is sent twice or lost.

**How does pinch zoom keep the point under the fingers?** `Matrix.postScale(s, s, focusX,
focusY)` scales around the focal point, so that point stays fixed; the zoom factor is clamped
to 1x–5x before it is applied.

**Why `Size.ORIGINAL` in Coil?** Otherwise Coil decodes the image at the view's size and
zooming in shows a blurry, upscaled bitmap.

**Why is the MediaPlayer in a ViewModel and not in the fragment?** The ViewModel survives
rotation, so the music continues; it is released in `onCleared()` when the screen is gone for
good.

**How do you make sure audio and video never play together?** The Audio screen pauses in
`onStop` (unless rotating), the Video screen pauses in `onPause`, and the video never resumes
on its own after you come back to it.

**What is `prepareAsync()` for?** Preparing reads and decodes the start of the file. Doing it
asynchronously keeps the UI responsive; `onPrepared` tells us when we may call `start()`.
