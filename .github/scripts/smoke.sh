#!/usr/bin/env bash
# Emulator smoke test for the release (R8) build of Listo.
# Installs the APK, taps through every screen, adds the widget to the home screen,
# repeats key screens at Galaxy S24 screen size, and saves screenshots and logs.
# Usage: smoke.sh path/to/app-release.apk     (OUT_DIR is where results go)

set -u
APK="$1"
PKG=com.hariv.listo
OUT="${OUT_DIR:-$PWD/verify-out}/emulator"
mkdir -p "$OUT"
FAIL=0
STEP=0

log() { echo "[smoke] $*" | tee -a "$OUT/steps.txt"; }

shot() {
  STEP=$((STEP + 1))
  local name
  name=$(printf "%02d-%s" "$STEP" "$1")
  adb exec-out screencap -p > "$OUT/$name.png" 2>/dev/null || log "screenshot $name failed"
}

dump_ui() {
  adb shell rm -f /sdcard/ui.xml > /dev/null 2>&1
  adb shell uiautomator dump /sdcard/ui.xml > "$OUT/.last-dump.txt" 2>&1
  adb shell cat /sdcard/ui.xml 2> /dev/null
}

bounds_of() { # $1 = grep -E pattern for the node; prints "x1 y1 x2 y2" of the last match
  dump_ui | grep -o "<node [^>]*>" | grep -E "$1" | tail -1 \
    | grep -o 'bounds="\[[0-9]*,[0-9]*\]\[[0-9]*,[0-9]*\]"' | grep -o '[0-9]\+' | tr '\n' ' '
}

# Tap the last on-screen element whose text or description is exactly $1.
# With $2 = "left", tap just left of it instead (where a task's check circle sits).
tap() {
  local b try
  for try in 1 2 3; do
    b=$(bounds_of "(text|content-desc)=\"$1\"")
    # Fall back to elements whose label contains the text (merged labels like "New rule").
    [ -z "$b" ] && b=$(bounds_of "(text|content-desc)=\"[^\"]*$1[^\"]*\"")
    [ -n "$b" ] && break
    sleep 1
  done
  if [ -z "$b" ]; then
    log "could not find \"$1\" on screen (dump said: $(tr '\n' ' ' < "$OUT/.last-dump.txt"))"
    dump_ui > "$OUT/ui-missing-$(echo "$1" | tr -c 'A-Za-z0-9' '_').xml"
    return 1
  fi
  set -- $b "${2:-}"
  if [ "$5" = "left" ]; then
    adb shell input tap $(( $1 - 63 )) $(( ($2 + $4) / 2 ))
  else
    adb shell input tap $(( ($1 + $3) / 2 )) $(( ($2 + $4) / 2 ))
  fi
  sleep 1.5
}

# Tap the first text field on screen.
tap_field() {
  local b
  b=$(dump_ui | grep -o "<node [^>]*>" | grep 'class="android.widget.EditText"' | head -1 \
    | grep -o 'bounds="\[[0-9]*,[0-9]*\]\[[0-9]*,[0-9]*\]"' | grep -o '[0-9]\+' | tr '\n' ' ')
  if [ -z "$b" ]; then log "no text field on screen"; return 1; fi
  set -- $b
  adb shell input tap $(( ($1 + $3) / 2 )) $(( ($2 + $4) / 2 ))
  sleep 1
}

type_text() {
  adb shell input text "$(echo "$1" | sed 's/ /%s/g')"
  sleep 0.5
}

alive() { adb shell pidof "$PKG" > /dev/null 2>&1; }

check() {
  if ! alive; then
    log "ERROR: Listo is not running after: $1"
    FAIL=1
  fi
}

open_app() {
  adb shell am start -W -n "$PKG/.ui.MainActivity" > /dev/null 2>&1
  sleep 3
}

log "Installing $(ls -la "$APK" | awk '{print $5}') bytes"
if ! adb install -r "$APK" > "$OUT/install.txt" 2>&1; then
  log "ERROR: install failed"; cat "$OUT/install.txt"; exit 1
