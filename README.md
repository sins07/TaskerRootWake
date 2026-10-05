# Tasker Root Wake

Tap the icon and it runs, as root:

    am startservice -n net.dinglisch.android.taskerm/com.joaomgcd.taskerm.plugin.ServiceRequestQuery

If Android 8+ refuses that (background start limits), it retries with
`am start-foreground-service`. A toast shows the result; full output goes to
logcat under the tag `TaskerRootWake`.

## Build
Push to GitHub. Actions builds the APK; download it from the run's
**Artifacts** section (TaskerRootWake-apk).
