# 11-USER-GUIDE

# RegexCaller — User Guide

## What Is RegexCaller?

RegexCaller is a call blocker that runs silently in the background on your Samsung Galaxy S23. It blocks or silences incoming calls that match patterns you define — like blocking all numbers starting with `98765`.

**Key points:**

- Your Samsung Phone app stays unchanged — RegexCaller works alongside it, not as a replacement
- No internet access — all data stays on your phone
- No battery drain — the app only activates for a split second when a call comes in

---

## First-Time Setup

### Step 1: Open the App

After installing (via Android Studio or sideloading the APK), tap the **RegexCaller** icon to launch.

### Step 2: Grant Call Screening Permission

On first launch, you'll see the **Onboarding Screen**:

1.  Tap **"Grant Call Screening Permission"**
2.  A system dialog appears: **"Allow RegexCaller to screen your calls?"**
3.  Tap **Allow**

> This does NOT change your default phone app. Samsung Phone stays as your dialer.

### Step 3: Battery Optimization (Samsung-Specific)

After granting the call screening role, the app may show a second prompt:

1.  Tap **"Exclude from Battery Optimization"**
2.  Tap **Allow** on the system dialog

This prevents Samsung from putting the app to sleep and disabling call blocking.

### Step 4: Add to "Never Sleeping Apps" (Recommended)

For best reliability on Samsung:

1.  Open **Settings → Battery → Background usage limits**
2.  Tap **"Never sleeping apps"**
3.  Tap **+** and add **RegexCaller**

This step is optional but recommended — it ensures Samsung's aggressive battery saver doesn't interfere with call screening.

### Step 5: Done!

Tap **"Start Adding Rules"** to go to the Home Screen. The app is now active and monitoring incoming calls.

---

## Adding a Blocking Rule

### From the Home Screen

1.  Tap the **\+ button** (floating action button, bottom-right)
2.  Fill in the fields:

| Field | Description | Required |
| --- | --- | --- |
| **Label** | A name for the rule (e.g., "Telemarketing 98765") | Optional — auto-fills with the pattern if left blank |
| **Pattern** | The number pattern to match | Required |
| **Regex** | Toggle ON for advanced regex patterns | Default: OFF (wildcard mode) |
| **Action** | What to do when a call matches | Default: BLOCK |

3.  Tap **Save**

### Pattern Examples

#### Wildcard Mode (default — Regex toggle OFF)

| Pattern | What It Blocks |
| --- | --- |
| `98765*` | All numbers starting with 98765 |
| `*1234` | All numbers ending with 1234 |
| `9876?00000` | Numbers where the 5th digit can be anything |
| `9876500000` | Exactly this one number |

**Wildcard characters:**

- `*` — matches any number of digits (including none)
- `?` — matches exactly one digit

#### Regex Mode (Regex toggle ON)

| Pattern | What It Blocks |
| --- | --- |
| `^98765\d+$` | Numbers starting with 98765 |
| `^(98765\|98766)\d*$` | Numbers starting with 98765 or 98766 |
| `^\d{10}$` | Any 10-digit number |

> Most users will only need wildcard mode. Regex mode is for advanced users.

### Actions Explained

| Action | What Happens |
| --- | --- |
| **BLOCK** | Call is rejected immediately — caller hears busy/goes to voicemail |
| **SILENCE** | Phone doesn't ring, but call still goes to voicemail |
| **ALLOW** | Explicitly allow this number — overrides other blocking rules |

---

## Managing Rules

### Home Screen Overview

The Home Screen shows all your rules in a list. Each rule displays:

- **Label** — the name you gave it
- **Pattern** — the matching pattern
- **Match count** — how many calls this rule has blocked
- **On/Off switch** — enable or disable without deleting

### Edit a Rule

1.  Tap the **edit icon** (pencil) on any rule
2.  Modify the fields
3.  Tap **Save Changes**

### Delete a Rule

1.  Tap the **delete icon** (trash) on any rule
2.  The rule is removed immediately

### Enable / Disable a Rule

- Use the **toggle switch** on each rule to temporarily disable it without deleting
- Disabled rules are ignored during call screening

---

## Testing a Number

Before waiting for a real call, you can test whether a number would be blocked:

1.  From the Home Screen, tap the **phone icon** in the top bar
2.  Enter a phone number (any format works):
    - `9876500000`
    - `+919876500000`
    - `09876500000`
3.  The screen shows:
    - **Normalized number** — what the app sees after stripping country code and trunk prefix
    - **Match result:**
        - 🟢 **No match** — "Call would be allowed"
        - 🔴 **BLOCK match** — Shows which rule matched
        - 🟡 **SILENCE match** — Shows which rule matched
        - 🔵 **ALLOW match** — Shows which rule matched

> This is useful for verifying your patterns before real calls come in.