fi
adb logcat -c

# --- First launch -----------------------------------------------------------------
open_app
check "first launch"
shot "first-launch"

# --- Add tasks --------------------------------------------------------------------
for task in "Pay the electricity bill" "Call mom about the weekend" "Book dentist appointment"; do
  tap_field
  type_text "$task"
  adb shell input keyevent 66
  sleep 1
done
adb shell input keyevent 4 # hide the keyboard
sleep 1
check "adding tasks"
shot "tasks-added"

# Tick one task (its check circle sits left of the text).
tap "Book dentist appointment" left
sleep 1
check "ticking a task"
shot "task-ticked"

# --- Recurring --------------------------------------------------------------------
tap "Recurring"
check "opening Recurring"
shot "recurring-empty"
tap "New rule"
check "opening the rule editor"
tap_field
type_text "Pay the rent"
adb shell input keyevent 4
sleep 1
shot "rule-editor"
tap "Add rule"
check "saving a rule"
shot "recurring-with-rule"

# --- Archive ----------------------------------------------------------------------
tap "Archive"
check "opening Archive"
shot "archive"

# --- Settings ---------------------------------------------------------------------
tap "Tasks"
tap "Settings"
check "opening Settings"
shot "settings"
adb shell input swipe 540 1900 540 700 300
sleep 1
shot "settings-scrolled"

# --- Widget -----------------------------------------------------------------------
# Settings > Home-screen widget > Add asks the launcher to place the widget.
tap "Add"
sleep 2
shot "widget-pin-dialog"
tap "Add to home screen" || tap "ADD TO HOME SCREEN" || tap "Add automatically"
sleep 2
adb shell input keyevent 3 # home
sleep 3
shot "home-page-1"
# Find the page holding the widget (its header mark is labelled "Open Listo").
FOUND=0
for page in 1 2 3 4; do
  if dump_ui | grep -q 'content-desc="Open Listo"'; then FOUND=$page; break; fi
  adb shell input swipe 950 1200 150 1200 250
  sleep 2
done
if [ "$FOUND" -gt 0 ]; then
  log "widget found on home page $FOUND"
  shot "home-widget"
  # Tick a task right on the widget, then add one with the widget's + button.
  tap "Call mom about the weekend" left && sleep 2 && shot "widget-ticked"
  if tap "Add a task"; then
    sleep 1
    type_text "Buy milk"
    adb shell input keyevent 66
    sleep 4
    shot "widget-after-quick-add"
  fi
  check "using the widget"
else
  log "ERROR: widget not found on the home screen"
  shot "home-no-widget"
  FAIL=1
fi

# --- Background job ----------------------------------------------------------------
# Run WorkManager's jobs now (recurring tasks + auto-archive) to make sure the worker starts.
for id in 0 1 2 3; do adb shell cmd jobscheduler run -f "$PKG" "$id" > /dev/null 2>&1; done
sleep 4

# --- Galaxy S24 screen size (384dp wide) ------------------------------------------------
adb shell wm size 1080x2340
adb shell wm density 450
sleep 2
adb shell am force-stop "$PKG"
open_app
check "launch at S24 size"
shot "s24-tasks"
tap "Recurring"
shot "s24-recurring"
tap "Tasks"
tap "Settings"
shot "s24-settings"
adb shell wm size reset
adb shell wm density reset

# --- Crashes -------------------------------------------------------------------------
adb logcat -d -b crash > "$OUT/crash-log.txt" 2>&1
adb logcat -d -v time 2>/dev/null | grep -iE "listo|AndroidRuntime|glance|WM-|FATAL" | tail -400 > "$OUT/logcat.txt"
if grep -q "$PKG" "$OUT/crash-log.txt"; then
  log "ERROR: crash recorded (see crash-log.txt)"
  FAIL=1
fi

log "Finished, fail=$FAIL"
exit $FAIL
