# Quick Start: Testing Shake Detection

## 🚀 How to Test the Shake Detection Feature

### Prerequisites
- Android Studio installed
- Android device or emulator (API 24+)
- Device with accelerometer sensor

---

## Step 1: Build and Install

```bash
cd ShakeSkipPlayer
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

Or use Android Studio:
- Open project
- Click "Run" (Shift + F10)
- Select your device

---

## Step 2: Grant Permissions

On first launch, the app will request:
- ✅ Read Media Audio
- ✅ Post Notifications

**Note**: Accelerometer doesn't require runtime permissions!

---

## Step 3: Test Basic Shake Detection

### Method 1: Using Existing Test Code

1. Open `ShakeDetectorTest.kt`
2. Run tests: Right-click → Run 'ShakeDetectorTest'
3. Verify all tests pass

### Method 2: Manual Testing on Device

1. **Load some music** into the app
2. **Start playback** of any track
3. **Go to Settings** (if navigation is wired up)
4. **Enable "Shake to Skip"** (should be on by default)
5. **Shake your device firmly** (like shaking a salt shaker)
6. **Observe**:
   - ✅ Device vibrates briefly
   - ✅ Track skips to next song
   - ✅ Shake indicator shows "Shake Active"

### Method 3: Emulator Testing (Limited)

⚠️ **Note**: Android Emulator has limited accelerometer simulation
- Use "Virtual Sensors" in Extended Controls
- Or test on physical device for best results

---

## Step 4: Test Sensitivity Adjustment

1. Go to **Settings → Shake Detection**
2. Move **Sensitivity Slider** to different positions:
   - **Gentle** (10-12 m/s²): Very light shake needed
   - **Medium** (16-18 m/s²): Normal shake
   - **Vigorous** (22-25 m/s²): Firm shake required
3. Test shake detection at each level
4. Verify settings persist after closing and reopening app

---

## Step 5: Test Edge Cases

### Test 1: Debounce (No Double-Skip)
- Shake once quickly
- Shake again immediately within 500ms
- ✅ Should only skip once, not twice

### Test 2: Playback State
- Pause music
- Try to shake
- ✅ Should NOT skip (detection disabled when paused)
- Resume playback
- Shake again
- ✅ Should skip (detection re-enabled)

### Test 3: Haptic Feedback
- Enable haptic in settings
- Shake device
- ✅ Should feel vibration
- Disable haptic in settings
- Shake device
- ✅ Should NOT feel vibration (but still skips)

### Test 4: False Positives
- Walk around with phone in pocket
- Walk up/down stairs
- Put phone in car cup holder while driving
- ✅ Should NOT trigger skips during normal movement

---

## Step 6: Check Logs

Enable detailed logging to see shake detection in action:

```bash
adb logcat -s ShakeDetector:D ShakeDetectionService:D PlaybackViewModel:D
```

Expected log output:
```
ShakeDetector: Shake detected! Acceleration: 16.8 m/s²
ShakeDetectionService: Shake detected - triggering callback
PlaybackViewModel: Shake detected - skipping to next track
```

---

## Step 7: Monitor Battery Usage

### Short Test (15 minutes)
1. Fully charge device
2. Enable shake detection
3. Play music for 15 minutes
4. Check battery: Settings → Battery
5. ✅ Should use < 1.5% battery

### Long Test (1 hour)
1. Fully charge device
2. Enable shake detection
3. Play music for 1 hour
4. Check battery usage
5. ✅ Should use < 5% battery (target)

---

## Debugging Tips

### Shake Not Detected
- Check if accelerometer available: `adb shell dumpsys sensorservice`
- Verify shake detection enabled in settings
- Try increasing sensitivity (lower threshold)
- Shake harder or more deliberately
- Check logs for "Shake detected" messages

### Too Many False Positives
- Decrease sensitivity (higher threshold)
- Try 18-22 m/s² range
- Ensure device not in pocket during test
- Avoid testing while walking

### No Haptic Feedback
- Check device has vibrator capability
- Verify haptic feedback enabled in settings
- Some emulators don't support vibration
- Check vibrate permission granted

### Settings Not Persisting
- Check DataStore initialization
- Verify app has storage permissions
- Clear app data and try again
- Check logcat for DataStore errors

---

## Integration Testing Script

```kotlin
// Add this to your test suite
@Test
fun testShakeToSkipIntegration() {
    // 1. Start playback
    playbackViewModel.playSong(testSong)
    
    // 2. Bind shake detection service
    playbackViewModel.bindServices()
    
    // 3. Enable shake detection
    playbackViewModel.toggleShakeDetection() // if disabled
    
    // 4. Simulate shake (requires instrumented test)
    // triggerShakeEvent()
    
    // 5. Verify next track started
    // verify(playbackService).skipToNext()
}
```

---

## Performance Benchmarks

### Expected Performance Metrics

| Metric | Target | Acceptable |
|--------|--------|------------|
| Detection latency | < 50ms | < 100ms |
| False positive rate | < 1% | < 5% |
| Battery per hour | < 3% | < 5% |
| Memory usage | < 10MB | < 20MB |
| CPU usage (active) | < 5% | < 10% |

---

## Troubleshooting Common Issues

### Issue: "Service not bound"
**Solution**: Ensure `bindServices()` called in ViewModel initialization

### Issue: "No accelerometer found"
**Solution**: 
- Check device specs
- Some emulators don't have sensors
- Test on physical device

### Issue: Shake detection too sensitive/not sensitive enough
**Solution**: Adjust sensitivity in settings (10-25 range)

### Issue: App crashes on shake
**Solution**: 
- Check logcat for stack trace
- Verify all services properly initialized
- Ensure playback service running

---

## Testing on Different Devices

### Recommended Test Devices
- **Google Pixel** (reference device)
- **Samsung Galaxy** (popular manufacturer)
- **OnePlus/Xiaomi** (different sensor calibrations)

### OS Versions to Test
- ✅ Android 7.0 (API 24) - Minimum
- ✅ Android 10.0 (API 29)
- ✅ Android 13.0 (API 33) - New permissions
- ✅ Android 14/15 (latest)

---

## Next Steps After Testing

Once testing is complete:
1. ✅ Document any issues found
2. ✅ Adjust default sensitivity if needed
3. ✅ Optimize battery consumption if high
4. ✅ Add user feedback if detection confusing
5. ✅ Prepare for beta testing with real users

---

## Success Criteria

You've successfully tested shake detection when:
- ✅ Shake consistently triggers track skip
- ✅ No false positives during normal use
- ✅ Battery impact is acceptable
- ✅ Haptic feedback works correctly
- ✅ Settings persist properly
- ✅ Works across multiple devices
- ✅ No crashes or lag

---

## Need Help?

Check these resources:
- `SHAKE_DETECTION_README.md` - Technical details
- `IMPLEMENTATION_SUMMARY.md` - Architecture overview
- Inline code comments - Implementation details
- Android docs: https://developer.android.com/guide/topics/sensors

---

**Happy Testing! 🎵**

Remember: This is a beta feature. User feedback will be valuable for fine-tuning!