---

## How Call Blocking Works

When someone calls you, here's what happens behind the scenes:

```
Incoming call arrives
       ↓
Android sends the number to RegexCaller
       ↓
Number is normalized (strip +91, trunk prefix, etc.)
       ↓
Checked against all your enabled rules
       ↓
┌──────────────────┐
│ Match found?     │
├── Yes ───────────┤
│   BLOCK → Call rejected silently          │
│   SILENCE → Phone doesn't ring            │
│   ALLOW → Call goes through               │
├── No ────────────┤
│   Call rings normally (Samsung Phone)     │
└──────────────────┘
```

**Important:** Your Samsung Phone app handles the call as usual. RegexCaller only intercepts and filters — it never replaces your dialer.

---

## Number Normalization

You don't need to worry about number formats when creating rules. The app automatically normalizes incoming numbers:

| Caller's Number | What the App Sees | Your Rule | Match? |
| --- | --- | --- | --- |
| +919876500000 | 9876500000 | `98765*` | ✅ Yes |
| 09876500000 | 9876500000 | `98765*` | ✅ Yes |
| 9876500000 | 9876500000 | `98765*` | ✅ Yes |

**Write patterns using the bare 10-digit Indian number** (no +91, no leading 0).

---

## Common Use Cases

### Block a telemarketing prefix

Many spam callers share the same first 5-6 digits:

- Pattern: `98765*` (wildcard mode)
- Action: BLOCK

### Block all calls from an area code

- Pattern: `0120*` → normalizes, but you can also use: `0120*`
- Action: BLOCK

### Allow a specific number that gets caught by a broad rule

If `98765*` blocks a friend at `9876598765`:

1.  Add a new rule: Pattern `9876598765`, Action **ALLOW**
2.  ALLOW rules take priority — your friend's calls will ring through

### Silence unknown international numbers

- Pattern: `\+(?!91)\d+` (regex mode — matches numbers NOT starting with +91)
- Action: SILENCE

---

## Settings & Maintenance

### Check if Call Screening is Active

- From the Home Screen, tap the **settings icon** (gear) in the top bar
- The Onboarding Screen shows:
    - ✅ **"Call screening is active"** — everything is working
    - ❌ **"Call screening permission not granted"** — tap to re-grant

### Re-grant Permission

If call blocking stops working (e.g., after a Samsung firmware update):

1.  Open the app → Settings (gear icon)
2.  Tap **"Grant Call Screening Permission"** again
3.  Allow the permission

### Check Battery Optimization

If calls aren't being blocked after the phone has been idle:

1.  Open the app → Settings (gear icon)
2.  Check the battery optimization status
3.  Re-exempt if needed

---

## FAQ

### Does this replace my Samsung Phone app?

**No.** Your Samsung Phone app stays as the default dialer. RegexCaller works as a background filter — it screens calls before Samsung Phone shows them.

### Does it use internet or send my data anywhere?

**No.** The app has no internet permission at all — it's technically impossible for it to send data anywhere. All rules and call data stay on your device.

### Does it drain battery?

**No.** The app does nothing between calls. It only activates for a few milliseconds when a call comes in — less impact than checking the time on your screen.

### Will it block calls if the app is closed?

**Yes.** The call screening service runs independently of the app UI. You can close/swipe away the app and it will still block calls. The only things that can stop it are: revoking the permission, or Samsung putting it in "Deep sleeping apps."

### What happens to blocked calls?

Blocked calls are rejected silently. The caller typically hears a busy signal or goes directly to voicemail (depends on carrier). You won't see a missed call notification for blocked numbers.

### What about silenced calls?

Silenced calls still connect to voicemail, but your phone won't ring or vibrate. You may see a missed call notification depending on your Samsung Phone settings.

### Does it work on calls from contacts?

**Yes.** The app checks ALL incoming calls against your rules. If you want to ensure contacts are never blocked, add an ALLOW rule for specific numbers.

### How many rules can I have?

There's no hard limit. The app efficiently checks 100+ rules in under 10ms per call. Performance is not a concern.

### Can I import/export rules?

Not in the current version. Rules are stored locally in the app's database.

### Does it work after a phone restart?

**Yes.** The call screening role persists across reboots. No action needed after restarting your phone.

### What about private/unknown/withheld numbers?

Private and unknown numbers (where the caller ID is hidden) are always **allowed through** — the app cannot match a pattern against a number it doesn’t receive. If Samsung’s Phone app shows it as “Private number”, RegexCaller lets it ring normally.

### Does the app show notifications when a call is blocked?

**No.** RegexCaller operates as a completely silent filter. You can check the **match count** on each rule in the Home Screen to see how many calls were caught. Blocked calls may still appear in your Samsung Phone call log (depending on your Samsung Phone settings).